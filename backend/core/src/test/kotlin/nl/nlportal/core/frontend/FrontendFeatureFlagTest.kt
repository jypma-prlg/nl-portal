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

@SpringBootTest
@AutoConfigureWebTestClient(timeout = "36000")
class FrontendFeatureFlagTest(
    @Autowired private val webTestClient: WebTestClient,
) {
    @Test
    fun `message count toggle follows the flag file, also after the file changes`() {
        writeFlagFile(messageCountEnabled = false)
        await().atMost(Duration.ofSeconds(10)).untilAsserted {
            assertEquals(false, messageCountToggle())
        }

        writeFlagFile(messageCountEnabled = true)
        await().atMost(Duration.ofSeconds(10)).untilAsserted {
            assertEquals(true, messageCountToggle())
        }
    }

    private fun messageCountToggle(): Boolean? {
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
            .requiredAt("/toggles/messageCountEnabled")
            .booleanValue()
    }

    companion object {
        private val flagFile: Path = Files.createTempFile("feature-flags", ".flagd.json")

        private fun writeFlagFile(messageCountEnabled: Boolean) {
            val default = if (messageCountEnabled) "on" else "off"
            Files.writeString(
                flagFile,
                """
                {
                  "${'$'}schema": "https://flagd.dev/schema/v0/flags.json",
                  "flags": {
                    "ops.frontend.message-count": {
                      "state": "ENABLED",
                      "variants": { "on": true, "off": false },
                      "defaultVariant": "$default"
                    }
                  }
                }
                """.trimIndent(),
            )
        }

        @JvmStatic
        @DynamicPropertySource
        fun featureFlagProperties(registry: DynamicPropertyRegistry) {
            writeFlagFile(messageCountEnabled = false)
            registry.add("nl-portal.feature-flags.file") { flagFile.toString() }
            registry.add("nl-portal.feature-flags.poll-interval-ms") { 200 }
        }
    }
}