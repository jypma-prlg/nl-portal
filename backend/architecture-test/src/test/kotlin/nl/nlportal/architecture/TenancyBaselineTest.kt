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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TenancyBaselineTest {
    private fun assessment(vararg signals: TenancySignal) = ModuleAssessment(Size.M, Risk.MIDDEL, signals.associateWith { "reden" })

    private val modules = listOf(":a", ":b")

    @Test
    fun `a baseline that matches the detected signals has no failures`() {
        val failures =
            TenancyBaseline.compare(
                detected = mapOf(":a" to setOf(TenancySignal.OWN_WEBCLIENT)),
                baseline = mapOf(":a" to assessment(TenancySignal.OWN_WEBCLIENT), ":b" to assessment()),
                modules = modules,
            )

        assertThat(failures).isEmpty()
    }

    @Test
    fun `a new signal that is not in the baseline fails`() {
        val failures =
            TenancyBaseline.compare(
                detected = mapOf(":a" to setOf(TenancySignal.GLOBAL_STATE)),
                baseline = mapOf(":a" to assessment(), ":b" to assessment()),
                modules = modules,
            )

        assertThat(failures)
            .singleElement()
            .asString()
            .contains(":a")
            .contains("globaal")
            .contains("nieuw")
    }

    @Test
    fun `a baseline signal that no longer occurs fails so the entry gets removed`() {
        val failures =
            TenancyBaseline.compare(
                detected = emptyMap(),
                baseline = mapOf(":a" to assessment(TenancySignal.CONFIG_SINGLETON), ":b" to assessment()),
                modules = modules,
            )

        assertThat(failures)
            .singleElement()
            .asString()
            .contains(":a")
            .contains("config")
            .contains("verdwenen")
    }

    @Test
    fun `a module without a baseline entry fails and so does an entry for an unknown module`() {
        val failures =
            TenancyBaseline.compare(
                detected = emptyMap(),
                baseline = mapOf(":a" to assessment(), ":oud" to assessment()),
                modules = modules,
            )

        assertThat(failures).hasSize(2)
        assertThat(failures.joinToString("\n"))
            .contains(":b")
            .contains("ontbreekt")
            .contains(":oud")
            .contains("bestaat niet")
    }
}