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
package nl.nlportal.core.modulestatus

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalManagementPort
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "management.server.port=0",
        "management.endpoints.web.exposure.include=health,modulestatus",
        "management.endpoints.access.default=read_only",
        "nl-portal.beheer.gebruikersnaam=beheer",
        "nl-portal.beheer.wachtwoord-hash={noop}geheim",
    ],
)
class ModuleStatusManagementPortTest(
    @LocalServerPort private val serverPort: Int,
    @LocalManagementPort private val managementPort: Int,
) {
    private fun client(port: Int) = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

    @Test
    fun `op de beheerpoort vraagt modulestatus om inloggegevens`() {
        client(managementPort)
            .get()
            .uri("/actuator/modulestatus")
            .exchange()
            .expectStatus()
            .isUnauthorized
    }

    @Test
    fun `op de beheerpoort werkt modulestatus met inloggegevens`() {
        client(managementPort)
            .get()
            .uri("/actuator/modulestatus")
            .headers { it.setBasicAuth("beheer", "geheim") }
            .exchange()
            .expectStatus()
            .isOk
    }

    @Test
    fun `op de publieke poort bestaat modulestatus niet`() {
        client(serverPort)
            .get()
            .uri("/actuator/modulestatus")
            .headers { it.setBasicAuth("beheer", "geheim") }
            .exchange()
            .expectStatus()
            .isNotFound
    }
}