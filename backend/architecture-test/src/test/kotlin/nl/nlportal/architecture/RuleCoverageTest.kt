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

class RuleCoverageTest {
    private val mappings =
        listOf(
            ModuleMapping(":core", "p.core", Layer.CORE),
            ModuleMapping(":graphql", "p.graphql", Layer.SHARED_INFRA),
            ModuleMapping(":portal-authentication", "p.auth", Layer.SHARED_INFRA),
            ModuleMapping(":zgw:zaken", "p.zaken", Layer.FEATURE),
            ModuleMapping(":zgw:taak", "p.taak", Layer.FEATURE),
            ModuleMapping(":form", "p.form", Layer.FEATURE),
            ModuleMapping(":payment-direct", "p.payment", Layer.FEATURE),
            ModuleMapping(":haalcentraal-brp", "p.hc2", Layer.FEATURE),
            ModuleMapping(listOf(":app", ":app-dev"), "p.app", Layer.APP),
        )

    private fun classify(
        from: String,
        to: String,
    ) = RuleCoverage.classify(ModuleEdge(from, to), mappings)

    @Test
    fun `an edge that a rule forbids is a violation`() {
        assertThat(classify(":core", ":zgw:zaken")).isEqualTo(Coverage.VIOLATION)
        assertThat(classify(":graphql", ":form")).isEqualTo(Coverage.VIOLATION)
        assertThat(classify(":zgw:zaken", ":app")).isEqualTo(Coverage.VIOLATION)
    }

    @Test
    fun `an edge in the designed direction is allowed`() {
        assertThat(classify(":zgw:zaken", ":core")).isEqualTo(Coverage.ALLOWED)
        assertThat(classify(":graphql", ":core")).isEqualTo(Coverage.ALLOWED)
        assertThat(classify(":app", ":zgw:zaken")).isEqualTo(Coverage.ALLOWED)
    }

    @Test
    fun `an edge between modules that are checked for cycles is guarded`() {
        assertThat(classify(":zgw:zaken", ":zgw:taak")).isEqualTo(Coverage.GUARDED)
        assertThat(classify(":form", ":zgw:taak")).isEqualTo(Coverage.GUARDED)
    }

    @Test
    fun `an edge that no rule covers is a gap`() {
        // core depending on shared infra is described in the layer explanation but has no rule
        assertThat(classify(":core", ":graphql")).isEqualTo(Coverage.GAP)
        // shared infra modules may form a cycle: only zgw modules and form are checked
        assertThat(classify(":graphql", ":portal-authentication")).isEqualTo(Coverage.GAP)
        // features outside the cycle-checked set can depend on each other in any direction
        assertThat(classify(":payment-direct", ":haalcentraal-brp")).isEqualTo(Coverage.GAP)
        assertThat(classify(":zgw:zaken", ":payment-direct")).isEqualTo(Coverage.GAP)
    }
}