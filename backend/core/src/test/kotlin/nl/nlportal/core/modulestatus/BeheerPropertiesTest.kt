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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BeheerPropertiesTest {
    private fun props(
        naam: String?,
        hash: String?,
    ) = BeheerProperties().apply {
        gebruikersnaam = naam
        wachtwoordHash = hash
    }

    @Test
    fun `geconfigureerd alleen met naam en hash`() {
        assertTrue(props("beheer", "{bcrypt}x").isGeconfigureerd())
        assertFalse(props(null, "{bcrypt}x").isGeconfigureerd())
        assertFalse(props(" ", "{bcrypt}x").isGeconfigureerd())
        assertFalse(props("beheer", null).isGeconfigureerd())
        assertFalse(props("beheer", " ").isGeconfigureerd())
        assertFalse(props(null, null).isGeconfigureerd())
    }

    @Test
    fun `noop-hash geeft een waarschuwing`() {
        assertEquals(1, props("beheer", "{noop}x").waarschuwingen(heeftBeheerpoort = true).size)
        assertEquals(emptyList<String>(), props("beheer", "{bcrypt}x").waarschuwingen(heeftBeheerpoort = true))
    }

    @Test
    fun `publieke poort geeft alleen een waarschuwing als beheer is geconfigureerd`() {
        assertEquals(1, props("beheer", "{bcrypt}x").waarschuwingen(heeftBeheerpoort = false).size)
        assertEquals(emptyList<String>(), props(null, null).waarschuwingen(heeftBeheerpoort = false))
    }
}