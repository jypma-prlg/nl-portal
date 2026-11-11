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
package nl.nlportal.haalcentraalbrp.client

import io.netty.handler.ssl.SslContextBuilder
import kotlinx.coroutines.runBlocking
import nl.nlportal.commonground.authentication.JwtBuilder
import nl.nlportal.core.ssl.ClientSslContextResolver
import nl.nlportal.core.ssl.Ssl
import nl.nlportal.haalcentraalbrp.autoconfiguration.HaalCentraalBrpModuleConfiguration.HaalCentraalBrpConfigurationProperties
import nl.nlportal.haalcentraalbrp.client.path.Bewoningen
import nl.nlportal.haalcentraalbrp.client.path.Personen
import nl.nlportal.haalcentraalbrp.domain.bewoning.BewoningenApiRequest
import nl.nlportal.haalcentraalbrp.domain.brp.BrpApiRequest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.web.reactive.function.client.WebClient
import java.util.concurrent.TimeUnit

internal class HaalCentraalBrpClientTest {
    private lateinit var server: MockWebServer
    private lateinit var properties: HaalCentraalBrpConfigurationProperties
    private val authentication = JwtBuilder().aanvragerBsn("999993653").buildBurgerAuthentication()

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        properties =
            HaalCentraalBrpConfigurationProperties().apply {
                brpApiUrl = server.url("/").toString()
                bewoningApiUrl = server.url("/").toString()
                apiKey = "geheime-sleutel"
                additionalHeaders = mapOf("X-Extra" to "waarde")
            }
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `personen post stuurt api key, extra headers en token mee`() =
        runBlocking {
            server.enqueue(jsonResponse("""{"type": "RaadpleegMetBurgerservicenummer", "personen": []}"""))
            val client = HaalCentraalBrpClient(properties, null, WebClient.builder())

            val response =
                client
                    .path<Personen>()
                    .post(BrpApiRequest(fields = listOf("naam"), burgerservicenummer = listOf("999993653")), authentication)

            val request = requireNotNull(server.takeRequest(5, TimeUnit.SECONDS))
            assertThat(response.personen).isEmpty()
            assertThat(request.path).isEqualTo("/personen")
            assertThat(request.getHeader("X-API-KEY")).isEqualTo("geheime-sleutel")
            assertThat(request.getHeader("X-Extra")).isEqualTo("waarde")
            assertThat(request.getHeader("Authorization")).startsWith("Bearer ")
        }

    @Test
    fun `bewoningen post stuurt api key, extra headers en token mee`() =
        runBlocking {
            server.enqueue(jsonResponse("""{"bewoningen": []}"""))
            val client = HaalCentraalBrpBewoningClient(properties, null, WebClient.builder())

            val response =
                client
                    .path<Bewoningen>()
                    .post(BewoningenApiRequest("BewoningMetPeildatum", "2026-01-01", "0226010000038820"), authentication)

            val request = requireNotNull(server.takeRequest(5, TimeUnit.SECONDS))
            assertThat(response.bewoningen).isEmpty()
            assertThat(request.path).isEqualTo("/bewoningen")
            assertThat(request.getHeader("X-API-KEY")).isEqualTo("geheime-sleutel")
            assertThat(request.getHeader("X-Extra")).isEqualTo("waarde")
            assertThat(request.getHeader("Authorization")).startsWith("Bearer ")
        }

    @Test
    fun `zonder api key en extra headers worden die niet meegestuurd`() =
        runBlocking {
            properties.apiKey = null
            properties.additionalHeaders = emptyMap()
            server.enqueue(jsonResponse("""{"type": "RaadpleegMetBurgerservicenummer", "personen": []}"""))
            val client = HaalCentraalBrpClient(properties, null, WebClient.builder())

            client
                .path<Personen>()
                .post(BrpApiRequest(fields = listOf("naam"), burgerservicenummer = listOf("999993653")), authentication)

            val request = requireNotNull(server.takeRequest(5, TimeUnit.SECONDS))
            assertThat(request.getHeader("X-API-KEY")).isNull()
            assertThat(request.getHeader("X-Extra")).isNull()
        }

    @Test
    fun `de ssl context wordt opgehaald als ssl is ingesteld`() {
        val resolver = mock<ClientSslContextResolver>()
        whenever(resolver.resolve(anyOrNull(), anyOrNull())).thenReturn(SslContextBuilder.forClient().build())
        properties.ssl = Ssl(trustedCertificate = "certificaat")

        val brpClient = HaalCentraalBrpClient(properties, resolver, WebClient.builder())
        val bewoningClient = HaalCentraalBrpBewoningClient(properties, resolver, WebClient.builder())

        assertThat(brpClient.webClient).isNotNull
        assertThat(bewoningClient.webClient).isNotNull
        verify(resolver, org.mockito.kotlin.times(2)).resolve(anyOrNull(), any())
    }

    private fun jsonResponse(body: String) =
        MockResponse()
            .setBody(body)
            .addHeader("Content-Type", "application/json")
}