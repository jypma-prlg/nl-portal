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
package nl.nlportal.haalcentraalbrp.autoconfiguration

import nl.nlportal.haalcentraalbrp.graphql.HaalCentraalBrpQuery
import nl.nlportal.haalcentraalbrp.service.HaalCentraalBrpService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

internal class HaalCentraalBrpGraphQLConfigurationTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(HaalCentraalBrpGraphQLConfiguration::class.java))
            .withUserConfiguration(ServiceConfiguration::class.java)

    @Test
    fun `brp query is beschikbaar als de module aan staat`() {
        contextRunner
            .withPropertyValues("nl-portal.config.haalcentraal-brp.enabled=true")
            .run { context ->
                assertThat(context).hasSingleBean(HaalCentraalBrpQuery::class.java)
            }
    }

    @Test
    fun `brp query ontbreekt als de module uit staat`() {
        contextRunner.run { context ->
            assertThat(context).doesNotHaveBean(HaalCentraalBrpQuery::class.java)
        }
    }

    @Configuration
    internal class ServiceConfiguration {
        @Bean
        fun haalCentraalBrpService(): HaalCentraalBrpService = mock()
    }
}