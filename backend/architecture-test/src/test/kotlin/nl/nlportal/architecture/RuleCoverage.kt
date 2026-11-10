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

enum class Coverage {
    /** A rule forbids this dependency; it only exists in the code if that rule fails. */
    VIOLATION,

    /** The dependency runs in the designed direction (towards a lower layer). */
    ALLOWED,

    /** Both modules are checked for cycles, so a mutual dependency is caught. */
    GUARDED,

    /** No rule covers this dependency: it can change direction or form a cycle unnoticed. */
    GAP,
}

/** Classifies a module dependency against the rules in [ArchitectureRules]; keep both in step. */
object RuleCoverage {
    fun classify(
        edge: ModuleEdge,
        mappings: List<ModuleMapping>,
    ): Coverage {
        val from = mappings.first { edge.from in it.modules }
        val to = mappings.first { edge.to in it.modules }
        return when {
            to.layer == Layer.APP && from.layer != Layer.APP -> Coverage.VIOLATION
            to.layer == Layer.FEATURE && from.layer in setOf(Layer.CORE, Layer.SHARED_INFRA) -> Coverage.VIOLATION
            from.layer == Layer.APP -> Coverage.ALLOWED
            from.layer == Layer.CORE && to.layer == Layer.SHARED_INFRA -> Coverage.GAP
            from.layer.ordinal > to.layer.ordinal -> Coverage.ALLOWED
            ModuleLayers.isCycleChecked(from) && ModuleLayers.isCycleChecked(to) -> Coverage.GUARDED
            else -> Coverage.GAP
        }
    }
}