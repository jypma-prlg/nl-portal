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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.io.path.readText

/**
 * The PR summary reads ModuleLayers from the base branch as source text. This test keeps the parser
 * in step with the file: if ModuleLayers.kt is reformatted so the parser no longer sees exactly
 * [ModuleLayers.production], adjust [ModuleLayersSource] rather than the mapping.
 */
class ModuleLayersSourceTest {
    @Test
    fun `parsing ModuleLayers kt yields exactly the compiled production mapping`() {
        val source = Path(ModuleLayersSource.PATH).readText()

        assertThat(ModuleLayersSource.parse(source)).isEqualTo(ModuleLayers.production)
    }

    @Test
    fun `an older ModuleLayers without exclusions or descriptions is still readable`() {
        val source =
            """
            object ModuleLayers {
                val production: List<ModuleMapping> =
                    listOf(
                        ModuleMapping(":core", "nl.nlportal.core", Layer.CORE),
                        ModuleMapping(listOf(":app", ":app-dev"), "nl.nlportal.app", Layer.APP),
                    )
            }
            """.trimIndent()

        assertThat(ModuleLayersSource.parse(source)).containsExactly(
            ModuleMapping(":core", "nl.nlportal.core", Layer.CORE),
            ModuleMapping(listOf(":app", ":app-dev"), "nl.nlportal.app", Layer.APP),
        )
    }
}