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
package nl.nlportal.berichten

import dev.openfeature.sdk.ImmutableContext
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import nl.nlportal.core.modulegate.ModuleGate
import org.awaitility.Awaitility.await
import org.springframework.test.context.DynamicPropertyRegistry

/**
 * The flagd file of the berichten tests. All test contexts share this one file, because OpenFeature is a
 * JVM-wide singleton: contexts that would each have their own file could replace each other's provider.
 * The module is on by default; a test that needs it off calls [switchTo] and restores it afterwards.
 */
object BerichtenFlagFile {
    private val file: Path = Files.createTempFile("berichten-flags", ".flagd.json").also { write(it, enabled = true) }

    fun register(registry: DynamicPropertyRegistry) {
        registry.add("nl-portal.feature-flags.file") { file.toString() }
        registry.add("nl-portal.feature-flags.poll-interval-ms") { 200 }
    }

    /** Writes the flag and waits until the running application sees the new state. */
    fun switchTo(
        enabled: Boolean,
        moduleGate: ModuleGate,
    ) {
        write(file, enabled)
        await().atMost(Duration.ofSeconds(10)).until { moduleGate.isEnabled(BERICHTEN_MODULE, ImmutableContext()) == enabled }
    }

    private fun write(
        file: Path,
        enabled: Boolean,
    ) {
        Files.writeString(
            file,
            """
            {
              "${'$'}schema": "https://flagd.dev/schema/v0/flags.json",
              "flags": {
                "${BERICHTEN_MODULE.flag.key}": {
                  "state": "ENABLED",
                  "variants": { "on": true, "off": false },
                  "defaultVariant": "${if (enabled) "on" else "off"}"
                }
              }
            }
            """.trimIndent(),
        )
    }
}