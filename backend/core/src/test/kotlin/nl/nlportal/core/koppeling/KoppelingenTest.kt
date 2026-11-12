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
package nl.nlportal.core.koppeling

import nl.nlportal.core.util.ModuleNotConfiguredException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

class KoppelingenTest {
    @Configuration
    @EnableConfigurationProperties(KoppelingenProperties::class)
    class TestConfiguration

    private val runner = ApplicationContextRunner().withUserConfiguration(TestConfiguration::class.java)

    @Test
    fun `een module zonder gekozen koppeling gebruikt de koppeling met de modulenaam`() {
        runner
            .withPropertyValues("nl-portal.koppelingen.zakenapi.url=https://zaken.example.nl")
            .run { context ->
                val koppeling = context.getBean(KoppelingenProperties::class.java).voorModule("zakenapi", null)

                assertEquals("https://zaken.example.nl", koppeling.url)
            }
    }

    @Test
    fun `een module met een gekozen koppeling gebruikt die koppeling, naast andere koppelingen`() {
        runner
            .withPropertyValues(
                "nl-portal.koppelingen.zakenapi.url=https://zaken.example.nl",
                "nl-portal.koppelingen.vergunningen.url=https://vergunningen.example.nl",
            ).run { context ->
                val koppeling = context.getBean(KoppelingenProperties::class.java).voorModule("zakenapi", "vergunningen")

                assertEquals("https://vergunningen.example.nl", koppeling.url)
            }
    }

    @Test
    fun `de autoconfiguratie levert de koppelingen en de fabriek aan modules`() {
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(KoppelingAutoConfiguration::class.java))
            .withBean(WebClient.Builder::class.java, { WebClient.builder() })
            .withBean(ZgwJwtGenerator::class.java, { ZgwJwtGenerator() })
            .withPropertyValues("nl-portal.koppelingen.zakenapi.url=https://zaken.example.nl")
            .run { context ->
                assertNotNull(context.getBean(KoppelingWebClientFactory::class.java))
                assertEquals("https://zaken.example.nl", context.getBean(KoppelingenProperties::class.java).voorModule("zakenapi", null).url)
            }
    }

    @Test
    fun `leverancier en versie van een koppeling zijn beschikbaar voor gedrag per leverancier`() {
        runner
            .withPropertyValues(
                "nl-portal.koppelingen.zakenapi.url=https://zaken.example.nl",
                "nl-portal.koppelingen.zakenapi.leverancier=Open Zaak",
                "nl-portal.koppelingen.zakenapi.versie=1.31",
            ).run { context ->
                val properties = context.getBean(KoppelingenProperties::class.java)
                val koppeling = properties.voorModule("zakenapi", null)

                assertEquals("Open Zaak", koppeling.leverancier)
                assertEquals("1.31", koppeling.versie)
                assertEquals(setOf("zakenapi"), properties.koppelingen.keys)
            }
    }

    @Test
    fun `een onbekende koppeling geeft pas bij gebruik een duidelijke fout en laat het opstarten slagen`() {
        runner.run { context ->
            assertNull(context.startupFailure)

            val fout =
                assertThrows<ModuleNotConfiguredException> {
                    context.getBean(KoppelingenProperties::class.java).voorModule("zakenapi", "vergunningen")
                }

            assertTrue(fout.message!!.contains("zakenapi"))
            assertTrue(fout.message!!.contains("vergunningen"))
        }
    }
}