/*
 * Copyright 2015-2023 Den Haag, Ritense, the Netherlands.
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
package nl.nlportal.commonground.authentication

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import io.github.oshai.kotlinlogging.KotlinLogging
import nl.nlportal.commonground.authentication.exception.TokenExchangeException
import nl.nlportal.commonground.authentication.exception.UserTypeUnsupportedException
import nl.nlportal.portal.authentication.domain.PortalAuthentication
import nl.nlportal.portal.authentication.domain.SUB_KEY
import nl.nlportal.portal.authentication.service.PortalAuthenticationConverter
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.net.URI

class CommonGroundAuthenticationConverter(
    val decoder: ReactiveJwtDecoder,
    val keycloakConfig: KeycloakConfig,
) : PortalAuthenticationConverter() {
    private val jwtGrantedAuthoritiesConverter = JwtGrantedAuthoritiesConverter()
    private val webClient = WebClient.create()

    override fun convert(jwt: Jwt): Mono<PortalAuthentication> {
        return tokenExchange(jwt).flatMap {
            decoder.decode(it.accessToken).map { exchangedJwt ->
                val aanvrager = exchangedJwt.claims[AANVRAGER_KEY]

                if (aanvrager is Map<*, *>) {
                    if (aanvrager[BSN_KEY] != null) {
                        return@map BurgerAuthentication(exchangedJwt, jwtGrantedAuthoritiesConverter.convert(exchangedJwt))
                    } else if (aanvrager[KVK_NUMMER_KEY] != null) {
                        return@map BedrijfAuthentication(exchangedJwt, jwtGrantedAuthoritiesConverter.convert(exchangedJwt))
                    } else if (aanvrager[SUB_KEY] != null) {
                        return@map KeycloakUserAuthentication(exchangedJwt, jwtGrantedAuthoritiesConverter.convert(exchangedJwt))
                    }
                }

                // This block is for temporary backwards compatibility
                if (exchangedJwt.claims[BSN_KEY] != null) {
                    return@map BurgerAuthentication(exchangedJwt, jwtGrantedAuthoritiesConverter.convert(exchangedJwt))
                } else if (exchangedJwt.claims[KVK_NUMMER_KEY] != null) {
                    return@map BedrijfAuthentication(exchangedJwt, jwtGrantedAuthoritiesConverter.convert(exchangedJwt))
                } else if (jwt.claims[SUB_KEY] != null) {
                    return@map KeycloakUserAuthentication(jwt, jwtGrantedAuthoritiesConverter.convert(jwt))
                }

                val subject = exchangedJwt.subject
                if (subject == null) {
                    logger.error { "User with unknown subject has no bsn or kvk nummer assigned" }
                } else {
                    logger.error { "User with subject $subject has no bsn or kvk nummer assigned" }
                }

                throw UserTypeUnsupportedException("User type not supported")
            }
        }
    }

    fun tokenExchange(jwt: Jwt): Mono<TokenResponse> =
        webClient
            .post()
            .uri(URI.create("${jwt.issuer.toString().trimEnd('/')}/protocol/openid-connect/token"))
            .body(BodyInserters.fromFormData(tokenExchangeFormData(jwt)))
            .retrieve()
            .onStatus({ it.isError }) { response ->
                response
                    .bodyToMono<String>()
                    .defaultIfEmpty("")
                    .flatMap { body ->
                        logger.error { "Token exchange failed with status ${response.statusCode()}: $body" }
                        Mono.error(TokenExchangeException())
                    }
            }.bodyToMono<TokenResponse>()

    private fun tokenExchangeFormData(jwt: Jwt): MultiValueMap<String, String> {
        val formData = LinkedMultiValueMap<String, String>()
        formData.add("client_id", keycloakConfig.resource)
        formData.add("client_secret", keycloakConfig.credentials.secret)
        formData.add("grant_type", GRANT_TYPE_TOKEN_EXCHANGE)
        formData.add("subject_token", jwt.tokenValue)
        formData.add("requested_token_type", TOKEN_TYPE_ACCESS_TOKEN)
        formData.add("subject_token_type", TOKEN_TYPE_ACCESS_TOKEN)
        keycloakConfig.audience
            .takeIf { it.isNotBlank() }
            ?.let { formData.add("audience", it) }

        return formData
    }

    data class TokenResponse(
        @JsonValue
        @JsonProperty("access_token")
        val accessToken: String,
    )

    companion object {
        val logger = KotlinLogging.logger {}

        const val GRANT_TYPE_TOKEN_EXCHANGE = "urn:ietf:params:oauth:grant-type:token-exchange"
        const val TOKEN_TYPE_ACCESS_TOKEN = "urn:ietf:params:oauth:token-type:access_token"
    }
}