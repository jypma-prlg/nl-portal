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

import nl.nlportal.core.util.ModuleNotConfiguredException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono
import java.io.IOException
import java.net.URI

class ModuleStatusWebClientTest {
    private val registry = ModuleStatusRegistry()
    private val request = ClientRequest.create(HttpMethod.GET, URI.create("http://localhost/x")).build()

    private fun status() = registry.snapshot().single()

    private fun roep(next: ExchangeFunction) = registry.webClientFilter("zakenapi", "zakenapi").filter(request, next)

    private fun antwoord(status: HttpStatus) = ExchangeFunction { Mono.just(ClientResponse.create(status).build()) }

    @Test
    fun `een 200 is bereikbaar`() {
        roep(antwoord(HttpStatus.OK)).block()

        assertEquals(ModuleState.UP, status().status)
    }

    @Test
    fun `een 404 betekent bereikbaar`() {
        roep(antwoord(HttpStatus.NOT_FOUND)).block()

        assertEquals(ModuleState.UP, status().status)
    }

    @Test
    fun `een 5xx is onbereikbaar`() {
        roep(antwoord(HttpStatus.SERVICE_UNAVAILABLE)).block()

        assertEquals(ModuleState.DEGRADED, status().status)
        assertEquals(Oorzaak.UNREACHABLE, status().reden)
    }

    @Test
    fun `een verbindingsfout is onbereikbaar`() {
        assertThrows(Exception::class.java) { roep { Mono.error(IOException("refused")) }.block() }

        assertEquals(Oorzaak.UNREACHABLE, status().reden)
    }

    @Test
    fun `een ontbrekende configuratie in de keten is NOT_CONFIGURED`() {
        val fout = IllegalStateException("verpakt", ModuleNotConfiguredException("zakenapi", "secret"))

        assertThrows(Exception::class.java) { roep { Mono.error(fout) }.block() }

        assertEquals(ModuleState.NOT_CONFIGURED, status().status)
    }

    @Test
    fun `bewaakConfiguratie legt NOT_CONFIGURED vast en gooit de fout opnieuw`() {
        assertThrows(ModuleNotConfiguredException::class.java) {
            registry.bewaakConfiguratie("zakenapi", "zakenapi") { throw ModuleNotConfiguredException("zakenapi", "url") }
        }

        assertEquals(ModuleState.NOT_CONFIGURED, status().status)
    }

    @Test
    fun `bewaakConfiguratie geeft het resultaat door zonder iets vast te leggen`() {
        assertEquals("ok", registry.bewaakConfiguratie("zakenapi", "zakenapi") { "ok" })

        assertEquals(emptyList<ModuleStatus>(), registry.snapshot())
    }
}