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

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory

/** A Gradle project in the build and where it lives on disk. */
data class BuildModule(
    val path: String,
    val dir: Path,
) {
    /** True when the module has Kotlin/Java sources under [packagePrefix] (a dotted package name) in src/main. */
    fun hasProductionSources(packagePrefix: String): Boolean =
        listOf("kotlin", "java").any { sourceRoot ->
            val packageDir = dir.resolve("src/main/$sourceRoot/${packagePrefix.replace('.', '/')}")
            packageDir.isDirectory() &&
                Files.walk(packageDir).use { files -> files.anyMatch { it.extension == "kt" || it.extension == "java" } }
        }
}

/**
 * Checks that the module list in [ModuleLayers] is complete and honest. Called by
 * [LayerArchitectureTest] and, with fixture input, by the proof test, so the failure itself is proven.
 * Every message names the intended fix: the test must steer towards a deliberate choice, not a green tick.
 */
object ModuleCoverage {
    private const val ROOT_PACKAGE = "nl.nlportal"

    fun violations(
        buildModules: List<BuildModule>,
        mappings: List<ModuleMapping>,
        excluded: Map<String, String>,
    ): List<String> {
        val byPath = buildModules.associateBy { it.path }
        val mapped = mappings.flatMap { it.modules }.toSet()
        val result = mutableListOf<String>()

        buildModules
            .filter { it.path !in mapped && it.path !in excluded }
            .forEach {
                result +=
                    "Gradle-module ${it.path} heeft geen laag. Kies bewust een laag in ModuleLayers.production " +
                    "(${layerGuide()}). Voeg de module niet toe aan ModuleLayers.excluded, tenzij de module " +
                    "geen productiecode onder $ROOT_PACKAGE bevat."
            }

        excluded.keys
            .filter { it in mapped }
            .forEach {
                result += "$it staat zowel in ModuleLayers.production als in ModuleLayers.excluded. Kies één: heeft de module productiecode, haal hem dan uit excluded."
            }

        excluded.entries
            .filter { (path, reason) -> path in byPath && reason.isBlank() }
            .forEach { (path, _) -> result += "Uitzondering $path heeft geen reden. Leg uit waarom de module geen productiecode bevat." }

        excluded.keys
            .filter { it !in byPath }
            .forEach { result += "ModuleLayers.excluded noemt $it, maar die module bestaat niet meer in de build. Verwijder de regel." }

        excluded.keys
            .mapNotNull { byPath[it] }
            .filter { it.hasProductionSources(ROOT_PACKAGE) }
            .forEach {
                result +=
                    "${it.path} staat in ModuleLayers.excluded maar bevat productieklassen onder $ROOT_PACKAGE; " +
                    "die ontsnappen dan aan alle architectuurregels. Geef de module een laag in ModuleLayers.production " +
                    "(${layerGuide()}) en haal hem uit excluded."
            }

        mapped
            .filter { it !in byPath }
            .forEach { result += "ModuleLayers.production noemt $it, maar die module bestaat niet meer in de build. Verwijder of hernoem de mapping." }

        mappings.forEach { mapping ->
            if (mapping.description.isBlank()) {
                result +=
                    "Mapping ${mapping.modules} in ModuleLayers.production heeft geen omschrijving. Beschrijf in één Nederlandse " +
                    "zin wat de module voor burger of gemeente doet."
            }
            mapping.modules
                .mapNotNull { byPath[it] }
                .filter { !it.hasProductionSources(mapping.packagePrefix) }
                .forEach {
                    result +=
                        "${it.path} is gemapt op ${mapping.packagePrefix} maar draagt geen productieklassen in dat package bij; " +
                        "de laagregels zouden niets van deze module toetsen. Corrigeer het package in de mapping " +
                        "(niet naar een leeg package wijzigen) of verwijder de module uit de mapping."
                }
        }
        return result
    }

    fun assertComplete(
        buildModules: List<BuildModule>,
        mappings: List<ModuleMapping>,
        excluded: Map<String, String>,
    ) {
        val violations = violations(buildModules, mappings, excluded)
        if (violations.isNotEmpty()) {
            throw AssertionError("De module-indeling in ModuleLayers klopt niet met de build:\n- " + violations.joinToString("\n- "))
        }
    }

    private fun layerGuide() = Layer.entries.joinToString("; ") { "${it.name} = ${it.label}: ${it.explanation}" }
}