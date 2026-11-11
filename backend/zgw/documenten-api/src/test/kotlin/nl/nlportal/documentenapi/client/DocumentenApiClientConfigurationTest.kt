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
package nl.nlportal.documentenapi.client

import kotlinx.coroutines.runBlocking
import nl.nlportal.core.util.ModuleNotConfiguredException
import nl.nlportal.documentenapi.client.DocumentApisConfig.DocumentenApisConfigProperties
import nl.nlportal.documentenapi.client.DocumentApisConfig.DocumentenApisConfigProperties.DocumentApiConfig
import nl.nlportal.core.koppeling.ZgwJwtGenerator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class DocumentenApiClientConfigurationTest {
    @Test
    fun `unknown document api fails clearly`() {
        val client = DocumentenApiClient(DocumentenApisConfigProperties(), ZgwJwtGenerator())
        assertThrows<ModuleNotConfiguredException> { runBlocking { client.getDocument(UUID.randomUUID(), "openzaak") } }
    }

    @Test
    fun `document api without url fails clearly`() {
        val properties = DocumentenApisConfigProperties().apply { configurations = mapOf("openzaak" to DocumentApiConfig()) }
        val client = DocumentenApiClient(properties, ZgwJwtGenerator())
        assertThrows<ModuleNotConfiguredException> { runBlocking { client.getDocument(UUID.randomUUID(), "openzaak") } }
    }
}