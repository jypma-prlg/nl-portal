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
import com.tngtech.archunit.core.importer.ImportOption
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Path

/**
 * Enforces the layering described in site/architectuur/arc42: core -> shared infra (graphql,
 * portal-authentication) -> feature modules -> app/app-dev. The module -> package -> layer relation
 * lives in [ModuleLayers]; a violation here fails the build instead of surfacing at code review.
 */
class LayerArchitectureTest {
    private val classes =
        ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("nl.nlportal")

    private val rules = ArchitectureRules(ModuleLayers.production)

    private val buildModules =
        System
            .getProperty("architecture.modules")
            .split("|")
            .filter { it.isNotBlank() }
            .map { entry -> BuildModule(entry.substringBefore("="), Path.of(entry.substringAfter("="))) }

    @Test
    fun `every gradle module is assigned to a layer or deliberately excluded`() {
        ModuleCoverage.assertComplete(buildModules, ModuleLayers.production, ModuleLayers.excluded)
    }

    @Test
    fun `every production package belongs to a mapped module`() {
        assertThat(rules.unmappedPackages(classes))
            .describedAs(
                "Packages not covered by ModuleLayers: map the owning module to a deliberately chosen layer " +
                    "instead of widening an existing prefix or filtering the scan",
            ).isEmpty()
    }

    @Test
    fun `core does not depend on a feature module`() {
        rules.coreDoesNotDependOnFeature().check(classes)
    }

    @Test
    fun `shared infra modules do not depend on a feature module`() {
        rules.sharedInfraDoesNotDependOnFeature().check(classes)
    }

    @Test
    fun `nothing outside the app packages depends on app or app-dev`() {
        rules.nothingDependsOnApp().check(classes)
    }

    @Test
    fun `zgw modules and form have no cyclic dependencies`() {
        rules.cycleFree().check(classes)
    }
}