/*
 * Copyright 2015-2024 Den Haag, Ritense, the Netherlands.
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
package nl.nlportal.berichten.autoconfigure

import nl.nlportal.berichten.BERICHTEN_MODULE
import nl.nlportal.berichten.graphql.BerichtenQuery
import nl.nlportal.berichten.security.config.BerichtDocumentResourceHttpSecurityConfigurer
import nl.nlportal.berichten.service.BerichtenService
import nl.nlportal.berichten.web.rest.BerichtDocumentResource
import nl.nlportal.core.modulegate.ModuleFlag
import nl.nlportal.core.modulegate.ModuleGate
import nl.nlportal.core.security.config.HttpSecurityConfigurer
import nl.nlportal.documentenapi.service.DocumentenApiService
import nl.nlportal.zgw.objectenapi.service.ObjectenApiService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@AutoConfiguration
@EnableConfigurationProperties(BerichtenConfiguration::class)
@ConditionalOnProperty(prefix = "nl-portal.config", name = ["documentenapis.enabled"], havingValue = "true")
class BerichtenAutoConfiguration {
    /** Lets the frontend module list and the module status know the module flag. */
    @Bean
    fun berichtenModuleFlag(): ModuleFlag = BERICHTEN_MODULE

    @Bean
    fun berichtenService(
        objectenApiService: ObjectenApiService,
        documentenApiService: DocumentenApiService,
        berichtenConfigurationProperties: BerichtenConfiguration,
    ): BerichtenService =
        BerichtenService(
            objectenApiService = objectenApiService,
            berichtenConfigurationProperties = berichtenConfigurationProperties.properties,
            documentenApiService = documentenApiService,
        )

    @Bean
    @ConditionalOnMissingBean(BerichtenQuery::class)
    fun berichtenQuery(
        berichtenService: BerichtenService,
        moduleGate: ModuleGate,
    ): BerichtenQuery = BerichtenQuery(berichtenService, moduleGate)

    @Bean
    @ConditionalOnMissingBean(BerichtDocumentResource::class)
    fun berichtDocumentResource(
        berichtenService: BerichtenService,
        moduleGate: ModuleGate,
    ): BerichtDocumentResource = BerichtDocumentResource(berichtenService, moduleGate)

    @Bean
    @ConditionalOnMissingBean(BerichtDocumentResourceHttpSecurityConfigurer::class)
    fun berichtDocumentResourceHttpSecurityConfigurer(): HttpSecurityConfigurer = BerichtDocumentResourceHttpSecurityConfigurer()
}