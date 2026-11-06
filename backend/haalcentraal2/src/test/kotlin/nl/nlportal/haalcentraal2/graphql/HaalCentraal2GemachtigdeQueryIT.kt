/*
 * Copyright 2015-2023 Den Haag, Ritense, the Netherlands.
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
package nl.nlportal.haalcentraal2.graphql

import tools.jackson.databind.JsonNode
import nl.nlportal.commonground.authentication.WithBurgerUser
import nl.nlportal.haalcentraal2.TestHelper
import nl.nlportal.haalcentraal.hr.client.HaalCentraalHrConfig
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.graphql.test.tester.HttpGraphQlTester
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@SpringBootTest
@AutoConfigureHttpGraphQlTester
@AutoConfigureWebTestClient(timeout = "36000")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
internal class HaalCentraal2GemachtigdeQueryIT(
    @Autowired private val httpGraphQlTester: HttpGraphQlTester,
    @Autowired private val haalCentraalHrClientConfig: HaalCentraalHrConfig,
) {
    companion object {
        @JvmStatic
        var server: MockWebServer? = null

        @JvmStatic
        var url: String = ""

        @JvmStatic
        @DynamicPropertySource
        fun properties(propsRegistry: DynamicPropertyRegistry) {
            propsRegistry.add("nl-portal.config.haalcentraal2.properties.brp-api-url") { url }
            propsRegistry.add("nl-portal.config.haalcentraal.hr.properties.url") { url }
        }

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            server = MockWebServer()
            server?.start()
            url = server?.url("/brp").toString()
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            server?.shutdown()
        }
    }

    @BeforeEach
    internal fun setUp() {
        setupMockServer()
        url = server?.url("/").toString()
        haalCentraalHrClientConfig.properties.url = url
    }

    @Test
    @WithBurgerUser("318634776", gemachtigdeBsn = "999993847")
    fun `getGemachtigde with bsn`() {
        val query =
            """
            query {
                getGemachtigdeV2 {
                    persoon {
                        burgerservicenummer,
                        naam {
                            geslachtsnaam,
                            volledigeNaam,
                        }
                    }
                }
            }
            """.trimIndent()

        val responseBody =
            httpGraphQlTester
                .document(query)
                .execute()
                .errors()
                .verify()
                .path("getGemachtigdeV2")
                .entity(JsonNode::class.java)
                .get()

        assertEquals("Pieter Jan de Vries", responseBody.requiredAt("/persoon/naam/volledigeNaam")?.stringValue())
        assertEquals("Vries", responseBody.requiredAt("/persoon/naam/geslachtsnaam")?.stringValue())
    }

    @Test
    @WithBurgerUser("318634776", gemachtigdeKvk = "90012768")
    fun `getGemachtigde with kvk`() {
        val query =
            """
            query {
                getGemachtigdeV2 {
                    bedrijf {
                        naam
                        embedded{
                            eigenaar {
                                rechtsvorm
                            }
                            vestiging {
                                eersteHandelsnaam
                                adressen {
                                    straatnaam
                                    huisnummer
                                    postbusnummer
                                    postcode
                                    plaats
                                    volledigAdres
                                }
                            }
                        }
                    }
                }
            }
            """.trimIndent()

        val responseBody =
            httpGraphQlTester
                .document(query)
                .execute()
                .errors()
                .verify()
                .path("getGemachtigdeV2")
                .entity(JsonNode::class.java)
                .get()

        assertEquals("Test bedrijf", responseBody.requiredAt("/bedrijf/naam")?.stringValue())
        assertEquals("Eenmanszaak", responseBody.requiredAt("/bedrijf/embedded/eigenaar/rechtsvorm")?.stringValue())
        assertEquals("Test bedrijf", responseBody.requiredAt("/bedrijf/embedded/vestiging/eersteHandelsnaam")?.stringValue())
        assertEquals("Postbus 1000 2260BA LEIDSCHENDAM", responseBody.requiredAt("/bedrijf/embedded/vestiging/adressen/0/volledigAdres")?.stringValue())
    }

    private fun setupMockServer() {
        val dispatcher: Dispatcher =
            object : Dispatcher() {
                @Throws(InterruptedException::class)
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val path = request.path?.substringBefore('?')
                    val response =
                        when (request.method + " " + path) {
                            "POST /brp/personen" -> {
                                TestHelper.mockResponseFromFile("/data/get-personen.json")
                            }

                            "GET /basisprofielen/90012768" -> {
                                TestHelper.mockResponseFromFile(
                                    "/data/get-maatschappelijke-activiteiten.json",
                                )
                            }

                            else -> {
                                MockResponse().setResponseCode(404)
                            }
                        }
                    return response
                }
            }
        server?.dispatcher = dispatcher
    }
}