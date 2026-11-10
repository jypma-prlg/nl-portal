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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ModuleDependenciesTest {
    private val base = "nl.nlportal.architecture.fixtures"

    private fun classesOf(pkg: String) = ClassFileImporter().importPackages("$base.$pkg")

    @Test
    fun `a class that uses a class of another module yields one edge between those modules`() {
        val mappings =
            listOf(
                ModuleMapping(":core", "$base.corevio.core", Layer.CORE),
                ModuleMapping(":feature", "$base.corevio.feature", Layer.FEATURE),
            )

        val edges = ModuleDependencies.of(classesOf("corevio"), mappings)

        assertThat(edges).containsExactly(ModuleEdge(from = ":core", to = ":feature"))
    }

    @Test
    fun `modules that share one mapping are reported under the first module of that mapping`() {
        val mappings =
            listOf(
                ModuleMapping(":core", "$base.appvio.feature", Layer.CORE),
                ModuleMapping(listOf(":app", ":app-dev"), "$base.appvio.app", Layer.APP),
            )

        val edges = ModuleDependencies.of(classesOf("appvio"), mappings)

        assertThat(edges).containsExactly(ModuleEdge(from = ":core", to = ":app"))
    }

    @Test
    fun `dependencies inside one module and on unmapped packages are not edges`() {
        val mappings = listOf(ModuleMapping(":core", "$base.corevio.core", Layer.CORE))

        assertThat(ModuleDependencies.of(classesOf("corevio"), mappings)).isEmpty()
    }
}