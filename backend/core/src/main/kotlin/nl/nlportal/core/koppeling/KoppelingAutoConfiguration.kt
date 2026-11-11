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

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.function.client.WebClient

/** Registreert de koppelingen en de fabriek waarmee modules er een geauthenticeerde [WebClient] van maken. */
@AutoConfiguration
@EnableConfigurationProperties(KoppelingenProperties::class)
class KoppelingAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(KoppelingWebClientFactory::class)
    fun koppelingWebClientFactory(
        webClientBuilder: WebClient.Builder,
        zgwJwtGenerator: ZgwJwtGenerator,
    ) = KoppelingWebClientFactory(webClientBuilder, zgwJwtGenerator)
}