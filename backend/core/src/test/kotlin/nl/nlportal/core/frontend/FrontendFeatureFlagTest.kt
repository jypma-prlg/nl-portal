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
import nl.nlportal.core.util.Mapper
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import tools.jackson.databind.JsonNode

@SpringBootTest
@AutoConfigureWebTestClient(timeout = "36000")
class FrontendFeatureFlagTest(
    @Autowired private val webTestClient: WebTestClient,
) {
    @Test
    fun `every frontend toggle follows its flag, also after the file changes`() {
        writeFlagFile(enabled = false)
        await().atMost(Duration.ofSeconds(10)).untilAsserted {
            val toggles = frontendToggles()
            TOGGLE_FLAGS.keys.forEach { assertEquals(false, toggles.requiredAt("/$it").booleanValue(), it) }
            assertEquals("{}", toggles.requiredAt("/custom").stringValue())
        }

        writeFlagFile(enabled = true)
        await().atMost(Duration.ofSeconds(10)).untilAsserted {
            val toggles = frontendToggles()
            TOGGLE_FLAGS.keys.forEach { assertEquals(true, toggles.requiredAt("/$it").booleanValue(), it) }
            assertEquals(mapOf("betaBanner" to true), Mapper.get().readValue(toggles.requiredAt("/custom").stringValue(), Map::class.java))
        }
    }

    private fun frontendToggles(): JsonNode {
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
            .requiredAt("/toggles")
    }

    companion object {
        private val flagFile: Path = Files.createTempFile("feature-flags", ".flagd.json")

        private val TOGGLE_FLAGS =
            mapOf(
                "messageCountEnabled" to "ops.frontend.message-count",
                "casesPartialSearchEnabled" to "ops.frontend.cases-partial-search",
                "openProductEnabled" to "ops.frontend.open-product",
                "casesResultExplanationEnabled" to "ops.frontend.cases-result-explanation",
                "myInhabitantCountEnabled" to "ops.frontend.my-inhabitant-count",
                "casesContactMomentsEnabled" to "ops.frontend.cases-contact-moments",
                "overviewIntroEnabled" to "ops.frontend.overview-intro",
                "overviewMaintenanceAlertEnabled" to "ops.frontend.overview-maintenance-alert",
            )

        private fun writeFlagFile(enabled: Boolean) {
            val default = if (enabled) "on" else "off"
            val booleanFlags =
                TOGGLE_FLAGS.values.joinToString(",") {
                    "\"$it\": { \"state\": \"ENABLED\", \"variants\": { \"on\": true, \"off\": false }, \"defaultVariant\": \"$default\" }"
                }
            val customVariant = if (enabled) """{ "betaBanner": true }""" else "{}"
            Files.writeString(
                flagFile,
                """
                {
                  "${'$'}schema": "https://flagd.dev/schema/v0/flags.json",
                  "flags": {
                    $booleanFlags,
                    "ops.frontend.custom-toggles": {
                      "state": "ENABLED",
                      "variants": { "current": $customVariant },
                      "defaultVariant": "current"
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