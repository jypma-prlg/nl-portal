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
package nl.nlportal.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

/** Proves every rule actually fails on a deliberate violation (fixtures live in test code only). */
class ArchitectureRulesProofTest {
    @TempDir
    lateinit var tempDir: Path

    private val base = "nl.nlportal.architecture.fixtures"

    private fun classesOf(pkg: String) = ClassFileImporter().importPackages("$base.$pkg")

    private fun rules(vararg mappings: ModuleMapping) = ArchitectureRules(mappings.toList())

    @Test
    fun `core rule fails when core depends on a feature`() {
        val rules =
            rules(
                ModuleMapping(":core", "$base.corevio.core", Layer.CORE),
                ModuleMapping(":feature", "$base.corevio.feature", Layer.FEATURE),
            )
        assertThatThrownBy { rules.coreDoesNotDependOnFeature().check(classesOf("corevio")) }
            .isInstanceOf(AssertionError::class.java)
            .hasMessageContaining("Core")
            .hasMessageContaining("Feature")
    }

    @Test
    fun `infra rule fails when shared infra depends on a feature`() {
        val rules =
            rules(
                ModuleMapping(":infra", "$base.infravio.infra", Layer.SHARED_INFRA),
                ModuleMapping(":feature", "$base.infravio.feature", Layer.FEATURE),
            )
        assertThatThrownBy { rules.sharedInfraDoesNotDependOnFeature().check(classesOf("infravio")) }
            .isInstanceOf(AssertionError::class.java)
            .hasMessageContaining("Infra")
            .hasMessageContaining("Feature")
    }

    @Test
    fun `app rule fails when a feature depends on app`() {
        val rules =
            rules(
                ModuleMapping(":feature", "$base.appvio.feature", Layer.FEATURE),
                ModuleMapping(listOf(":app", ":app-dev"), "$base.appvio.app", Layer.APP),
            )
        assertThatThrownBy { rules.nothingDependsOnApp().check(classesOf("appvio")) }
            .isInstanceOf(AssertionError::class.java)
            .hasMessageContaining("Feature")
            .hasMessageContaining("App")
    }

    @Test
    fun `cycle rule fails on a cycle between zgw modules`() {
        val rules =
            rules(
                ModuleMapping(":zgw:alpha", "$base.cyclevio.alpha", Layer.FEATURE),
                ModuleMapping(":zgw:beta", "$base.cyclevio.beta", Layer.FEATURE),
            )
        assertThatThrownBy { rules.cycleFree().check(classesOf("cyclevio")) }
            .isInstanceOf(AssertionError::class.java)
            .hasMessageContaining("Cycle detected")
    }

    @Test
    fun `cycle rule catches a cycle between form and zgw taak`() {
        val rules =
            rules(
                ModuleMapping(":form", "$base.formcycle.form", Layer.FEATURE),
                ModuleMapping(":zgw:taak", "$base.formcycle.taak", Layer.FEATURE),
            )
        assertThatThrownBy { rules.cycleFree().check(classesOf("formcycle")) }
            .isInstanceOf(AssertionError::class.java)
            .hasMessageContaining("Cycle detected")
    }

    @Test
    fun `cycle rule reports no phantom cycle for the real objectenapi - form - taak chain`() {
        // Mirrors the Gradle graph: form -> zgw:objectenapi and zgw:taak -> form, with
        // objectenapi and taak below one shared parent package. That is a chain, not a cycle.
        val rules =
            rules(
                ModuleMapping(":zgw:objectenapi", "$base.zgwlike.objectenapi", Layer.FEATURE),
                ModuleMapping(":zgw:taak", "$base.zgwlike.taak", Layer.FEATURE),
                ModuleMapping(":form", "$base.zgwlike.form", Layer.FEATURE),
            )
        rules.cycleFree().check(classesOf("zgwlike"))
    }

    private val coreMapping = ModuleMapping(":core", "nl.nlportal.core", Layer.CORE, "de basis van het portaal")

    private fun module(
        path: String,
        vararg sourcePackages: String,
    ): BuildModule {
        val dir = Files.createTempDirectory(tempDir, "module")
        sourcePackages.forEach { pkg ->
            val packageDir = dir.resolve("src/main/kotlin/${pkg.replace('.', '/')}").also { Files.createDirectories(it) }
            Files.writeString(packageDir.resolve("X.kt"), "package $pkg")
        }
        return BuildModule(path, dir)
    }

    private fun coverageFailure(
        modules: List<BuildModule>,
        mappings: List<ModuleMapping>,
        excluded: Map<String, String> = emptyMap(),
    ) = assertThatThrownBy { ModuleCoverage.assertComplete(modules, mappings, excluded) }.isInstanceOf(AssertionError::class.java)

    @Test
    fun `module coverage passes for a complete mapping`() {
        ModuleCoverage.assertComplete(
            listOf(module(":core", "nl.nlportal.core"), module(":agg")),
            listOf(coreMapping),
            mapOf(":agg" to "aggregator zonder code"),
        )
    }

    @Test
    fun `module coverage fails on a gradle module without a mapping and points to the intended fix`() {
        coverageFailure(listOf(module(":core", "nl.nlportal.core"), module(":brand-new", "nl.nlportal.brandnew")), listOf(coreMapping))
            .hasMessageContaining(":brand-new")
            .hasMessageContaining("Kies bewust een laag")
            .hasMessageContaining("niet toe aan ModuleLayers.excluded")
    }

    @Test
    fun `module coverage fails on a mapped module that left the build`() {
        coverageFailure(
            listOf(module(":core", "nl.nlportal.core")),
            listOf(coreMapping, ModuleMapping(":gone", "x.gone", Layer.FEATURE, "weg")),
        ).hasMessageContaining(":gone").hasMessageContaining("bestaat niet meer")
    }

    @Test
    fun `module coverage fails on a stale exclusion`() {
        coverageFailure(listOf(module(":core", "nl.nlportal.core")), listOf(coreMapping), mapOf(":gone" to "reden"))
            .hasMessageContaining(":gone")
            .hasMessageContaining("bestaat niet meer")
    }

    @Test
    fun `module coverage fails when an excluded module ships production classes`() {
        coverageFailure(
            listOf(module(":core", "nl.nlportal.core"), module(":sneaky", "nl.nlportal.sneaky")),
            listOf(coreMapping),
            mapOf(":sneaky" to "reden"),
        ).hasMessageContaining(":sneaky").hasMessageContaining("productieklassen")
    }

    @Test
    fun `module coverage fails on a module that is both mapped and excluded`() {
        coverageFailure(listOf(module(":core", "nl.nlportal.core")), listOf(coreMapping), mapOf(":core" to "reden"))
            .hasMessageContaining("zowel")
    }

    @Test
    fun `module coverage fails when an exclusion has no reason`() {
        coverageFailure(listOf(module(":core", "nl.nlportal.core"), module(":agg")), listOf(coreMapping), mapOf(":agg" to " "))
            .hasMessageContaining("geen reden")
    }

    @Test
    fun `module coverage fails when a mapping has no description`() {
        coverageFailure(listOf(module(":core", "nl.nlportal.core")), listOf(ModuleMapping(":core", "nl.nlportal.core", Layer.CORE)))
            .hasMessageContaining("geen omschrijving")
    }

    @Test
    fun `module coverage fails when a mapped module contributes no classes to its package`() {
        coverageFailure(listOf(module(":core", "nl.nlportal.other")), listOf(coreMapping))
            .hasMessageContaining(":core")
            .hasMessageContaining("geen productieklassen")
    }

    @Test
    fun `a layer rule fails instead of passing vacuously when its layer has no classes`() {
        val rules = rules(ModuleMapping(":feature", "$base.corevio.feature", Layer.FEATURE))
        assertThatThrownBy { rules.coreDoesNotDependOnFeature().check(classesOf("corevio")) }
            .isInstanceOf(AssertionError::class.java)
    }

    @Test
    fun `a layer rule failure names the intended fix`() {
        val rules =
            rules(
                ModuleMapping(":core", "$base.corevio.core", Layer.CORE),
                ModuleMapping(":feature", "$base.corevio.feature", Layer.FEATURE),
            )
        assertThatThrownBy { rules.coreDoesNotDependOnFeature().check(classesOf("corevio")) }
            .hasMessageContaining("pas niet de mapping")
    }

    @Test
    fun `a package outside every mapping is reported`() {
        val rules = rules(ModuleMapping(":core", "$base.corevio.core", Layer.CORE))
        assertThat(rules.unmappedPackages(classesOf("corevio")))
            .containsExactly("$base.corevio.feature")
    }
}