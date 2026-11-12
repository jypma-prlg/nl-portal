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

import nl.nlportal.core.modulegate.ModuleFlag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(
    properties = [
        "management.endpoints.web.exposure.include=modulestatus",
        "management.endpoints.access.default=read_only",
        "nl-portal.beheer.gebruikersnaam=beheer",
        "nl-portal.beheer.wachtwoord-hash={noop}geheim",
    ],
)
@AutoConfigureWebTestClient
@Import(ModuleStatusEndpointTest.VoorbeeldModule::class)
class ModuleStatusEndpointTest(
    @Autowired private val client: WebTestClient,
    @Autowired private val registry: ModuleStatusRegistry,
) {
    @TestConfiguration
    class VoorbeeldModule {
        @Bean
        fun voorbeeldModuleFlag() = ModuleFlag("voorbeeld")
    }

    @Test
    fun `zonder inloggegevens is het endpoint niet toegankelijk`() {
        client
            .get()
            .uri("/actuator/modulestatus")
            .exchange()
            .expectStatus()
            .isUnauthorized
    }

    @Test
    fun `met verkeerd wachtwoord is het endpoint niet toegankelijk`() {
        client
            .get()
            .uri("/actuator/modulestatus")
            .headers { it.setBasicAuth("beheer", "fout") }
            .exchange()
            .expectStatus()
            .isUnauthorized
    }

    @Test
    fun `beheerder ziet reden en laatste fout zonder foutdetails`() {
        registry.recordFailure("zaken", "zakenapi", Oorzaak.UNREACHABLE)

        client
            .get()
            .uri("/actuator/modulestatus")
            .headers { it.setBasicAuth("beheer", "geheim") }
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$[?(@.module=='zaken')].status")
            .isEqualTo("DEGRADED")
            .jsonPath("$[?(@.module=='zaken')].apis[0].laatsteFout.oorzaak")
            .isEqualTo("UNREACHABLE")
    }

    @Test
    fun `modules uit de configuratie staan erin als DISABLED of UNKNOWN`() {
        client
            .get()
            .uri("/actuator/modulestatus")
            .headers { it.setBasicAuth("beheer", "geheim") }
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$[?(@.module=='besluiten')].status")
            .isEqualTo("DISABLED")
            .jsonPath("$[?(@.module=='taak')].status")
            .isEqualTo("UNKNOWN")
    }

    @Test
    fun `een module met een module-flag staat erin als DISABLED zolang de flag uit staat`() {
        client
            .get()
            .uri("/actuator/modulestatus")
            .headers { it.setBasicAuth("beheer", "geheim") }
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$[?(@.module=='voorbeeld')].status")
            .isEqualTo("DISABLED")
    }
}