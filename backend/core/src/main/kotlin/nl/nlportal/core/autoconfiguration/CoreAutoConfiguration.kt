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
package nl.nlportal.core.autoconfiguration

import nl.nlportal.core.featureflag.FeatureContextFactory
import nl.nlportal.core.featureflag.FeatureGate
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties
import nl.nlportal.core.frontend.configuration.FrontendModuleConfigurationProperties
import nl.nlportal.core.frontend.service.FrontendConfigurationService
import nl.nlportal.core.frontend.web.rest.FrontendConfigurationResource
import nl.nlportal.core.frontend.web.rest.PingResource
import nl.nlportal.core.util.Mapper
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import tools.jackson.databind.json.JsonMapper

@AutoConfiguration
@EnableConfigurationProperties(
    FrontendModuleConfigurationProperties::class,
    FrontendConfigurationProperties::class,
)
class CoreAutoConfiguration {
    @Bean("objectMapper")
    @ConditionalOnMissingBean(name = ["jsonMapper"])
    fun objectMapper(): JsonMapper = Mapper.get()

    @Bean
    @ConditionalOnMissingBean(FrontendConfigurationService::class)
    fun frontendConfigurationService(
        frontendConfigurationProperties: FrontendConfigurationProperties,
        frontendModuleConfigurationProperties: FrontendModuleConfigurationProperties,
        featureGate: FeatureGate,
    ) = FrontendConfigurationService(
        frontendConfigurationProperties = frontendConfigurationProperties,
        frontendModuleConfigurationProperties = frontendModuleConfigurationProperties,
        featureGate = featureGate,
    )

    @Bean
    @ConditionalOnMissingBean(FrontendConfigurationResource::class)
    fun frontendConfigurationResource(
        frontendConfigurationService: FrontendConfigurationService,
        featureContextFactory: FeatureContextFactory,
    ) = FrontendConfigurationResource(frontendConfigurationService, featureContextFactory)

    @Bean
    @ConditionalOnProperty(prefix = "nl-portal.support", name = ["ping-endpoint.enabled"], havingValue = "true")
    @ConditionalOnMissingBean(PingResource::class)
    fun pingResource(): PingResource = PingResource()
}