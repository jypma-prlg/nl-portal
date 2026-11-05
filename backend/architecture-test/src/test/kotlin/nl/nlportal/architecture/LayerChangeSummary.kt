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

/**
 * Plain-language summary of architecture decisions in a PR: new modules and layer changes in
 * [ModuleLayers] compared with the base branch. Reviewers are not Java specialists, so the summary
 * asks the question in words instead of relying on them to read a Kotlin diff.
 */
object LayerChangeSummary {
    fun render(
        base: List<ModuleMapping>,
        head: List<ModuleMapping>,
    ): String {
        val baseLayers = byModule(base)
        val questions =
            byModule(head)
                .mapNotNull { (module, mapping) ->
                    val layer = mapping.layer
                    when (val before = baseLayers[module]?.layer) {
                        null -> {
                            "- Nieuwe module `$module`: *${mapping.description}*. Ingedeeld als *${layer.label}*: " +
                                "${layer.explanation}. Klopt dit?"
                        }

                        layer -> {
                            null
                        }

                        else -> {
                            "- `$module` verschuift van *${before.label}* naar *${layer.label}*: ${layer.explanation}. Klopt dit?"
                        }
                    }
                }
        if (questions.isEmpty()) return ""
        return "### Architectuurkeuze: module-indeling gewijzigd\n\n" +
            "Deze PR voegt een backend-module toe of verplaatst er een naar een andere laag. Dat bepaalt welke " +
            "modules van elkaar mogen afhangen. Controleer per punt of de indeling klopt met wat de module doet.\n\n" +
            questions.joinToString("\n") + "\n"
    }

    private fun byModule(mappings: List<ModuleMapping>): Map<String, ModuleMapping> = mappings.flatMap { mapping -> mapping.modules.map { it to mapping } }.toMap()
}