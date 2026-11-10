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
package nl.nlportal.besluiten.autoconfigure

import nl.nlportal.besluiten.client.BesluitenApiClient
import nl.nlportal.besluiten.client.BesluitenApiConfig
import nl.nlportal.besluiten.graphql.BesluitenQuery
import nl.nlportal.besluiten.service.BesluitenService
import nl.nlportal.catalogiapi.service.CatalogiApiService
import nl.nlportal.documentenapi.service.DocumentenApiService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.function.client.WebClient

@AutoConfiguration
@EnableConfigurationProperties(BesluitenApiConfig::class)
@ConditionalOnProperty(prefix = "nl-portal.config", name = ["besluitenapi.enabled", "documentenapis.enabled", "zakenapi.enabled"], havingValue = "true")
class BesluitenAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(BesluitenApiClient::class)
    fun besluitenApiClient(
        besluitenApiConfig: BesluitenApiConfig,
        webClientBuilder: WebClient.Builder,
    ): BesluitenApiClient = BesluitenApiClient(besluitenApiConfig.properties, webClientBuilder)

    @Bean
    @ConditionalOnMissingBean(BesluitenService::class)
    fun besluitenService(
        besluitenApiClient: BesluitenApiClient,
        documentenApiService: DocumentenApiService,
    ): BesluitenService =
        BesluitenService(
            besluitenApiClient = besluitenApiClient,
            documentenApiService = documentenApiService,
        )

    @Bean
    @ConditionalOnMissingBean(BesluitenQuery::class)
    fun besluitenQuery(
        besluitenService: BesluitenService,
        catalogiApiService: CatalogiApiService,
    ): BesluitenQuery =
        BesluitenQuery(
            besluitenService = besluitenService,
            catalogiApiService = catalogiApiService,
        )
}