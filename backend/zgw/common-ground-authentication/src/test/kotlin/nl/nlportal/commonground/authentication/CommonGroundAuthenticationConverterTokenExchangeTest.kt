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
package nl.nlportal.commonground.authentication

import nl.nlportal.commonground.authentication.KeycloakConfig.KeycloakCredentials
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterEach
import nl.nlportal.commonground.authentication.exception.TokenExchangeException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

internal class CommonGroundAuthenticationConverterTokenExchangeTest {
    private lateinit var mockServer: MockWebServer
    private lateinit var decoder: ReactiveJwtDecoder

    @BeforeEach
    fun setUp() {
        mockServer = MockWebServer()
        mockServer.start()
        decoder = mock()
    }

    @AfterEach
    fun tearDown() {
        mockServer.shutdown()
    }

    @Test
    fun `sends subject_token_type and omits the audience when it is not configured`() {
        val converter = converter(audience = "")
        mockServer.enqueue(tokenResponse())

        converter.tokenExchange(jwt()).block()

        val formData = mockServer.takeRequest().formData()
        assertEquals("urn:ietf:params:oauth:token-type:access_token", formData["subject_token_type"])
        assertEquals("urn:ietf:params:oauth:grant-type:token-exchange", formData["grant_type"])
        assertNull(formData["audience"])
    }

    @Test
    fun `sends the audience when one is configured`() {
        val converter = converter(audience = "some-other-client")
        mockServer.enqueue(tokenResponse())

        converter.tokenExchange(jwt()).block()

        val formData = mockServer.takeRequest().formData()
        assertEquals("urn:ietf:params:oauth:token-type:access_token", formData["subject_token_type"])
        assertEquals("some-other-client", formData["audience"])
    }

    @Test
    fun `omits a blank audience`() {
        val converter = converter(audience = " ")
        mockServer.enqueue(tokenResponse())

        converter.tokenExchange(jwt()).block()

        assertNull(mockServer.takeRequest().formData()["audience"])
    }

    @Test
    fun `an error response from keycloak fails without exposing any keycloak detail`() {
        val converter = converter(audience = "stale-audience")
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error":"invalid_request","error_description":"Requested audience not available"}"""),
        )

        val exception =
            assertThrows(TokenExchangeException::class.java) {
                converter.tokenExchange(jwt()).block()
            }

        assertEquals(1, mockServer.requestCount)
        assertEquals("Token exchange failed", exception.message)

        val rendered = "${exception.message} ${exception.cause?.message.orEmpty()}"
        listOf(
            "invalid_request",
            "Requested audience not available",
            "stale-audience",
            mockServer.hostName,
            mockServer.port.toString(),
            "400",
        ).forEach { detail ->
            assertFalse(
                rendered.contains(detail),
                "the exception must not carry '$detail' towards the end user, got '$rendered'",
            )
        }
    }

    private fun converter(audience: String): CommonGroundAuthenticationConverter =
        CommonGroundAuthenticationConverter(
            decoder,
            KeycloakConfig(
                resource = "nl-portal-m2m",
                audience = audience,
                credentials = KeycloakCredentials("secret"),
            ),
        )

    private fun jwt(): Jwt =
        Jwt
            .withTokenValue("token")
            .header("alg", "none")
            .issuer("http://${mockServer.hostName}:${mockServer.port}/auth/realms/nlportal")
            .claim("sub", "a-subject")
            .build()

    private fun tokenResponse(): MockResponse =
        MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody("""{"access_token":"exchanged-token"}""")

    private fun RecordedRequest.formPairs(): List<Pair<String, String>> =
        body
            .readUtf8()
            .split("&")
            .filter { it.isNotEmpty() }
            .map { parameter ->
                val (name, value) = parameter.split("=", limit = 2)
                URLDecoder.decode(name, StandardCharsets.UTF_8) to URLDecoder.decode(value, StandardCharsets.UTF_8)
            }

    private fun RecordedRequest.formData(): Map<String, String> = formPairs().toMap()
}