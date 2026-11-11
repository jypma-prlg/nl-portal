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
package nl.nlportal.kvk.client

import io.netty.handler.ssl.SslContextBuilder
import kotlinx.coroutines.runBlocking
import nl.nlportal.core.ssl.ClientSslContextResolver
import nl.nlportal.core.ssl.Ssl
import nl.nlportal.kvk.client.KvkConfig.KvkConfigProperties
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.concurrent.TimeUnit

internal class KvkClientHeadersTest {
    private lateinit var server: MockWebServer

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `getVestiging vraagt het vestigingsprofiel op met de api key`() =
        runBlocking {
            server.enqueue(
                MockResponse()
                    .setBody("""{"vestigingsnummer": "000037178598"}""")
                    .addHeader("Content-Type", "application/json"),
            )
            val client = KvkClient(KvkConfigProperties(url = server.url("/").toString(), apiKey = "geheime-sleutel"))

            client.getVestiging("000037178598")

            val request = requireNotNull(server.takeRequest(5, TimeUnit.SECONDS))
            assertThat(request.path).isEqualTo("/vestigingsprofielen/000037178598")
            assertThat(request.getHeader("X-API-KEY")).isEqualTo("geheime-sleutel")
        }

    @Test
    fun `de ssl context wordt opgehaald als ssl is ingesteld`() {
        val resolver = mock<ClientSslContextResolver>()
        whenever(resolver.resolve(anyOrNull(), anyOrNull())).thenReturn(SslContextBuilder.forClient().build())
        val properties =
            KvkConfigProperties(url = server.url("/").toString()).apply {
                ssl = Ssl(trustedCertificate = "certificaat")
            }

        val webClient = KvkClient(properties, resolver).webClient()

        assertThat(webClient).isNotNull
        verify(resolver).resolve(anyOrNull(), anyOrNull())
    }
}