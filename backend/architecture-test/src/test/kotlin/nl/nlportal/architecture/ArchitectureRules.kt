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

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.dependencies.SliceAssignment
import com.tngtech.archunit.library.dependencies.SliceIdentifier
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices

/** ArchUnit rules derived from a module mapping, so they can be proven against fixture mappings. */
class ArchitectureRules(
    private val mappings: List<ModuleMapping>,
) {
    private fun mappingOf(javaClass: JavaClass): ModuleMapping? =
        mappings
            .filter { it.owns(javaClass.packageName) }
            .maxByOrNull { it.packagePrefix.length }

    private fun inLayer(layer: Layer) = DescribedPredicate.describe<JavaClass>("reside in layer $layer") { c -> mappingOf(c)?.layer == layer }

    private fun notInLayer(layer: Layer) = DescribedPredicate.describe<JavaClass>("reside outside layer $layer") { c -> mappingOf(c)?.layer != layer }

    fun coreDoesNotDependOnFeature(): ArchRule = layerDoesNotDependOn(Layer.CORE, Layer.FEATURE)

    fun sharedInfraDoesNotDependOnFeature(): ArchRule = layerDoesNotDependOn(Layer.SHARED_INFRA, Layer.FEATURE)

    private fun layerDoesNotDependOn(
        from: Layer,
        to: Layer,
    ): ArchRule =
        noClasses()
            .that(inLayer(from))
            .should()
            .dependOnClassesThat(inLayer(to))

    fun nothingDependsOnApp(): ArchRule =
        noClasses()
            .that(notInLayer(Layer.APP))
            .should()
            .dependOnClassesThat(inLayer(Layer.APP))

    /** Slices per Gradle module (not per package), so modules sharing a package stay distinct. */
    fun cycleFree(): ArchRule {
        val assignment =
            object : SliceAssignment {
                override fun getIdentifierOf(javaClass: JavaClass): SliceIdentifier {
                    val mapping = mappingOf(javaClass)
                    return if (mapping != null && ModuleLayers.isCycleChecked(mapping)) {
                        SliceIdentifier.of(mapping.modules.first())
                    } else {
                        SliceIdentifier.ignore()
                    }
                }

                override fun getDescription() = "gradle modules"
            }
        return slices().assignedFrom(assignment).should().beFreeOfCycles()
    }

    /** Production classes under nl.nlportal that no mapping claims; they would escape every rule. */
    fun unmappedPackages(classes: JavaClasses): Set<String> =
        classes
            .filter { mappingOf(it) == null }
            .map { it.packageName }
            .toSortedSet()

    companion object {
        /** Gradle modules missing from the mapping. */
        fun unmappedModules(
            buildModules: Collection<String>,
            mappings: List<ModuleMapping>,
        ): Set<String> = buildModules.toSet() - mappings.flatMap { it.modules }.toSet()

        /** Mapped modules that no longer exist in the build. */
        fun staleModules(
            buildModules: Collection<String>,
            mappings: List<ModuleMapping>,
        ): Set<String> = mappings.flatMap { it.modules }.toSet() - buildModules.toSet()
    }
}