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

import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClasses

/** A dependency of one Gradle module on another, named after the first module of each mapping. */
data class ModuleEdge(
    val from: String,
    val to: String,
)

/** Derives the module -> module dependencies from bytecode, using the same mapping as the rules. */
object ModuleDependencies {
    fun of(
        classes: JavaClasses,
        mappings: List<ModuleMapping>,
    ): Set<ModuleEdge> {
        fun moduleOf(javaClass: JavaClass): String? =
            mappings
                .filter { it.owns(javaClass.packageName) }
                .maxByOrNull { it.packagePrefix.length }
                ?.modules
                ?.first()

        return classes
            .flatMap { source ->
                val from = moduleOf(source)
                source.directDependenciesFromSelf.mapNotNull { dependency ->
                    val to = moduleOf(dependency.targetClass)
                    if (from != null && to != null && from != to) ModuleEdge(from, to) else null
                }
            }.toCollection(linkedSetOf())
    }
}