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
package nl.nlportal.core.koppeling

import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jwt.SignedJWT
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.http.client.reactive.MockClientHttpRequest
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.nio.file.Path
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import java.util.Base64

class KoppelingWebClientFactoryTest {
    private val tokenUrl = "https://idp.example.nl/token"
    private var tokenAntwoord = """{"access_token":"uitgegeven-token","token_type":"Bearer","expires_in":300}"""

    /** Vervangt het netwerk: legt elk uitgaand verzoek vast; het token-eindpunt geeft een token, de rest een lege 200. */
    private val verzoeken = mutableListOf<ClientRequest>()
    private val netwerk =
        ExchangeFunction { verzoek ->
            verzoeken += verzoek
            val antwoord = ClientResponse.create(HttpStatus.OK)
            if (verzoek.url().toString() == tokenUrl) {
                antwoord
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(tokenAntwoord)
            } else {
                antwoord.body("{}")
            }.let { Mono.just(it.build()) }
        }
    private val factory = KoppelingWebClientFactory(WebClient.builder().exchangeFunction(netwerk))

    private fun koppeling(configure: Koppeling.() -> Unit): Koppeling =
        Koppeling().apply {
            url = "https://api.example.nl"
            configure()
        }

    private fun clientCredentials() =
        koppeling {
            auth.type = KoppelingAuthType.OAUTH2_CLIENT_CREDENTIALS
            auth.tokenUrl = tokenUrl
            auth.clientId = "nl-portal"
            auth.secret = "geheim"
        }

    private fun roep(
        koppeling: Koppeling,
        client: WebClient = factory.webClient("testmodule", koppeling),
    ) {
        client
            .get()
            .uri("/ping")
            .retrieve()
            .bodyToMono(String::class.java)
            .block()
    }

    /** Doet één aanroep via de koppeling en geeft het verzoek terug dat de API bereikte. */
    private fun aanroep(koppeling: Koppeling): ClientRequest {
        roep(koppeling)
        return verzoeken.last { it.url().host == "api.example.nl" }
    }

    private fun tokenVerzoeken() = verzoeken.filter { it.url().toString() == tokenUrl }

    private fun lichaam(verzoek: ClientRequest): String {
        val mock = MockClientHttpRequest(verzoek.method(), verzoek.url())
        verzoek.writeTo(mock, ExchangeStrategies.withDefaults()).block()
        return mock.bodyAsString.block()!!
    }

    @Test
    fun `api-key wordt als header met de gekozen naam meegestuurd`() {
        val verzoek =
            aanroep(
                koppeling {
                    auth.type = KoppelingAuthType.API_KEY
                    auth.headerName = "X-Api-Key"
                    auth.value = "geheime-waarde"
                },
            )

        assertEquals("geheime-waarde", verzoek.headers().getFirst("X-Api-Key"))
        assertEquals("https://api.example.nl/ping", verzoek.url().toString())
    }

    @Test
    fun `een vast bearer-token wordt als Authorization-header meegestuurd`() {
        val verzoek =
            aanroep(
                koppeling {
                    auth.type = KoppelingAuthType.BEARER
                    auth.token = "vast-token"
                },
            )

        assertEquals("Bearer vast-token", verzoek.headers().getFirst("Authorization"))
    }

    @Test
    fun `zgw-jwt tekent een token met client-id en secret van de koppeling`() {
        val verzoek =
            aanroep(
                koppeling {
                    auth.type = KoppelingAuthType.ZGW_JWT
                    auth.clientId = "nl-portal"
                    auth.secret = "NYy.TAQYbHRRWC_b2rfoqs4oR9jXj38N"
                },
            )

        val token = SignedJWT.parse(verzoek.headers().getFirst("Authorization")!!.removePrefix("Bearer "))
        assertTrue(token.verify(MACVerifier("NYy.TAQYbHRRWC_b2rfoqs4oR9jXj38N")))
        assertEquals("nl-portal", token.jwtClaimsSet.getStringClaim("client_id"))
    }

    @Test
    fun `client credentials haalt een token met client-id en secret en hergebruikt het tot het verloopt`() {
        val koppeling =
            koppeling {
                auth.type = KoppelingAuthType.OAUTH2_CLIENT_CREDENTIALS
                auth.tokenUrl = tokenUrl
                auth.clientId = "nl-portal"
                auth.secret = "geheim"
            }
        val client = factory.webClient("testmodule", koppeling)

        roep(koppeling, client)
        roep(koppeling, client)

        val aanroepen = verzoeken.filter { it.url().host == "api.example.nl" }
        assertEquals(2, aanroepen.size)
        assertEquals("Bearer uitgegeven-token", aanroepen.last().headers().getFirst("Authorization"))
        assertEquals(1, tokenVerzoeken().size)
        val tokenVerzoek = tokenVerzoeken().single()
        val verwachtBasic = "Basic " + Base64.getEncoder().encodeToString("nl-portal:geheim".toByteArray())
        assertEquals(verwachtBasic, tokenVerzoek.headers().getFirst("Authorization"))
        assertEquals("grant_type=client_credentials", lichaam(tokenVerzoek))
    }

    @Test
    fun `een verlopen token wordt opnieuw opgehaald`() {
        tokenAntwoord = """{"access_token":"uitgegeven-token","expires_in":1}"""
        val koppeling = clientCredentials()
        val client = factory.webClient("testmodule", koppeling)

        roep(koppeling, client)
        roep(koppeling, client)

        assertEquals(2, tokenVerzoeken().size)
    }

    @Test
    fun `zonder expires_in in het antwoord wordt het token kort hergebruikt`() {
        tokenAntwoord = """{"access_token":"uitgegeven-token"}"""
        val koppeling = clientCredentials()
        val client = factory.webClient("testmodule", koppeling)

        roep(koppeling, client)
        roep(koppeling, client)

        assertEquals(1, tokenVerzoeken().size)
    }

    @Test
    fun `private_key_jwt bewijst de identiteit met een gesigneerde assertion uit het sleutelbestand`(
        @TempDir map: Path,
    ) {
        val sleutelpaar = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val pem =
            "-----BEGIN PRIVATE KEY-----\n" +
                Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(sleutelpaar.private.encoded) +
                "\n-----END PRIVATE KEY-----\n"
        val sleutelbestand = map.resolve("sleutel.pem").also { it.toFile().writeText(pem) }

        val verzoek =
            aanroep(
                koppeling {
                    auth.type = KoppelingAuthType.OAUTH2_PRIVATE_KEY_JWT
                    auth.tokenUrl = tokenUrl
                    auth.clientId = "nl-portal"
                    auth.privateKeyFile = sleutelbestand.toString()
                },
            )

        assertEquals("Bearer uitgegeven-token", verzoek.headers().getFirst("Authorization"))
        val formulier = lichaam(tokenVerzoeken().single())
        assertTrue(formulier.contains("grant_type=client_credentials"))
        assertTrue(formulier.contains("client_assertion_type=urn%3Aietf%3Aparams%3Aoauth%3Aclient-assertion-type%3Ajwt-bearer"))
        val assertion = formulier.split("&").first { it.startsWith("client_assertion=") }.removePrefix("client_assertion=")
        val jwt = SignedJWT.parse(assertion)
        assertTrue(jwt.verify(RSASSAVerifier(sleutelpaar.public as RSAPublicKey)))
        assertEquals("nl-portal", jwt.jwtClaimsSet.issuer)
        assertEquals("nl-portal", jwt.jwtClaimsSet.subject)
        assertEquals(listOf(tokenUrl), jwt.jwtClaimsSet.audience)
        assertNotNull(jwt.jwtClaimsSet.jwtid)
        val geldigheid = jwt.jwtClaimsSet.expirationTime.time - jwt.jwtClaimsSet.issueTime.time
        assertTrue(geldigheid in 59_000..61_000, "geldigheid $geldigheid ms")
    }
}