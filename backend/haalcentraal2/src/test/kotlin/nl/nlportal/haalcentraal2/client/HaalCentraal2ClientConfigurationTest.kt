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
package nl.nlportal.haalcentraal2.client

import nl.nlportal.core.util.ModuleNotConfiguredException
import nl.nlportal.haalcentraal2.autoconfiguration.HaalCentraal2ModuleConfiguration.HaalCentraal2ConfigurationProperties
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.web.reactive.function.client.WebClient

class HaalCentraal2ClientConfigurationTest {
    private val unconfigured = HaalCentraal2ConfigurationProperties()

    @Test
    fun `clients can be created without configuration`() {
        assertDoesNotThrow {
            HaalCentraal2BrpClient(unconfigured, null, WebClient.builder())
            HaalCentraal2BewoningClient(unconfigured, null, WebClient.builder())
        }
    }

    @Test
    fun `brp client fails clearly on use without configuration`() {
        val client = HaalCentraal2BrpClient(unconfigured, null, WebClient.builder())
        assertThrows<ModuleNotConfiguredException> { client.webClient }
    }

    @Test
    fun `bewoning client fails clearly on use without configuration`() {
        val client = HaalCentraal2BewoningClient(unconfigured, null, WebClient.builder())
        assertThrows<ModuleNotConfiguredException> { client.webClient }
    }
}