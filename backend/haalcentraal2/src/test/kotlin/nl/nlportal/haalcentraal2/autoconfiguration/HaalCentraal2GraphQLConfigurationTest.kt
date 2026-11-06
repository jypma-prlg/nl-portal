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
package nl.nlportal.haalcentraal2.autoconfiguration

import nl.nlportal.haalcentraal.hr.service.HandelsregisterService
import nl.nlportal.haalcentraal2.graphql.HaalCentraal2GemachtigdeQuery
import nl.nlportal.haalcentraal2.service.HaalCentraal2Service
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

internal class HaalCentraal2GraphQLConfigurationTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(HaalCentraal2GraphQLConfiguration::class.java))
            .withUserConfiguration(ServiceConfiguration::class.java)
            .withPropertyValues("nl-portal.config.haalcentraal2.enabled=true")

    @Test
    fun `gemachtigde query is beschikbaar als de handelsregister-module aan staat`() {
        contextRunner
            .withPropertyValues("nl-portal.config.haalcentraal.hr.enabled=true")
            .run { context ->
                assertThat(context).hasSingleBean(HaalCentraal2GemachtigdeQuery::class.java)
            }
    }

    @Test
    fun `gemachtigde query ontbreekt als de handelsregister-module uit staat`() {
        contextRunner.run { context ->
            assertThat(context).doesNotHaveBean(HaalCentraal2GemachtigdeQuery::class.java)
        }
    }

    @Configuration
    internal class ServiceConfiguration {
        @Bean
        fun haalCentraal2Service(): HaalCentraal2Service = mock()

        @Bean
        fun handelsregisterService(): HandelsregisterService = mock()
    }
}