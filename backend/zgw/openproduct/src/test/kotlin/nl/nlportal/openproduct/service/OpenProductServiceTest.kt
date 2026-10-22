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
package nl.nlportal.openproduct.service

import kotlinx.coroutines.test.runTest
import nl.nlportal.commonground.authentication.AuthenticationMachtigingsDienstService
import nl.nlportal.commonground.authentication.JwtBuilder
import nl.nlportal.documentenapi.service.DocumentenApiService
import nl.nlportal.openproduct.autoconfigure.OpenProductModuleConfiguration.OpenProductConfigurationProperties
import nl.nlportal.openproduct.client.OpenProductClient
import nl.nlportal.openproduct.client.OpenProductTypeClient
import nl.nlportal.openproduct.client.domain.OpenProductUrl
import nl.nlportal.zakenapi.domain.Zaak
import nl.nlportal.zakenapi.service.ZakenApiService
import nl.nlportal.zgw.taak.autoconfigure.TaakConfig.TaakConfigProperties
import nl.nlportal.zgw.taak.service.TaakService
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.net.URI
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertFailsWith

/**
 * Covers the failure-policy categories from ADR 0003: get-by-id (only an upstream 404 becomes
 * null, everything else propagates), list (always propagates), and loop/enrichment (one failing
 * item is logged and skipped, the rest of the list survives).
 */
class OpenProductServiceTest {
    @Mock
    private lateinit var authenticationMachtigingsDienstService: AuthenticationMachtigingsDienstService

    @Mock
    private lateinit var documentenApiService: DocumentenApiService

    @Mock
    private lateinit var taakService: TaakService

    @Mock
    private lateinit var taakConfigProperties: TaakConfigProperties

    @Mock
    private lateinit var zakenApiService: ZakenApiService

    private lateinit var mockServer: MockWebServer
    private lateinit var openProductTypeClient: OpenProductTypeClient
    private lateinit var openProductService: OpenProductService

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        mockServer = MockWebServer()
        mockServer.start()
        val host = "http://${mockServer.hostName}:${mockServer.port}"

        openProductTypeClient =
            OpenProductTypeClient(
                OpenProductConfigurationProperties().apply {
                    productTypeApiUrl = URI.create(host)
                    token = "test-token"
                },
            )
        val openProductClient =
            OpenProductClient(
                OpenProductConfigurationProperties().apply {
                    productApiUrl = URI.create(host)
                    token = "test-token"
                },
            )

        openProductService =
            OpenProductService(
                openProductClient = openProductClient,
                openProductTypeClient = openProductTypeClient,
                objectsApiTaskConfigProperties = taakConfigProperties,
                zakenApiService = zakenApiService,
                authenticationMachtigingsDienstService = authenticationMachtigingsDienstService,
                documentenApiService = documentenApiService,
                taakService = taakService,
            )
    }

    @AfterEach
    fun tearDown() {
        mockServer.shutdown()
    }

    // --- get-by-id category (e.g. getProductType) ---

    @Test
    fun `getProductType - upstream 404 becomes null`() =
        runTest {
            mockServer.enqueue(MockResponse().setResponseCode(404))

            val result = openProductService.getProductType(UUID.randomUUID())

            assertNull(result)
        }

    @Test
    fun `getProductType - upstream 500 propagates instead of being swallowed to null`() =
        runTest {
            // red, pre-ADR-0003: this used to be caught by a blanket catch(Exception) and logged
            // away to null, indistinguishable from a legitimate "not found".
            mockServer.enqueue(MockResponse().setResponseCode(500))

            assertFailsWith<WebClientResponseException> {
                openProductService.getProductType(UUID.randomUUID())
            }
        }

    // --- list category (e.g. getActies) ---

    @Test
    fun `getActies - upstream failure propagates instead of a silent empty page`() =
        runTest {
            // red, pre-ADR-0003: this used to be caught by a blanket catch(Exception) and logged
            // away to an empty ResultPage, indistinguishable from "no acties configured".
            mockServer.enqueue(MockResponse().setResponseCode(500))

            assertFailsWith<WebClientResponseException> {
                openProductService.getActies(pageNumber = 1, pageSize = 10)
            }
        }

    // --- loop/enrichment category (e.g. getProductZaken) ---

    @Test
    fun `getProductZaken - a failing zaak is skipped, the rest of the list survives`() =
        runTest {
            val authentication = JwtBuilder().aanvragerBsn("123456789").buildBurgerAuthentication()
            val okZaakId = UUID.randomUUID()
            val brokenZaakId = UUID.randomUUID()
            val zaak =
                Zaak(
                    uuid = okZaakId,
                    url = "http://localhost/zaken/$okZaakId",
                    identificatie = "ZAAK-1",
                    omschrijving = "test",
                    zaaktype = "http://localhost/zaaktype",
                    startdatum = LocalDate.now(),
                    einddatum = null,
                    status = null,
                )

            whenever(zakenApiService.getZaak(id = eq(okZaakId), authentication = any())).thenReturn(zaak)
            whenever(zakenApiService.getZaak(id = eq(brokenZaakId), authentication = any()))
                .thenThrow(RuntimeException("upstream is down"))

            val result =
                openProductService.getProductZaken(
                    authentication = authentication,
                    zaken =
                        listOf(
                            OpenProductUrl(url = "http://localhost/zaken/$brokenZaakId"),
                            OpenProductUrl(url = "http://localhost/zaken/$okZaakId"),
                        ),
                )

            assertEquals(listOf(zaak), result)
        }
}