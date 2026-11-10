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

/** Renders the module layers and the covered/uncovered dependencies as PlantUML text (deterministic). */
object PlantUmlRenderer {
    private fun alias(module: String) = module.removePrefix(":").replace(Regex("[^A-Za-z0-9]"), "_")

    private fun label(
        mapping: ModuleMapping,
        signals: Map<String, Set<TenancySignal>>,
        assessment: ModuleAssessment?,
    ): String {
        val name = mapping.modules.joinToString(" / ")
        val found = mapping.modules.flatMap { signals[it].orEmpty() }.toSortedSet()
        val parts =
            listOfNotNull(
                assessment?.let { "omvang ${it.size}, risico ${it.risk.label}" },
                found.takeIf { it.isNotEmpty() }?.let { "tenancy: ${it.joinToString(", ") { signal -> signal.short }}" },
            )
        return if (parts.isEmpty()) name else "$name\\n<size:10>${parts.joinToString(", ")}</size>"
    }

    private fun colour(assessment: ModuleAssessment?) =
        when (assessment?.risk) {
            Risk.HOOG -> " #F4B6B6"
            Risk.MIDDEL -> " #FBE3A6"
            Risk.LAAG -> " #CFE8CF"
            null -> ""
        }

    fun render(
        mappings: List<ModuleMapping>,
        edges: Map<ModuleEdge, Coverage>,
        signals: Map<String, Set<TenancySignal>> = emptyMap(),
        assessments: Map<String, ModuleAssessment> = emptyMap(),
    ): String {
        val out = StringBuilder()
        out.appendLine("@startuml")
        out.appendLine("title Modules, lagen en regeldekking")
        out.appendLine("left to right direction")
        Layer.entries.forEach { layer ->
            val inLayer = mappings.filter { it.layer == layer }.sortedBy { it.modules.first() }
            if (inLayer.isEmpty()) return@forEach
            out.appendLine("package \"${layer.label}\" {")
            inLayer.forEach {
                val assessment = assessments[it.modules.first()]
                out.appendLine("  [${label(it, signals, assessment)}] as ${alias(it.modules.first())}${colour(assessment)}")
            }
            out.appendLine("}")
        }
        edges.entries
            .sortedWith(compareBy({ alias(it.key.from) }, { alias(it.key.to) }))
            .forEach { (edge, coverage) ->
                val from = alias(edge.from)
                val to = alias(edge.to)
                out.appendLine(
                    when (coverage) {
                        Coverage.ALLOWED -> "$from --> $to #grey"
                        Coverage.GUARDED -> "$from --> $to #green : bewaakt"
                        Coverage.GAP -> "$from ..> $to #orange : gat"
                        Coverage.VIOLATION -> "$from -[#red,dashed]-> $to : verboden"
                    },
                )
            }
        out.appendLine("legend right")
        out.appendLine("  grijs: toegestaan, rood gestreept: verboden, oranje gestippeld: niet bewaakt, groen: bewaakt")
        out.appendLine("  tenancy: signalen die een gedeelde multi-tenant runtime in de weg staan (config, webclient, globaal)")
        out.appendLine("  vulkleur: risico van de multitenancy-migratie (rood hoog, geel middel, groen laag)")
        out.appendLine("endlegend")
        out.appendLine("@enduml")
        return out.toString()
    }
}