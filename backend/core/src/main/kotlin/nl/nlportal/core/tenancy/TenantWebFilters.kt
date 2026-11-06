/*
 * Copyright 2026 PinkRoccade Local Government B.V.
 *
 * Licensed under EUPL, Version 1.2 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.nlportal.core.tenancy

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.Ordered
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

/**
 * PROTOTYPE (wayfinder #204). Bepaalt de tenant uit het hostdomein (allowlist) en schrijft die met
 * `contextWrite` in de Reactor Context. Onbekend domein: 404, nooit een standaardtenant.
 * Draait vóór Spring Security (-100), zodat ook anonieme paden een tenant hebben.
 */
class TenantWebFilter(
    private val directory: TenantDirectory,
) : WebFilter,
    Ordered {
    override fun getOrder() = ORDER

    override fun filter(
        exchange: ServerWebExchange,
        chain: WebFilterChain,
    ): Mono<Void> {
        // request.uri is afgeleid van de Host-header (en van X-Forwarded-Host als een ForwardedHeaderTransformer
        // actief is: alleen veilig achter een ingress die die header overschrijft).
        val oin =
            exchange.request.uri.host
                ?.let(directory::tenantForHost)
        if (oin == null) {
            exchange.response.statusCode = HttpStatus.NOT_FOUND
            return exchange.response.setComplete()
        }
        return chain.filter(exchange).contextWrite { it.put(TenantContext.KEY, oin) }
    }

    companion object {
        const val ORDER = -200
    }
}

/**
 * PROTOTYPE (wayfinder #204). Bij een geauthenticeerd request moet de tenant van de token-issuer gelijk zijn
 * aan de host-tenant, anders 403 plus securityevent (alleen OIN's, geen tokeninhoud). Draait ná Spring Security
 * (-100) en leest de authenticatie uit de Reactor Context.
 */
class TenantIssuerConsistencyFilter(
    private val directory: TenantDirectory,
) : WebFilter,
    Ordered {
    override fun getOrder() = ORDER

    override fun filter(
        exchange: ServerWebExchange,
        chain: WebFilterChain,
    ): Mono<Void> =
        ReactiveSecurityContextHolder
            .getContext()
            .mapNotNull { it.authentication } // Elk JWT-gebonden token wordt gecontroleerd, ook als isAuthenticated=false: fail-closed, geen stille doorlaat.
            .filter { it is JwtAuthenticationToken }
            .cast(JwtAuthenticationToken::class.java)
            .flatMap { auth ->
                TenantContext.currentMono().map { hostTenant ->
                    val issuerTenant = directory.tenantForIssuer(auth.token.issuer?.toString())
                    (issuerTenant == hostTenant).also {
                        if (!it) logger.warn { "securityevent=tenant-mismatch hostTenant=$hostTenant issuerTenant=$issuerTenant" }
                    }
                }
            }.defaultIfEmpty(true) // geen token: anoniem pad, alleen de host telt
            .flatMap { consistent ->
                if (consistent) {
                    chain.filter(exchange)
                } else {
                    exchange.response.statusCode = HttpStatus.FORBIDDEN
                    exchange.response.setComplete()
                }
            }

    companion object {
        const val ORDER = -50
    }
}