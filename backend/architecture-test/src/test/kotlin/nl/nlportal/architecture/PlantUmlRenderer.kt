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
    ): String {
        val name = mapping.modules.joinToString(" / ")
        val found = mapping.modules.flatMap { signals[it].orEmpty() }.toSortedSet()
        return if (found.isEmpty()) name else "$name\\n<size:10>tenancy: ${found.joinToString(", ") { it.short }}</size>"
    }

    fun render(
        mappings: List<ModuleMapping>,
        edges: Map<ModuleEdge, Coverage>,
        signals: Map<String, Set<TenancySignal>> = emptyMap(),
    ): String {
        val out = StringBuilder()
        out.appendLine("@startuml")
        out.appendLine("title Modules, lagen en regeldekking")
        out.appendLine("left to right direction")
        Layer.entries.forEach { layer ->
            val inLayer = mappings.filter { it.layer == layer }.sortedBy { it.modules.first() }
            if (inLayer.isEmpty()) return@forEach
            out.appendLine("package \"${layer.label}\" {")
            inLayer.forEach { out.appendLine("  [${label(it, signals)}] as ${alias(it.modules.first())}") }
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
        out.appendLine("endlegend")
        out.appendLine("@enduml")
        return out.toString()
    }
}