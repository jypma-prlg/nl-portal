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

import org.yaml.snakeyaml.Yaml
import java.nio.file.Files
import java.nio.file.Path

enum class Size { S, M, L }

enum class Risk(
    val label: String,
) {
    LAAG("laag"),
    MIDDEL("middel"),
    HOOG("hoog"),
}

/** The hand-written judgement per module; the facts (signals) are re-derived from the code on every run. */
data class ModuleAssessment(
    val size: Size,
    val risk: Risk,
    /** Known signals, each with the reason it exists or the plan to remove it. */
    val signals: Map<TenancySignal, String>,
)

object TenancyBaseline {
    fun load(file: Path): Map<String, ModuleAssessment> {
        val root = Files.newBufferedReader(file).use { Yaml().load<Map<String, Any?>>(it) }
        val modules = root["modules"] as? Map<*, *> ?: error("No 'modules' map in $file")
        return modules.entries.associate { (name, value) ->
            val module = value as Map<*, *>
            val signals =
                (module["signalen"] as? Map<*, *> ?: emptyMap<Any, Any>()).entries.associate { (key, reason) ->
                    val signal =
                        TenancySignal.entries.firstOrNull { it.short == key.toString() }
                            ?: error("Onbekend signaal '$key' bij module $name in $file")
                    signal to reason.toString()
                }
            name.toString() to
                ModuleAssessment(
                    Size.valueOf(module["omvang"].toString()),
                    Risk.entries.first { it.label == module["risico"].toString() },
                    signals,
                )
        }
    }

    /** Failures in both directions: a signal missing from the baseline, and a baseline entry the code no longer backs. */
    fun compare(
        detected: Map<String, Set<TenancySignal>>,
        baseline: Map<String, ModuleAssessment>,
        modules: List<String>,
    ): List<String> {
        val failures = mutableListOf<String>()
        modules.filter { it !in baseline }.forEach {
            failures += "Module $it ontbreekt in de tenancy-baseline: beoordeel omvang en risico en voeg haar toe"
        }
        baseline.keys.filter { it !in modules }.forEach {
            failures += "Module $it staat in de tenancy-baseline maar bestaat niet meer: verwijder de regel"
        }
        for (module in modules.filter { it in baseline }) {
            val known = baseline.getValue(module).signals.keys
            val found = detected[module].orEmpty()
            (found - known).forEach {
                failures += "Module $module heeft een nieuw tenancy-signaal '${it.short}' (${it.label}): los het op of voeg het met reden toe aan de baseline"
            }
            (known - found).forEach {
                failures += "Tenancy-signaal '${it.short}' van module $module is verdwenen uit de code: verwijder het uit de baseline"
            }
        }
        return failures
    }
}