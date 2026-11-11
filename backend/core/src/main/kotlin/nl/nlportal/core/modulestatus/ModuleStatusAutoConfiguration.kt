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
package nl.nlportal.core.modulestatus

import io.github.oshai.kotlinlogging.KotlinLogging
import nl.nlportal.core.frontend.configuration.FrontendModuleConfigurationProperties
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint
import org.springframework.core.env.Environment
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.boot.security.autoconfigure.actuate.web.reactive.EndpointRequest
import reactor.core.publisher.Mono

@AutoConfiguration
@EnableConfigurationProperties(BeheerProperties::class)
class ModuleStatusAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun moduleStatusRegistry() = ModuleStatusRegistry()

    @Bean
    @ConditionalOnAvailableEndpoint(endpoint = ModuleStatusEndpoint::class)
    fun moduleStatusEndpoint(
        registry: ModuleStatusRegistry,
        modules: FrontendModuleConfigurationProperties,
    ) = ModuleStatusEndpoint(registry, modules)

    /**
     * Eigen keten voor het beheerendpoint, los van de JWT-keten van de gebruikers: HTTP Basic met een beheerrol.
     * Zonder geconfigureerde gebruiker is er geen account en dus geen toegang.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    fun beheerSecurityWebFilterChain(
        http: ServerHttpSecurity,
        properties: BeheerProperties,
        environment: Environment,
    ): SecurityWebFilterChain {
        properties.waarschuwingen(environment.getProperty("management.server.port") != null).forEach { logger.warn { it } }
        val inloggegevens = properties.inloggegevens()
        val basisManager: ReactiveAuthenticationManager =
            if (inloggegevens == null) {
                // Geen account: nooit authenticeren (fail closed), maar het opstarten blijft slagen.
                ReactiveAuthenticationManager { Mono.error(BadCredentialsException("Beheer niet geconfigureerd")) }
            } else {
                val gebruiker =
                    User
                        .withUsername(inloggegevens.first)
                        .password(inloggegevens.second)
                        .roles(BEHEER_ROL)
                        .build()
                UserDetailsRepositoryReactiveAuthenticationManager(MapReactiveUserDetailsService(gebruiker))
            }

        val manager = ReactiveAuthenticationManager { auth -> basisManager.authenticate(auth).doOnError { logger.warn { "Mislukte aanmelding op het beheerendpoint" } } }

        return http
            .securityMatcher(EndpointRequest.to(ModuleStatusEndpoint::class.java))
            .csrf { it.disable() }
            .authenticationManager(manager)
            .httpBasic { }
            .authorizeExchange { it.anyExchange().hasRole(BEHEER_ROL) }
            .build()
    }

    private companion object {
        val logger = KotlinLogging.logger {}
        const val BEHEER_ROL = "BEHEER"
    }
}