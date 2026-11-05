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
package nl.nlportal.zgw.objectenapi.client

import nl.nlportal.core.util.ModuleNotConfiguredException
import nl.nlportal.zgw.objectenapi.autoconfiguration.ObjectsApiClientConfig.ObjectsApiClientConfigProperties
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class ObjectsApiClientConfigurationTest {
    @Test
    fun `client can be created without configuration`() {
        assertDoesNotThrow { ObjectsApiClient(ObjectsApiClientConfigProperties()) }
    }

    @Test
    fun `client fails clearly on use without configuration`() {
        val client = ObjectsApiClient(ObjectsApiClientConfigProperties())
        assertThrows<ModuleNotConfiguredException> { client.webClient() }
        assertThrows<ModuleNotConfiguredException> { client.webClientWithoutBaseUrl() }
    }
}