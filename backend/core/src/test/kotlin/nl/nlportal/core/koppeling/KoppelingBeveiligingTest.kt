/*
 * Copyright 2015-2026 Den Haag, Ritense, the Netherlands.
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
package nl.nlportal.core.koppeling

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KoppelingBeveiligingTest {
    @Test
    fun `de tekstweergave van de authenticatie toont geen secrets maar wel het type en de client-id`() {
        val auth =
            KoppelingAuth().apply {
                type = KoppelingAuthType.ZGW_JWT
                clientId = "nl-portal"
                secret = "super-geheim"
                token = "geheim-token"
                value = "geheime-waarde"
            }

        val tekst = auth.toString()

        assertTrue(tekst.contains("ZGW_JWT"))
        assertTrue(tekst.contains("nl-portal"))
        listOf("super-geheim", "geheim-token", "geheime-waarde").forEach { assertFalse(tekst.contains(it), it) }
    }

    @Test
    fun `een ontbrekend secret staat als null in de tekstweergave en een aanwezig secret als sterretjes`() {
        assertTrue(KoppelingAuth().toString().contains("secret=null"))
        assertTrue(KoppelingAuth().apply { secret = "x" }.toString().contains("secret=***"))
    }

    @Test
    fun `de tekstweergave van een koppeling toont url, leverancier en versie`() {
        val tekst =
            Koppeling()
                .apply {
                    url = "https://zaken.example.nl"
                    leverancier = "Open Zaak"
                    versie = "1.31"
                }.toString()

        listOf("https://zaken.example.nl", "Open Zaak", "1.31").forEach { assertTrue(tekst.contains(it), it) }
    }

    @Test
    fun `zonder url of met een onleesbare url is een koppeling niet onbeveiligd`() {
        assertFalse(Koppeling().isOnbeveiligd())
        assertFalse(Koppeling().apply { url = "http://exa mple" }.isOnbeveiligd())
        assertFalse(Koppeling().apply { url = "niet-een-url" }.isOnbeveiligd())
    }

    @Test
    fun `een http-url naar een publiek adres is onbeveiligd`() {
        assertTrue(Koppeling().apply { url = "http://api.example.nl" }.isOnbeveiligd())
    }

    @Test
    fun `https, localhost en servicenamen binnen een containernetwerk zijn niet onbeveiligd`() {
        listOf("https://api.example.nl", "http://localhost:8001", "http://127.0.0.1:8001", "http://openzaak:8000").forEach {
            assertFalse(Koppeling().apply { url = it }.isOnbeveiligd(), it)
        }
    }
}