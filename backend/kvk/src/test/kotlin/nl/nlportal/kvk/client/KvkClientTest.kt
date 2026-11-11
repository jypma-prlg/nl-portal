/*
 * Copyright 2015-2022 Den Haag, Ritense, the Netherlands.
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

import tools.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.runBlocking
import nl.nlportal.kvk.client.KvkConfig.KvkConfigProperties
import nl.nlportal.kvk.domain.MaatschappelijkeActiviteit
import nl.nlportal.kvk.domain.MaterieleRegistratie
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class KvkClientTest {
    private lateinit var kvkClientConfigProperties: KvkConfigProperties
    private lateinit var client: KvkClient
    private lateinit var server: MockWebServer
    private val kvkNummer = "90012768"

    @BeforeEach
    internal fun setUp() {
        server = MockWebServer()
        server.start()

        server.enqueue(
            MockResponse()
                .setBody(
                    jacksonObjectMapper().writeValueAsString(
                        MaatschappelijkeActiviteit(
                            naam = "Test bedrijf",
                            "90012768",
                            "test",
                            "20230101",
                            MaterieleRegistratie("20020202"),
                            1,
                            "Test bedrijf",
                            listOf(),
                            listOf(),
                            null,
                        ),
                    ),
                ).addHeader("Content-Type", "application/json"),
        )
        kvkClientConfigProperties = KvkConfigProperties(url = server.url("/").toString())
        client = KvkClient(kvkClientConfigProperties)
    }

    @AfterEach
    internal fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `should get bedrijf by without certificate`() {
        runBlocking {
            val bedrijf = client.getMaatschappelijkeActiviteit(kvkNummer)

            assertThat(bedrijf).isNotNull
        }
    }
}