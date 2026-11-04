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
package nl.nlportal.zgw.taak.service

import kotlinx.coroutines.test.runTest
import nl.nlportal.commonground.authentication.AuthenticationMachtigingsDienstService
import nl.nlportal.commonground.authentication.JwtBuilder
import nl.nlportal.zgw.objectenapi.autoconfiguration.ObjectsApiClientConfig.ObjectsApiClientConfigProperties
import nl.nlportal.zgw.objectenapi.client.ObjectsApiClient
import nl.nlportal.zgw.taak.autoconfigure.TaakConfig.TaakConfigProperties
import nl.nlportal.zgw.taak.domain.TaakStatus
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.net.URLDecoder
import java.util.UUID

/**
 * Covers the error/fallback and authorization branches of [TaakService]
 * against a mocked Objects API (MockWebServer).
 */
class TaakServiceTest {
    private lateinit var mockServer: MockWebServer
    private lateinit var machtigingsDienstService: AuthenticationMachtigingsDienstService
    private lateinit var service: TaakService

    @BeforeEach
    fun setUp() {
        mockServer = MockWebServer()
        mockServer.start()

        machtigingsDienstService = mock()
        whenever(machtigingsDienstService.isAllowedTaakType(any(), any())).thenReturn(true)

        val objectsApiClient =
            ObjectsApiClient(
                ObjectsApiClientConfigProperties(
                    url = mockServer.url("/").toUri(),
                    token = "token",
                ),
            )
        service =
            TaakService(
                objectsApiClient,
                TaakConfigProperties().apply { objectTypeUrl = OBJECT_TYPE_URL },
                machtigingsDienstService,
            )
    }

    @AfterEach
    fun tearDown() {
        mockServer.shutdown()
    }

    @Test
    fun `getTakenV2 returns an empty page when the Objects API fails`() =
        runTest {
            mockServer.enqueue(MockResponse().setResponseCode(500))

            val page = service.getTakenV2(1, 10, burger())

            assertEquals(0, page.content.size)
            assertEquals(0, page.totalElements)
            assertEquals(1, page.number)
            assertEquals(10, page.size)
        }

    @Test
    fun `getTakenV2 searches open taken of the user by default`() =
        runTest {
            mockServer.enqueue(json(resultPage(taakJson())))

            val page = service.getTakenV2(1, 10, burger())

            assertEquals(1, page.content.size)
            val query = decodedQuery()
            assertTrue(query.contains("identificatie__type__exact__bsn"))
            assertTrue(query.contains("identificatie__value__exact__$BSN"))
            assertTrue(query.contains("status__exact__open"))
            assertTrue(query.contains("type=$OBJECT_TYPE_URL"))
            assertTrue(query.contains("ordering=-record__startAt"))
        }

    @Test
    fun `getTakenV2 applies status, zaak, title and eigenaar filters`() =
        runTest {
            val zaakId = UUID.randomUUID()
            whenever(machtigingsDienstService.taakTypes(any())).thenReturn(listOf("gzac", "other"))
            mockServer.enqueue(json(resultPage()))

            service.getTakenV2(1, 10, burger(), zaakId, TaakStatus.AFGEROND, "belasting")

            val query = decodedQuery()
            assertTrue(query.contains("status__exact__afgerond"))
            assertTrue(query.contains("koppeling__registratie__exact__zaak"))
            assertTrue(query.contains("koppeling__value__icontains__$zaakId"))
            assertTrue(query.contains("titel__icontains__belasting"))
            assertTrue(query.contains("eigenaar__in__gzac|other"))
        }

    @Test
    fun `getTaken wraps the result with the number of returned taken`() =
        runTest {
            mockServer.enqueue(json(resultPage(taakJson(), taakJson())))

            val page = service.getTaken(2, 5, burger())

            assertEquals(2, page.content.size)
            assertEquals(2, page.totalElements)
            assertEquals(2, page.number)
            assertEquals(5, page.size)
        }

    @Test
    fun `getTaakByIdV2 returns the taak for its owner`() =
        runTest {
            mockServer.enqueue(json(taakJson()))

            val taak = service.getTaakByIdV2(UUID.fromString(TAAK_ID), burger())

            assertEquals(UUID.fromString(TAAK_ID), taak.id)
            assertEquals(TaakStatus.OPEN, taak.status)
        }

    @Test
    fun `getTaakByIdV2 denies access to a taak of another user`() =
        runTest {
            mockServer.enqueue(json(taakJson()))

            val exception =
                assertThrows<ResponseStatusException> {
                    service.getTaakByIdV2(UUID.fromString(TAAK_ID), burger("999999999"))
                }

            assertEquals(HttpStatus.UNAUTHORIZED, exception.statusCode)
        }

    @Test
    fun `getTaakByIdV2 denies access when the taak type is not allowed`() =
        runTest {
            whenever(machtigingsDienstService.isAllowedTaakType(any(), any())).thenReturn(false)
            mockServer.enqueue(json(taakJson()))

            val exception =
                assertThrows<ResponseStatusException> {
                    service.getTaakByIdV2(UUID.fromString(TAAK_ID), burger())
                }

            assertEquals(HttpStatus.UNAUTHORIZED, exception.statusCode)
        }

    @Test
    fun `getTaakByIdV2 fails with bad request when the taak does not exist`() =
        runTest {
            // an empty body is mapped to null by the client
            mockServer.enqueue(MockResponse().setResponseCode(200))

            val exception =
                assertThrows<ResponseStatusException> {
                    service.getTaakByIdV2(UUID.fromString(TAAK_ID), burger())
                }

            assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
        }

    @Test
    fun `getTaakByIdV2 propagates an error status from the Objects API`() =
        runTest {
            mockServer.enqueue(MockResponse().setResponseCode(404))

            val exception =
                assertThrows<ResponseStatusException> {
                    service.getTaakByIdV2(UUID.fromString(TAAK_ID), burger())
                }

            assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
        }

    @Test
    fun `submitTaak completes an open taak and stores the submission`() =
        runTest {
            mockServer.enqueue(json(taakJson()))
            mockServer.enqueue(json(taakJson(status = "afgerond")))

            val result = service.submitTaak(UUID.fromString(TAAK_ID), mapOf("voornaam" to "Piet"), burger())

            assertEquals(TaakStatus.AFGEROND, result.status)
            mockServer.takeRequest() // GET
            val put = mockServer.takeRequest()
            assertEquals("PUT", put.method)
            val body = put.body.readUtf8()
            assertTrue(body.contains("\"afgerond\""))
            assertTrue(body.contains("\"Piet\""))
            assertTrue(body.contains("BSN:$BSN"))
        }

    @Test
    fun `submitTaakV2 rejects a taak that is not open`() =
        runTest {
            mockServer.enqueue(json(taakJson(status = "afgerond")))

            val exception =
                assertThrows<ResponseStatusException> {
                    service.submitTaakV2(UUID.fromString(TAAK_ID), mapOf("a" to "b"), burger())
                }

            assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
            // nothing was written
            assertEquals(1, mockServer.requestCount)
        }

    @Test
    fun `submitTaakV2 denies access to a taak of another user`() =
        runTest {
            mockServer.enqueue(json(taakJson()))

            val exception =
                assertThrows<ResponseStatusException> {
                    service.submitTaakV2(UUID.fromString(TAAK_ID), mapOf("a" to "b"), burger("999999999"))
                }

            assertEquals(HttpStatus.UNAUTHORIZED, exception.statusCode)
            assertEquals(1, mockServer.requestCount)
        }

    private fun burger(bsn: String = BSN) = JwtBuilder().aanvragerBsn(bsn).buildBurgerAuthentication()

    private fun decodedQuery(): String = URLDecoder.decode(mockServer.takeRequest().path!!, Charsets.UTF_8)

    private fun json(body: String) =
        MockResponse()
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .setResponseCode(200)
            .setBody(body)

    private fun resultPage(vararg results: String) =
        """
        {
            "count": ${results.size},
            "next": null,
            "previous": null,
            "results": [${results.joinToString(",")}]
        }
        """.trimIndent()

    private fun taakJson(status: String = "open") =
        """
        {
            "uuid": "$TAAK_ID",
            "type": "$OBJECT_TYPE_URL",
            "record": {
                "index": 9,
                "typeVersion": 1,
                "startAt": "2022-06-01",
                "data": {
                    "titel": "Very important task",
                    "status": "$status",
                    "soort": "portaalformulier",
                    "verloopdatum": "2023-09-20T18:25:43.524Z",
                    "eigenaar": "gzac",
                    "identificatie": { "type": "bsn", "value": "$BSN" },
                    "koppeling": { "registratie": "zaak", "value": "5551a7c5-4e92-43e6-8d23-80359b7e22b7" },
                    "portaalformulier": {
                        "formulier": { "soort": "url", "value": "http://localhost:8010/api/v2/objects/4e40fb4c" },
                        "data": { "voornaam": "Jan" }
                    },
                    "verwerker_taak_id": "58fad5ab-dc2f-11ec-9075-f22a405ce708"
                }
            }
        }
        """.trimIndent()

    companion object {
        private const val TAAK_ID = "58fad5ab-dc2f-11ec-9075-f22a405ce707"
        private const val BSN = "569312863"
        private const val OBJECT_TYPE_URL = "http://localhost:8010/api/v1/objecttypes/3e852115-277a-4570-873a-9a64be3aeb35"
    }
}