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

/**
 * Strictness of the capability fitness test. Entry points in the YAML that no longer exist in code
 * always fail the build; this switch only controls what happens to entry points in code that
 * are missing from the YAML. Flip to true (hard mode) after maintainer sign-off.
 */
const val HARD_MODE = false

enum class EntryPointType(
    val yamlName: String,
) {
    GRAPHQL_QUERY("graphql-query"),
    GRAPHQL_MUTATION("graphql-mutation"),
    REST("rest"),
    EVENT_LISTENER("event-listener"),
    ;

    companion object {
        fun fromYaml(name: String): EntryPointType =
            entries.firstOrNull { it.yamlName == name }
                ?: error("Unknown entry point type '$name' in capabilities.yaml")
    }
}

/** Identity of a backend entry point: type + name, exactly as used in docs/capabilities.yaml. */
data class EntryPoint(
    val type: EntryPointType,
    val naam: String,
) {
    override fun toString() = "${type.yamlName} $naam"
}

data class InventoryComparison(
    /** In code, not in the YAML. */
    val unmapped: Set<EntryPoint>,
    /** In the YAML, not in code. */
    val stale: Set<EntryPoint>,
) {
    fun failures(hardMode: Boolean): List<String> =
        buildList {
            if (stale.isNotEmpty()) {
                add("docs/capabilities.yaml lists entry points that no longer exist in code; remove or rename them:\n${bullets(stale)}")
            }
            if (hardMode && unmapped.isNotEmpty()) add(unmappedMessage())
        }

    fun warnings(hardMode: Boolean): List<String> = if (!hardMode && unmapped.isNotEmpty()) listOf(unmappedMessage()) else emptyList()

    private fun unmappedMessage() = "Entry points found in code but missing from docs/capabilities.yaml; add them to a capability:\n${bullets(unmapped)}"

    private fun bullets(entries: Set<EntryPoint>) = entries.map { it.toString() }.sorted().joinToString("\n") { "  - $it" }

    companion object {
        fun of(
            code: Set<EntryPoint>,
            inventory: Set<EntryPoint>,
        ) = InventoryComparison(unmapped = code - inventory, stale = inventory - code)
    }
}