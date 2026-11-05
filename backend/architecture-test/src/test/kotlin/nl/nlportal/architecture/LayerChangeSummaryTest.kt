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

class LayerChangeSummaryTest {
    private val core = ModuleMapping(":core", "nl.nlportal.core", Layer.CORE, "de basis van het portaal")
    private val openklant = ModuleMapping(":zgw:openklant", "nl.nlportal.openklant", Layer.FEATURE, "contactgegevens via Open Klant")
    private val form = ModuleMapping(":form", "nl.nlportal.form", Layer.FEATURE, "formulieren afhandelen")

    @Test
    fun `a new module asks the reviewer whether its layer is right`() {
        val summary = LayerChangeSummary.render(base = listOf(core), head = listOf(core, form))

        assertThat(summary).contains(
            "Nieuwe module `:form`: *formulieren afhandelen*. Ingedeeld als *functionaliteit*: " +
                "losse functionele modules; de kern en gedeelde infrastructuur mogen er niet van afhangen. Klopt dit?",
        )
        assertThat(summary).doesNotContain("`:core`")
    }

    @Test
    fun `a layer change names the old and new layer and what that means`() {
        val summary = LayerChangeSummary.render(base = listOf(core, openklant), head = listOf(core, openklant.copy(layer = Layer.CORE)))

        assertThat(summary).contains(
            "- `:zgw:openklant` verschuift van *functionaliteit* naar *kern*: de kern mag niet afhangen van " +
                "gedeelde infrastructuur of functionaliteit; alles mag van de kern afhangen. Klopt dit?",
        )
    }

    @Test
    fun `a summary with questions explains why the reviewer is asked`() {
        val summary = LayerChangeSummary.render(base = listOf(core), head = listOf(core, form))

        assertThat(summary).startsWith("### Architectuurkeuze: module-indeling gewijzigd\n")
    }

    @Test
    fun `no new module and no layer change gives no summary`() {
        val changedDescriptionAndPackage = openklant.copy(packagePrefix = "nl.nlportal.klant", description = "andere omschrijving")

        assertThat(LayerChangeSummary.render(base = listOf(core, openklant), head = listOf(core, changedDescriptionAndPackage))).isEmpty()
    }

    @Test
    fun `a removed module gives no summary`() {
        assertThat(LayerChangeSummary.render(base = listOf(core, openklant), head = listOf(core))).isEmpty()
    }

    @Test
    fun `when main has no module mapping yet every module counts as new`() {
        val summary = LayerChangeSummary.render(base = emptyList(), head = listOf(core, form))

        assertThat(summary).contains("Nieuwe module `:core`").contains("Nieuwe module `:form`")
    }

    @Test
    fun `modules sharing one mapping are each reported`() {
        val app = ModuleMapping(listOf(":app", ":app-dev"), "nl.nlportal.app", Layer.APP, "de applicatie")

        assertThat(LayerChangeSummary.render(base = listOf(core), head = listOf(core, app)))
            .contains("Nieuwe module `:app`")
            .contains("Nieuwe module `:app-dev`")
    }
}