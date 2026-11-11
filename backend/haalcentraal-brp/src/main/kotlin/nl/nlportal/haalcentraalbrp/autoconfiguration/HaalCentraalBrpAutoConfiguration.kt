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
package nl.nlportal.haalcentraalbrp.autoconfiguration

import nl.nlportal.core.ssl.ClientSslContextResolver
import nl.nlportal.haalcentraalbrp.client.HaalCentraalBrpBewoningClient
import nl.nlportal.haalcentraalbrp.client.HaalCentraalBrpClient
import nl.nlportal.haalcentraalbrp.service.HaalCentraalBrpService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.function.client.WebClient

@AutoConfiguration
@EnableConfigurationProperties(HaalCentraalBrpModuleConfiguration::class)
@ConditionalOnProperty(prefix = "nl-portal.config.haalcentraal-brp", name = ["enabled"], havingValue = "true")
class HaalCentraalBrpAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(HaalCentraalBrpBewoningClient::class)
    fun haalCentraalBrpBewoningClient(
        haalCentraalBrpModuleConfiguration: HaalCentraalBrpModuleConfiguration,
        @Autowired(required = false) clientSslContextResolver: ClientSslContextResolver? = null,
        webClientBuilder: WebClient.Builder,
    ): HaalCentraalBrpBewoningClient =
        HaalCentraalBrpBewoningClient(
            haalCentraalBrpConfigurationProperties = haalCentraalBrpModuleConfiguration.properties,
            clientSslContextResolver = clientSslContextResolver,
            webClientBuilder = webClientBuilder,
        )

    @Bean
    @ConditionalOnMissingBean(HaalCentraalBrpClient::class)
    fun haalCentraalBrpClient(
        haalCentraalBrpModuleConfiguration: HaalCentraalBrpModuleConfiguration,
        @Autowired(required = false) clientSslContextResolver: ClientSslContextResolver? = null,
        webClientBuilder: WebClient.Builder,
    ): HaalCentraalBrpClient =
        HaalCentraalBrpClient(
            haalCentraalBrpConfigurationProperties = haalCentraalBrpModuleConfiguration.properties,
            clientSslContextResolver = clientSslContextResolver,
            webClientBuilder = webClientBuilder,
        )

    @Bean
    @ConditionalOnMissingBean(HaalCentraalBrpService::class)
    fun haalCentraalBrpService(
        haalCentraalBrpModuleConfiguration: HaalCentraalBrpModuleConfiguration,
        haalCentraalBrpClient: HaalCentraalBrpClient,
        haalCentraalBrpBewoningClient: HaalCentraalBrpBewoningClient,
    ): HaalCentraalBrpService =
        HaalCentraalBrpService(
            haalCentraalBrpConfigurationProperties = haalCentraalBrpModuleConfiguration.properties,
            haalCentraalBrpClient = haalCentraalBrpClient,
            haalCentraalBrpBewoningClient = haalCentraalBrpBewoningClient,
        )
}