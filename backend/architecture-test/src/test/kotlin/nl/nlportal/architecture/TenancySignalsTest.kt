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
package nl.nlportal.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TenancySignalsTest {
    private val base = "nl.nlportal.architecture.fixtures.signals"

    private fun signalsOf(
        pkg: String,
        layer: Layer = Layer.FEATURE,
    ) = TenancySignals.of(
        ClassFileImporter().importPackages("$base.$pkg"),
        listOf(ModuleMapping(":m", "$base.$pkg", layer)),
    )

    @Test
    fun `a module that builds its own WebClient is flagged`() {
        assertThat(signalsOf("webclient")).containsEntry(":m", setOf(TenancySignal.OWN_WEBCLIENT))
    }

    @Test
    fun `core may build a WebClient`() {
        assertThat(signalsOf("webclient", Layer.CORE)).doesNotContainKey(":m")
    }

    @Test
    fun `a module with JVM-global state is flagged`() {
        assertThat(signalsOf("global")).containsEntry(":m", setOf(TenancySignal.GLOBAL_STATE))
    }

    @Test
    fun `configuration properties used outside the autoconfiguration is flagged`() {
        assertThat(signalsOf("config")).containsEntry(":m", setOf(TenancySignal.CONFIG_SINGLETON))
    }

    @Test
    fun `a module without signals is not listed`() {
        assertThat(signalsOf("clean")).isEmpty()
    }
}