/*
 * Copyright 2015-2026 Den Haag, Ritense, the Netherlands.
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
package nl.nlportal.core.koppeling

import io.github.oshai.kotlinlogging.KotlinLogging
import nl.nlportal.core.util.requireConfigured
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant

/** Bouwt uit een [Koppeling] een [WebClient] met de authenticatie van die koppeling. */
class KoppelingWebClientFactory(
    private val webClientBuilder: WebClient.Builder,
    private val zgwJwtGenerator: ZgwJwtGenerator = ZgwJwtGenerator(),
) {
    fun webClient(
        modulenaam: String,
        koppeling: Koppeling,
    ): WebClient {
        val builder = webClientBuilder.clone().baseUrl(koppeling.url.requireConfigured(modulenaam, "url"))
        if (koppeling.isOnbeveiligd()) {
            logger.warn { "Koppeling van module '$modulenaam' gebruikt http naar een publiek adres; gebruik https." }
        }
        val auth = koppeling.auth
        when (auth.type.requireConfigured(modulenaam, "auth.type")) {
            KoppelingAuthType.API_KEY -> {
                builder.defaultHeader(
                    auth.headerName.requireConfigured(modulenaam, "auth.header-name"),
                    auth.value.requireConfigured(modulenaam, "auth.value"),
                )
            }

            KoppelingAuthType.BEARER -> {
                builder.defaultHeader(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer ${auth.token.requireConfigured(modulenaam, "auth.token")}",
                )
            }

            KoppelingAuthType.ZGW_JWT -> {
                builder.filter(
                    bearer {
                        Mono.fromCallable {
                            zgwJwtGenerator.generateToken(
                                auth.secret.requireConfigured(modulenaam, "auth.secret"),
                                auth.clientId.requireConfigured(modulenaam, "auth.client-id"),
                            )
                        }
                    },
                )
            }

            KoppelingAuthType.OAUTH2_CLIENT_CREDENTIALS -> {
                builder.filter(
                    bearer(
                        TokenCache {
                            haalToken(
                                auth.tokenUrl.requireConfigured(modulenaam, "auth.token-url"),
                                basicAuth =
                                    auth.clientId.requireConfigured(modulenaam, "auth.client-id") to
                                        auth.secret.requireConfigured(modulenaam, "auth.secret"),
                            )
                        }::token,
                    ),
                )
            }

            KoppelingAuthType.OAUTH2_PRIVATE_KEY_JWT -> {
                builder.filter(
                    bearer(
                        TokenCache {
                            val tokenUrl = auth.tokenUrl.requireConfigured(modulenaam, "auth.token-url")
                            val assertion =
                                ClientAssertion.sign(
                                    auth.clientId.requireConfigured(modulenaam, "auth.client-id"),
                                    tokenUrl,
                                    auth.privateKeyFile.requireConfigured(modulenaam, "auth.private-key-file"),
                                )
                            haalToken(
                                tokenUrl,
                                extraVelden =
                                    mapOf(
                                        "client_assertion_type" to ClientAssertion.TYPE,
                                        "client_assertion" to assertion,
                                    ),
                            )
                        }::token,
                    ),
                )
            }
        }
        return builder.build()
    }

    private fun bearer(token: () -> Mono<String>) =
        ExchangeFilterFunction.ofRequestProcessor { verzoek ->
            token().map { ClientRequest.from(verzoek).header(HttpHeaders.AUTHORIZATION, "Bearer $it").build() }
        }

    private fun haalToken(
        tokenUrl: String,
        basicAuth: Pair<String, String>? = null,
        extraVelden: Map<String, String> = emptyMap(),
    ): Mono<AccessToken> {
        val formulier = LinkedMultiValueMap<String, String>().apply { add("grant_type", "client_credentials") }
        extraVelden.forEach { (naam, waarde) -> formulier.add(naam, waarde) }
        return webClientBuilder
            .clone()
            .build()
            .post()
            .uri(tokenUrl)
            .headers { koppen -> basicAuth?.let { koppen.setBasicAuth(it.first, it.second) } }
            .body(BodyInserters.fromFormData(formulier))
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<Map<String, Any>>() {})
            .map { antwoord ->
                val geldigheid = (antwoord["expires_in"] as? Number)?.toLong() ?: DEFAULT_GELDIGHEID_SECONDEN
                AccessToken(
                    antwoord["access_token"] as String,
                    Instant.now().plus(Duration.ofSeconds(geldigheid)).minus(MARGE),
                )
            }
    }

    private class AccessToken(
        val waarde: String,
        val verloopt: Instant,
    )

    /** Hergebruikt het laatste token tot het (min een marge) verloopt. */
    private class TokenCache(
        private val ophalen: () -> Mono<AccessToken>,
    ) {
        @Volatile
        private var token: AccessToken? = null

        fun token(): Mono<String> =
            Mono.defer {
                token?.takeIf { Instant.now().isBefore(it.verloopt) }?.let { Mono.just(it.waarde) }
                    ?: ophalen().doOnNext { token = it }.map { it.waarde }
            }
    }

    private companion object {
        val logger = KotlinLogging.logger {}
        const val DEFAULT_GELDIGHEID_SECONDEN = 60L
        val MARGE: Duration = Duration.ofSeconds(30)
    }
}