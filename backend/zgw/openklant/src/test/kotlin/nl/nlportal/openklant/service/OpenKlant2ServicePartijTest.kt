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
package nl.nlportal.openklant.service

import java.util.UUID
import kotlinx.coroutines.test.runTest
import nl.nlportal.commonground.authentication.JwtBuilder
import nl.nlportal.openklant.TestHelper
import nl.nlportal.openklant.TestHelper.mockJsonResponse
import nl.nlportal.openklant.TestHelper.singlePartij
import nl.nlportal.openklant.TestHelper.singlePartijPage
import nl.nlportal.openklant.TestHelper.url
import nl.nlportal.openklant.autoconfigure.OpenKlantModuleConfiguration
import nl.nlportal.openklant.autoconfigure.OpenKlantModuleConfiguration.OpenKlantConfigurationProperties
import nl.nlportal.openklant.client.OpenKlant2KlantinteractiesClient
import nl.nlportal.openklant.client.domain.OpenKlant2Identificator
import nl.nlportal.openklant.client.domain.OpenKlant2Partij
import nl.nlportal.openklant.client.domain.OpenKlant2PartijIdentificator
import nl.nlportal.openklant.client.domain.PersoonsIdentificatie
import nl.nlportal.openklant.client.domain.SoortPartij
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows

/**
 * Behaviour of [OpenKlant2Service] around the Partij of the authenticated user: finding it (by BSN, or by
 * KVK scoped to a vestigingsnummer), creating it together with its PartijIdentificator (rolling the Partij
 * back when the identificator fails), and what happens when OpenKlant2 or the authentication type misbehaves.
 */
class OpenKlant2ServicePartijTest {
    private lateinit var openklantModuleConfiguration: OpenKlantModuleConfiguration
    private lateinit var mockServer: MockWebServer
    private lateinit var openKlant2Client: OpenKlant2KlantinteractiesClient
    private lateinit var service: OpenKlant2Service

    @BeforeEach
    fun setUp() {
        mockServer = MockWebServer()
        mockServer.start()

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
        service = OpenKlant2Service(openKlant2Client, openklantModuleConfiguration.properties)
    }

    @AfterEach
    internal fun tearDown() {
        mockServer.shutdown()
    }

    @Test
    fun `findPartijByAuthentication returns null when the client call fails`() =
        runTest {
            mockServer.enqueue(MockResponse().setResponseCode(500))

            val partij =
                service.findPartijByAuthentication(
                    JwtBuilder().aanvragerBsn(BSN).buildBurgerAuthentication(),
                )

            assertNull(partij)
        }

    @Test
    fun `findPartijByAuthentication scopes the search to a vestigingsnummer when present`() =
        runTest {
            mockServer.enqueue(mockJsonResponse(singlePartijPage(PARTIJ_ID)))

            val partij =
                service.findPartijByAuthentication(
                    JwtBuilder().aanvragerKvk(KVK).vestigingsNummerKvk(KVK, VESTIGINGSNUMMER).buildBedrijfAuthentication(),
                )

            assertNotNull(partij)
            val request = mockServer.takeRequest()
            assertEquals(VESTIGINGSNUMMER, request.url.queryParameter("partijIdentificator__objectId"))
        }

    @Test
    fun `getPartij returns null when the client call fails`() =
        runTest {
            mockServer.enqueue(MockResponse().setResponseCode(404))

            assertNull(service.getPartij(UUID.fromString(PARTIJ_ID)))
        }

    @Test
    fun `createPartijWithIdentificator rolls back the created Partij when the identificator fails`() =
        runTest {
            // Partij is created successfully...
            mockServer.enqueue(mockJsonResponse(singlePartij(PARTIJ_ID)))
            // ...but attaching the PartijIdentificator fails...
            mockServer.enqueue(MockResponse().setResponseCode(400))
            // ...so the Partij must be deleted again.
            mockServer.enqueue(MockResponse().setResponseCode(204))

            val result =
                service.createPartijWithIdentificator(
                    partijIdentificator = somePartijIdentificator(),
                    partij = somePartij(),
                )

            assertNull(result)
            assertEquals(3, mockServer.requestCount)
            mockServer.takeRequest() // create partij
            mockServer.takeRequest() // failed create identificator
            val deleteRequest = mockServer.takeRequest()
            assertEquals("DELETE", deleteRequest.method)
            assertEquals(listOf("partijen", PARTIJ_ID), deleteRequest.url.pathSegments.takeLast(2))
        }

    @Test
    fun `findPartijIdentificatorenFromAuthentication throws for an unsupported authentication type`() =
        runTest {
            assertThrows<IllegalStateException> {
                service.findPartijIdentificatorenFromAuthentication(
                    JwtBuilder().buildKeycloakUserAuthentication(),
                )
            }
        }

    private fun somePartij() =
        OpenKlant2Partij(
            indicatieActief = true,
            soortPartij = SoortPartij.PERSOON,
            partijIdentificatie = PersoonsIdentificatie(),
        )

    private fun somePartijIdentificator() =
        OpenKlant2PartijIdentificator(
            partijIdentificator =
                OpenKlant2Identificator(
                    objectId = BSN,
                    codeSoortObjectId = "bsn",
                    codeObjecttype = "natuurlijkPersoon",
                    codeRegister = "brp",
                ),
        )

    private fun somePartijIdentificatorJson() =
        """
        {
            "uuid": "$PARTIJ_IDENTIFICATOR_ID",
            "url": "http://localhost:8007/klantinteracties/api/v1/partij-identificatoren/$PARTIJ_IDENTIFICATOR_ID",
            "identificeerdePartij": { "uuid": "$PARTIJ_ID" },
            "partijIdentificator": {
                "objectId": "$BSN",
                "codeSoortObjectId": "bsn",
                "codeObjecttype": "natuurlijkPersoon",
                "codeRegister": "brp"
            }
        }
        """.trimIndent()

    companion object {
        private const val PARTIJ_ID = "9f8e8009-2d1c-40ae-931e-c08861ba7207"
        private const val PARTIJ_IDENTIFICATOR_ID = "dd328a2e-7b8e-4809-be28-f9f894c11c34"
        private const val BSN = "296648875"
        private const val KVK = "69599084"
        private const val VESTIGINGSNUMMER = "000037178899"
    }
}