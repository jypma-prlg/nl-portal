/*
 * Copyright 2015-2024 Den Haag, Ritense, the Netherlands.
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
package nl.nlportal.openklant.service

import java.util.UUID
import kotlinx.coroutines.test.runTest
import nl.nlportal.commonground.authentication.JwtBuilder
import nl.nlportal.openklant.TestHelper
import nl.nlportal.openklant.TestHelper.mockJsonResponse
import nl.nlportal.openklant.TestHelper.url
import nl.nlportal.openklant.autoconfigure.OpenKlantModuleConfiguration
import nl.nlportal.openklant.autoconfigure.OpenKlantModuleConfiguration.OpenKlantConfigurationProperties
import nl.nlportal.openklant.client.OpenKlant2KlantinteractiesClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class OpenKlant2ServiceTest {
    private lateinit var openklantModuleConfiguration: OpenKlantModuleConfiguration
    private lateinit var mockServer: MockWebServer
    private lateinit var openKlant2Client: OpenKlant2KlantinteractiesClient
    private lateinit var hostUrl: String
    private lateinit var apiUrl: String

    @BeforeEach
    fun setUp() {
        mockServer = MockWebServer()
        mockServer.start()

        hostUrl = "http://${mockServer.hostName}:${mockServer.port}/"
        apiUrl = "http://${mockServer.hostName}:${mockServer.port}/myapi/v1"
        openklantModuleConfiguration =
            OpenKlantModuleConfiguration().apply {
                enabled = true
                properties =
                    OpenKlantConfigurationProperties(
                        klantinteractiesApiUrl = mockServer.url("/myapi/v1").toUri(),
                        token = "SuperSecretToken1234",
                    )
            }

        openKlant2Client = OpenKlant2KlantinteractiesClient(openklantModuleConfiguration.properties)
    }

    @AfterEach
    internal fun tearDown() {
        mockServer.shutdown()
    }

    @Test
    fun `should delete DigitaleAdres that belongs to the authenticated user`() =
        runTest {
            val service = OpenKlant2Service(openKlant2Client, openklantModuleConfiguration.properties)
            mockServer.enqueue(mockJsonResponse(digitaleAdressenPage))
            mockServer.enqueue(MockResponse().setResponseCode(204))

            service.deleteDigitaleAdresById(
                authentication = JwtBuilder().aanvragerBsn(OWNER_BSN).buildBurgerAuthentication(),
                digitaleAdresId = UUID.fromString(DIGITALE_ADRES_ID),
            )

            // the digitale adressen are searched scoped to the authenticated user
            val searchRequest = mockServer.takeRequest()
            assertEquals(OWNER_BSN, searchRequest.url.queryParameter("verstrektDoorPartij__partijIdentificator__objectId"))

            val deleteRequest = mockServer.takeRequest()
            assertEquals("DELETE", deleteRequest.method)
            assertEquals(listOf("digitaleadressen", DIGITALE_ADRES_ID), deleteRequest.url.pathSegments.takeLast(2))
        }

    @Test
    fun `should not delete DigitaleAdres of another user`() =
        runTest {
            val service = OpenKlant2Service(openKlant2Client, openklantModuleConfiguration.properties)
            // the scoped search of the other user does not return the digitale adres
            mockServer.enqueue(mockJsonResponse(TestHelper.emptyPage))

            val exception =
                assertThrows<ResponseStatusException> {
                    service.deleteDigitaleAdresById(
                        authentication = JwtBuilder().aanvragerBsn(OTHER_BSN).buildBurgerAuthentication(),
                        digitaleAdresId = UUID.fromString(DIGITALE_ADRES_ID),
                    )
                }

            assertEquals(HttpStatus.UNAUTHORIZED, exception.statusCode)
            // no DELETE request left this service
            assertEquals(1, mockServer.requestCount)
            assertEquals("GET", mockServer.takeRequest().method)
        }

    @Test
    fun `findDigitaleAdressen returns an empty list when the client call fails`() =
        runTest {
            val service = OpenKlant2Service(openKlant2Client, openklantModuleConfiguration.properties)
            mockServer.enqueue(MockResponse().setResponseCode(500))

            val result =
                service.findDigitaleAdressen(
                    JwtBuilder().aanvragerBsn(OWNER_BSN).buildBurgerAuthentication(),
                )

            assertEquals(emptyList<Any>(), result)
        }

    @Test
    fun `findDigitalAdres returns null when the Digitale Adres can not be found`() =
        runTest {
            val service = OpenKlant2Service(openKlant2Client, openklantModuleConfiguration.properties)
            mockServer.enqueue(MockResponse().setResponseCode(404))

            assertNull(service.findDigitalAdres(UUID.fromString(DIGITALE_ADRES_ID)))
        }

    companion object {
        private const val DIGITALE_ADRES_ID = "1300d2ab-13cb-4c12-9818-3b76e2a5d993"
        private const val OWNER_BSN = "296648875"
        private const val OTHER_BSN = "999999999"

        private val digitaleAdressenPage =
            """
            {
                "count": 1,
                "next": null,
                "previous": null,
                "results": [
                    {
                        "uuid": "$DIGITALE_ADRES_ID",
                        "url": "http://localhost:8007/klantinteracties/api/v1/digitaleadressen/$DIGITALE_ADRES_ID",
                        "adres": "lucas@boom.nl",
                        "soortDigitaalAdres": "email",
                        "omschrijving": "Persoonlijke email adres"
                    }
                ]
            }
            """.trimIndent()
    }
}