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
package nl.nlportal.kvk.autoconfiguration

import nl.nlportal.core.ssl.ClientSslContextResolver
import nl.nlportal.kvk.client.KvkClient
import nl.nlportal.kvk.graphql.KvkQuery
import nl.nlportal.kvk.service.KvkService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner

internal class KvkAutoConfigurationTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(KvkAutoConfiguration::class.java))

    @Test
    fun `client, service en query zijn beschikbaar als de module aan staat`() {
        contextRunner
            .withPropertyValues("nl-portal.config.kvk.enabled=true")
            .run { context ->
                assertThat(context).hasSingleBean(ClientSslContextResolver::class.java)
                assertThat(context).hasSingleBean(KvkClient::class.java)
                assertThat(context).hasSingleBean(KvkService::class.java)
                assertThat(context).hasSingleBean(KvkQuery::class.java)
            }
    }

    @Test
    fun `er is niets beschikbaar als de module uit staat`() {
        contextRunner.run { context ->
            assertThat(context).doesNotHaveBean(KvkClient::class.java)
            assertThat(context).doesNotHaveBean(KvkService::class.java)
            assertThat(context).doesNotHaveBean(KvkQuery::class.java)
        }
    }
}