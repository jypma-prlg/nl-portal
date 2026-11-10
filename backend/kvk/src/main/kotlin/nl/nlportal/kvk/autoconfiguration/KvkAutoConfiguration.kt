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
package nl.nlportal.kvk.autoconfiguration

import nl.nlportal.core.ssl.ClientSslContextResolver
import nl.nlportal.core.ssl.ResourceClientSslContextResolver
import nl.nlportal.kvk.client.KvkConfig
import nl.nlportal.kvk.client.KvkClient
import nl.nlportal.kvk.graphql.KvkQuery
import nl.nlportal.kvk.service.KvkService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ResourceLoader

@AutoConfiguration
@EnableConfigurationProperties(KvkConfig::class)
@ConditionalOnProperty(prefix = "nl-portal.config", name = ["kvk.enabled"], havingValue = "true")
class KvkAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(ClientSslContextResolver::class)
    fun clientSslContextResolver(resourceLoader: ResourceLoader): ClientSslContextResolver = ResourceClientSslContextResolver(resourceLoader)

    @Bean
    @ConditionalOnMissingBean(KvkClient::class)
    fun kvkClient(
        kvkClientConfig: KvkConfig,
        @Autowired(required = false) clientSslContextResolver: ClientSslContextResolver? = null,
    ): KvkClient =
        KvkClient(
            kvkClientConfig.properties,
            clientSslContextResolver,
        )

    @Bean
    @ConditionalOnMissingBean(KvkService::class)
    fun kvkService(kvkClient: KvkClient): KvkService = KvkService(kvkClient)

    @Bean
    @ConditionalOnMissingBean(KvkQuery::class)
    fun kvkQuery(kvkService: KvkService): KvkQuery = KvkQuery(kvkService)
}