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
package nl.nlportal.core.modulestatus

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ModuleStatusRegistryTest {
    private var nu = Instant.parse("2026-11-11T10:00:00Z")
    private val clock =
        object : Clock() {
            override fun getZone() = ZoneOffset.UTC

            override fun withZone(zone: java.time.ZoneId?) = this

            override fun instant() = nu
        }
    private val registry = ModuleStatusRegistry(clock)

    @Test
    fun `lege registry geeft lege lijst`() {
        assertEquals(emptyList<ModuleStatus>(), registry.snapshot())
    }

    @Test
    fun `module met alleen bereikbare API's is UP`() {
        registry.recordSuccess("zaken", "zakenapi")

        val status = registry.snapshot().single()

        assertEquals(ModuleState.UP, status.status)
        assertNull(status.reden)
    }

    @Test
    fun `onbereikbare API maakt de module DEGRADED met reden en laatste fout`() {
        registry.recordSuccess("zaken", "zakenapi")
        registry.recordFailure("zaken", "documentenapi", Oorzaak.UNREACHABLE)

        val status = registry.snapshot().single()

        assertEquals(ModuleState.DEGRADED, status.status)
        assertEquals(Oorzaak.UNREACHABLE, status.reden)
        assertEquals(LaatsteFout(Oorzaak.UNREACHABLE, nu), status.apis.single { it.api == "documentenapi" }.laatsteFout)
    }

    @Test
    fun `niet geconfigureerd weegt zwaarder dan onbereikbaar`() {
        registry.recordFailure("zaken", "a", Oorzaak.UNREACHABLE)
        registry.recordFailure("zaken", "b", Oorzaak.NOT_CONFIGURED)

        val status = registry.snapshot().single()

        assertEquals(ModuleState.NOT_CONFIGURED, status.status)
        assertEquals(Oorzaak.NOT_CONFIGURED, status.reden)
    }

    @Test
    fun `herstel zet de API weer op bereikbaar maar bewaart de laatste fout`() {
        registry.recordFailure("zaken", "zakenapi", Oorzaak.UNREACHABLE)
        val storing = nu
        nu = nu.plusSeconds(60)
        registry.recordSuccess("zaken", "zakenapi")

        val api =
            registry
                .snapshot()
                .single()
                .apis
                .single()

        assertEquals(true, api.bereikbaar)
        assertEquals(nu, api.laatstGecontroleerd)
        assertEquals(LaatsteFout(Oorzaak.UNREACHABLE, storing), api.laatsteFout)
        assertEquals(ModuleState.UP, registry.snapshot().single().status)
    }

    @Test
    fun `aangemelde module zonder verkeer is UNKNOWN`() {
        registry.register("taak")

        val status = registry.snapshot().single()

        assertEquals(ModuleState.UNKNOWN, status.status)
        assertNull(status.reden)
        assertEquals(emptyList<ApiStatus>(), status.apis)
    }

    @Test
    fun `modules en API's staan gesorteerd`() {
        listOf("h", "g", "f", "e", "d", "c").forEach { registry.recordSuccess("zaken", it) }
        registry.recordSuccess("zaken", "b")
        registry.recordSuccess("zaken", "a")
        registry.recordSuccess("berichten", "x")
        registry.register("taak")

        val snapshot = registry.snapshot()

        assertEquals(listOf("berichten", "taak", "zaken"), snapshot.map { it.module })
        assertEquals(listOf("a", "b", "c", "d", "e", "f", "g", "h"), snapshot.last().apis.map { it.api })
    }

    @Test
    fun `een herstelde API telt niet meer mee voor de reden`() {
        registry.recordFailure("zaken", "a", Oorzaak.NOT_CONFIGURED)
        registry.recordFailure("zaken", "b", Oorzaak.UNREACHABLE)
        registry.recordSuccess("zaken", "a")

        val status = registry.snapshot().single()

        assertEquals(ModuleState.DEGRADED, status.status)
        assertEquals(Oorzaak.UNREACHABLE, status.reden)
    }

    @Test
    fun `een succes na een fout wist de reden en zet de module op UP`() {
        registry.recordFailure("zaken", "a", Oorzaak.UNREACHABLE)
        registry.recordSuccess("zaken", "a")

        val status = registry.snapshot().single()

        assertEquals(ModuleState.UP, status.status)
        assertNull(status.reden)
    }
}