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

class PlantUmlRendererTest {
    private val mappings =
        listOf(
            ModuleMapping(":core", "p.core", Layer.CORE),
            ModuleMapping(":zgw:zaken", "p.zaken", Layer.FEATURE),
            ModuleMapping(":payment-direct", "p.payment", Layer.FEATURE),
            ModuleMapping(listOf(":app", ":app-dev"), "p.app", Layer.APP),
        )

    @Test
    fun `renders layers as packages and every edge in the style of its coverage`() {
        val edges =
            mapOf(
                ModuleEdge(":zgw:zaken", ":core") to Coverage.ALLOWED,
                ModuleEdge(":core", ":payment-direct") to Coverage.VIOLATION,
                ModuleEdge(":zgw:zaken", ":payment-direct") to Coverage.GAP,
                ModuleEdge(":app", ":zgw:zaken") to Coverage.ALLOWED,
            )

        val puml = PlantUmlRenderer.render(mappings, edges)

        assertThat(puml).isEqualTo(
            """
            @startuml
            title Modules, lagen en regeldekking
            left to right direction
            package "kern" {
              [:core] as core
            }
            package "functionaliteit" {
              [:payment-direct] as payment_direct
              [:zgw:zaken] as zgw_zaken
            }
            package "applicatie" {
              [:app / :app-dev] as app
            }
            app --> zgw_zaken #grey
            core -[#red,dashed]-> payment_direct : verboden
            zgw_zaken --> core #grey
            zgw_zaken ..> payment_direct #orange : gat
            legend right
              grijs: toegestaan, rood gestreept: verboden, oranje gestippeld: niet bewaakt, groen: bewaakt
              tenancy: signalen die een gedeelde multi-tenant runtime in de weg staan (config, webclient, globaal)
              vulkleur: risico van de multitenancy-migratie (rood hoog, geel middel, groen laag)
            endlegend
            @enduml

            """.trimIndent(),
        )
    }

    @Test
    fun `lists the multitenancy signals of a module under its name`() {
        val puml =
            PlantUmlRenderer.render(
                mappings,
                emptyMap(),
                mapOf(":payment-direct" to setOf(TenancySignal.CONFIG_SINGLETON, TenancySignal.GLOBAL_STATE)),
            )

        val literalBackslashN = "\\" + "n"
        assertThat(puml).contains("[:payment-direct$literalBackslashN<size:10>tenancy: config, globaal</size>] as payment_direct")
        assertThat(puml).contains("[:zgw:zaken] as zgw_zaken")
    }

    @Test
    fun `shows size and risk from the baseline and colours the module by risk`() {
        val puml =
            PlantUmlRenderer.render(
                mappings,
                emptyMap(),
                assessments = mapOf(":zgw:zaken" to ModuleAssessment(Size.L, Risk.HOOG, emptyMap())),
            )

        val literalBackslashN = "\\" + "n"
        assertThat(puml).contains("[:zgw:zaken$literalBackslashN<size:10>omvang L, risico hoog</size>] as zgw_zaken #F4B6B6")
    }

    @Test
    fun `shows the tenancy seam as a node that is not built yet and lists the proposed rules`() {
        val puml =
            PlantUmlRenderer.render(
                mappings,
                emptyMap(),
                proposedRules =
                    listOf(
                        RuleStatus(ProposedRule("noOwnWebClient", "geen eigen WebClient buiten core"), active = false),
                        RuleStatus(ProposedRule("noJvmGlobalState", "geen JVM-globale toestand"), active = true),
                    ),
            )

        val literalBackslashN = "\\" + "n"
        assertThat(puml).contains(
            "[tenancy-seam$literalBackslashN<size:10>TenantContext, TenantRegistry: nog niet gebouwd</size>] as tenancy_seam #white",
        )
        assertThat(puml).contains("noOwnWebClient: geen eigen WebClient buiten core (voorgesteld, niet actief)")
        assertThat(puml).contains("noJvmGlobalState: geen JVM-globale toestand (actief)")
    }
}