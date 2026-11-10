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
import com.tngtech.archunit.core.importer.ImportOption
import java.io.File
import java.nio.file.Path

/** The module diagram of the production code: bytecode dependencies classified by rule coverage. */
object ArchitectureDiagram {
    fun generate(
        baselineFile: Path,
        mappings: List<ModuleMapping> = ModuleLayers.production,
    ): String {
        val classes =
            ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("nl.nlportal")
        val edges = ModuleDependencies.of(classes, mappings).associateWith { RuleCoverage.classify(it, mappings) }
        return PlantUmlRenderer.render(mappings, edges, TenancySignals.of(classes, mappings), TenancyBaseline.load(baselineFile))
    }
}

/** Entry point of the Gradle task `architectureDiagram`; arguments: the file to write and the tenancy baseline. */
fun main(args: Array<String>) {
    val (target, baseline) = args
    val output = File(target)
    output.parentFile.mkdirs()
    output.writeText(ArchitectureDiagram.generate(Path.of(baseline)))
    println("Diagram geschreven naar $output")
}