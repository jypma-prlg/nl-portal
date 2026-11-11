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
package nl.nlportal.haalcentraalbrp.service

import kotlinx.coroutines.runBlocking
import nl.nlportal.commonground.authentication.JwtBuilder
import nl.nlportal.haalcentraalbrp.autoconfiguration.HaalCentraalBrpModuleConfiguration.HaalCentraalBrpConfigurationProperties
import nl.nlportal.haalcentraalbrp.client.HaalCentraalBrpBewoningClient
import nl.nlportal.haalcentraalbrp.client.HaalCentraalBrpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

internal class HaalCentraalBrpServiceTest {
    private lateinit var server: MockWebServer
    private lateinit var properties: HaalCentraalBrpConfigurationProperties
    private lateinit var service: HaalCentraalBrpService
    private val burger = JwtBuilder().aanvragerBsn("999993653").buildBurgerAuthentication()
    private val bedrijf = JwtBuilder().aanvragerKvk("90012768").buildBedrijfAuthentication()

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        properties =
            HaalCentraalBrpConfigurationProperties().apply {
                brpApiUrl = server.url("/").toString()
                bewoningApiUrl = server.url("/").toString()
            }
        service =
            HaalCentraalBrpService(
                properties,
                HaalCentraalBrpClient(properties, null, WebClient.builder()),
                HaalCentraalBrpBewoningClient(properties, null, WebClient.builder()),
            )
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `getPersoon geeft null voor een niet-burger zonder de API aan te roepen`() =
        runBlocking {
            assertThat(service.getPersoon(bedrijf)).isNull()
            assertThat(server.requestCount).isZero()
        }

    @Test
    fun `getPersoon zonder naamgebruik gebruikt de eigen geslachtsnaam`() =
        runBlocking {
            server.enqueue(personenResponse(naam = """{"voorvoegsel": "van", "geslachtsnaam": "Dijk"}"""))

            val persoon = service.getPersoon(burger)

            assertThat(persoon?.naam?.officialLastName).isEqualTo("van Dijk")
        }

    @Test
    fun `getPersoon zonder naam geeft een persoon zonder naam`() =
        runBlocking {
            server.enqueue(personenResponse(naam = null))

            val persoon = service.getPersoon(burger)

            assertThat(persoon).isNotNull
            assertThat(persoon?.naam).isNull()
        }

    @Test
    fun `getPersoon met naamgebruik partner gebruikt de naam van de partner`() =
        runBlocking {
            server.enqueue(personenResponse(naam = naam("P", "Jansen"), partnerNaam = """{"geslachtsnaam": "Bakker"}"""))

            assertThat(service.getPersoon(burger)?.naam?.officialLastName).isEqualTo("Bakker")
        }

    @Test
    fun `getPersoon met naamgebruik partner en eigen combineert beide namen`() =
        runBlocking {
            server.enqueue(personenResponse(naam = naam("V", "Jansen"), partnerNaam = """{"geslachtsnaam": "Bakker"}"""))

            assertThat(service.getPersoon(burger)?.naam?.officialLastName).isEqualTo("Bakker - Jansen")
        }

    @Test
    fun `getPersoon met naamgebruik eigen en partner combineert beide namen`() =
        runBlocking {
            server.enqueue(personenResponse(naam = naam("N", "Jansen"), partnerNaam = """{"geslachtsnaam": "Bakker"}"""))

            assertThat(service.getPersoon(burger)?.naam?.officialLastName).isEqualTo("Jansen - Bakker")
        }

    @Test
    fun `getPersoon met naamgebruik eigen gebruikt de eigen geslachtsnaam`() =
        runBlocking {
            server.enqueue(personenResponse(naam = naam("E", "Jansen"), partnerNaam = """{"geslachtsnaam": "Bakker"}"""))

            assertThat(service.getPersoon(burger)?.naam?.officialLastName).isEqualTo("Jansen")
        }

    @Test
    fun `getBewoningen geeft null voor een niet-burger`() =
        runBlocking {
            assertThat(service.getBewoningen(bedrijf, "0226010000038820")).isNull()
            assertThat(server.requestCount).isZero()
        }

    @Test
    fun `getBewoningen geeft null als de API faalt`() =
        runBlocking {
            server.enqueue(MockResponse().setResponseCode(500))

            assertThat(service.getBewoningen(burger, "0226010000038820")).isNull()
        }

    @Test
    fun `getBewonersAantal telt de bewoners van de eerste bewoning`() =
        runBlocking {
            server.enqueue(bewoningenResponse())

            assertThat(service.getBewonersAantal(burger, "0226010000038820")).isEqualTo(2)
        }

    @Test
    fun `getBewonersAantal geeft null als de woonplaats niet is toegestaan`() =
        runBlocking {
            properties.allowedCitiesForBewoning = listOf("'s-Gravenhage")

            assertThat(service.getBewonersAantal(burger, "0226010000038820", "Utrecht")).isNull()
            assertThat(service.getBewonersAantal(burger, "0226010000038820", null)).isNull()
            assertThat(server.requestCount).isZero()
        }

    @Test
    fun `getBewonersAantal vraagt de API aan als de woonplaats is toegestaan`() =
        runBlocking {
            properties.allowedCitiesForBewoning = listOf("'s-Gravenhage")
            server.enqueue(bewoningenResponse())

            assertThat(service.getBewonersAantal(burger, "0226010000038820", "'s-Gravenhage")).isEqualTo(2)
        }

    private fun naam(
        gebruik: String,
        geslachtsnaam: String,
    ) = """{"geslachtsnaam": "$geslachtsnaam", "aanduidingNaamgebruik": {"code": "$gebruik"}}"""

    private fun personenResponse(
        naam: String?,
        partnerNaam: String? = null,
    ): MockResponse {
        val fields =
            listOfNotNull(
                naam?.let { """"naam": $it""" },
                partnerNaam?.let { """"partners": [{"naam": $it}]""" },
            ).joinToString(",")
        return json("""{"type": "RaadpleegMetBurgerservicenummer", "personen": [{$fields}]}""")
    }

    private fun bewoningenResponse() =
        json(
            """{"bewoningen": [{"adresseerbaarObjectIdentificatie": "0226010000038820",
                "bewoners": [{"burgerservicenummer": "1"}, {"burgerservicenummer": "2"}]}]}""",
        )

    private fun json(body: String) =
        MockResponse()
            .setBody(body)
            .addHeader("Content-Type", "application/json")
}