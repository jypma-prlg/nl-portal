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
package nl.nlportal.app.security

import nl.nlportal.app.TestApplication
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.test.web.reactive.server.WebTestClient

/**
 * Zonder CORS-configuratie staat :app geen cross-origin verzoeken toe. Origins worden
 * expliciet per omgeving ingesteld via nl-portal.security.cors (zie site/hosting-en-configuratie).
 */
@SpringBootTest(classes = [TestApplication::class])
@AutoConfigureWebTestClient(timeout = "30000")
@Tag("integration")
class CorsDefaultIT {
    @Autowired
    lateinit var webTestClient: WebTestClient

    @Test
    fun `preflight van een vreemde origin wordt niet toegestaan`() {
        webTestClient
            // Een base URL is nodig: zonder host beoordeelt Spring de Origin als "malformed".
            .mutate()
            .baseUrl("http://localhost")
            .build()
            .method(HttpMethod.OPTIONS)
            .uri("/graphql")
            .header(HttpHeaders.ORIGIN, "https://evil.example")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
            .exchange()
            .expectHeader()
            .doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)
    }

    @Test
    fun `verzoek met een vreemde origin krijgt geen CORS-headers`() {
        webTestClient
            // Een base URL is nodig: zonder host beoordeelt Spring de Origin als "malformed".
            .mutate()
            .baseUrl("http://localhost")
            .build()
            .post()
            .uri("/graphql")
            .header(HttpHeaders.ORIGIN, "https://evil.example")
            .exchange()
            .expectHeader()
            .doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)
    }
}