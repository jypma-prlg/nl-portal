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
package nl.nlportal.openproduct.client

import nl.nlportal.openproduct.autoconfigure.OpenProductModuleConfiguration.OpenProductConfigurationProperties.OpenProductDmnConfigurationProperties
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.web.reactive.function.client.WebClient

class OpenProductDmnClientConfigurationTest {
    private val invalidSecret =
        OpenProductDmnConfigurationProperties().apply {
            clientId = "client"
            secret = "te-kort"
        }

    @Test
    fun `dmn client can be created with an invalid secret`() {
        assertDoesNotThrow { OpenProductDmnClient(invalidSecret, null, WebClient.builder()) }
    }

    @Test
    fun `dmn client fails on use with an invalid secret`() {
        val client = OpenProductDmnClient(invalidSecret, null, WebClient.builder())
        assertThrows<IllegalArgumentException> { client.webClient }
    }

    @Test
    fun `dmn client builds a web client without configuration`() {
        val client = OpenProductDmnClient(OpenProductDmnConfigurationProperties(), null, WebClient.builder())
        assertDoesNotThrow { client.webClient }
    }
}