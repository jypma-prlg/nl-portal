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
package nl.nlportal.core.frontend

import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import nl.nlportal.core.modulegate.ModuleFlag
import nl.nlportal.core.util.Mapper
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import tools.jackson.databind.JsonNode

/** The `modules` list of the frontend follows the module flag, not the old `nl-portal.config.<module>.enabled` property. */
@SpringBootTest(properties = ["nl-portal.config.voorbeeld.enabled=true"])
@AutoConfigureWebTestClient(timeout = "36000")
@Import(FrontendModulesFlagTest.VoorbeeldModule::class)
class FrontendModulesFlagTest(
    @Autowired private val webTestClient: WebTestClient,
) {
    @TestConfiguration
    class VoorbeeldModule {
        @Bean
        fun voorbeeldModuleFlag() = ModuleFlag("voorbeeld")
    }

    @Test
    fun `a flag-gated module is listed as enabled or disabled by its flag, whatever the old property says`() {
        writeFlagFile(enabled = false)
        await().atMost(Duration.ofSeconds(10)).untilAsserted {
            assertEquals(false, voorbeeldModule().requiredAt("/enabled").booleanValue())
        }

        writeFlagFile(enabled = true)
        await().atMost(Duration.ofSeconds(10)).untilAsserted {
            assertEquals(true, voorbeeldModule().requiredAt("/enabled").booleanValue())
        }
    }

    private fun voorbeeldModule(): JsonNode {
        val body =
            webTestClient
                .get()
                .uri("/api/public/frontend")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .returnResult()
                .responseBody
        return Mapper
            .get()
            .readTree(body)
            .requiredAt("/modules/voorbeeld")
    }

    companion object {
        private val flagFile: Path = Files.createTempFile("module-flags", ".flagd.json")

        private fun writeFlagFile(enabled: Boolean) {
            Files.writeString(
                flagFile,
                """
                {
                  "${'$'}schema": "https://flagd.dev/schema/v0/flags.json",
                  "flags": {
                    "ops.voorbeeld.enabled": {
                      "state": "ENABLED",
                      "variants": { "on": true, "off": false },
                      "defaultVariant": "${if (enabled) "on" else "off"}"
                    }
                  }
                }
                """.trimIndent(),
            )
        }

        @JvmStatic
        @DynamicPropertySource
        fun featureFlagProperties(registry: DynamicPropertyRegistry) {
            writeFlagFile(enabled = false)
            registry.add("nl-portal.feature-flags.file") { flagFile.toString() }
            registry.add("nl-portal.feature-flags.poll-interval-ms") { 200 }
        }
    }
}