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
import java.nio.file.Path

class CapabilityInventoryFitnessTest {
    private val code = setOf(entry("getZaken"), entry("getZaak"))
    private val inventory = setOf(entry("getZaken"), entry("getZaak"))

    @Test
    fun `agreeing code and inventory produce no failures or warnings`() {
        val result = InventoryComparison.of(code, inventory)

        assertThat(result.failures(hardMode = false)).isEmpty()
        assertThat(result.warnings(hardMode = false)).isEmpty()
    }

    @Test
    fun `entry point missing from inventory is a warning in warn mode and a failure in hard mode`() {
        val result = InventoryComparison.of(code, inventory - entry("getZaak"))

        assertThat(result.failures(hardMode = false)).isEmpty()
        assertThat(result.warnings(hardMode = false)).singleElement().asString().contains("graphql-query getZaak")
        assertThat(result.failures(hardMode = true)).singleElement().asString().contains("graphql-query getZaak")
    }

    @Test
    fun `inventory entry point that no longer exists in code always fails with a clear message`() {
        val result = InventoryComparison.of(code, inventory + entry("verwijderdeOperatie"))

        assertThat(result.failures(hardMode = false)).singleElement().asString().contains("graphql-query verwijderdeOperatie")
        assertThat(result.failures(hardMode = true)).hasSize(1)
    }

    @Test
    fun `backend entry points in code agree with docs capabilities yaml`() {
        val file = Path.of(System.getProperty("capabilities.file") ?: "../../docs/capabilities.yaml")
        val result = InventoryComparison.of(EntryPointScanner.scan(), CapabilityInventory.load(file))

        result.warnings(HARD_MODE).forEach { System.err.println("WARNING: $it") }
        assertThat(result.failures(HARD_MODE)).describedAs("capability inventory drift").isEmpty()
    }

    private fun entry(naam: String) = EntryPoint(EntryPointType.GRAPHQL_QUERY, naam)
}