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
package nl.nlportal.haalcentraalbrp.autoconfiguration

import nl.nlportal.haalcentraalbrp.client.HaalCentraalBrpBewoningClient
import nl.nlportal.haalcentraalbrp.client.HaalCentraalBrpClient
import nl.nlportal.haalcentraalbrp.service.HaalCentraalBrpService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

internal class HaalCentraalBrpAutoConfigurationTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(HaalCentraalBrpAutoConfiguration::class.java))
            .withUserConfiguration(WebClientConfiguration::class.java)

    @Test
    fun `clients en service zijn beschikbaar als de module aan staat`() {
        contextRunner
            .withPropertyValues("nl-portal.config.haalcentraal-brp.enabled=true")
            .run { context ->
                assertThat(context).hasSingleBean(HaalCentraalBrpClient::class.java)
                assertThat(context).hasSingleBean(HaalCentraalBrpBewoningClient::class.java)
                assertThat(context).hasSingleBean(HaalCentraalBrpService::class.java)
            }
    }

    @Test
    fun `er is niets beschikbaar als de module uit staat`() {
        contextRunner.run { context ->
            assertThat(context).doesNotHaveBean(HaalCentraalBrpClient::class.java)
            assertThat(context).doesNotHaveBean(HaalCentraalBrpBewoningClient::class.java)
            assertThat(context).doesNotHaveBean(HaalCentraalBrpService::class.java)
        }
    }

    @Configuration
    internal class WebClientConfiguration {
        @Bean
        fun webClientBuilder(): WebClient.Builder = WebClient.builder()
    }
}