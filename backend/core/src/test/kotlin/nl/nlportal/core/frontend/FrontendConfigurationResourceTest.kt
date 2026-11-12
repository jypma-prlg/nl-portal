/*
 * Copyright 2015-2025 Den Haag, Ritense, the Netherlands.
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
package nl.nlportal.core.frontend

import java.nio.charset.Charset
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties
import nl.nlportal.core.frontend.configuration.FrontendModuleConfigurationProperties
import nl.nlportal.core.util.Mapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest
@AutoConfigureWebTestClient(timeout = "36000")
@TestInstance(Lifecycle.PER_CLASS)
class FrontendConfigurationResourceTest(
    @Autowired private val webTestClient: WebTestClient,
    @Autowired private val frontendConfigurationProperties: FrontendConfigurationProperties,
    @Autowired private val frontendModuleConfigurationProperties: FrontendModuleConfigurationProperties,
) {
    @Test
    fun `get frontend configuration`() {
        val responseBodyContent =
            webTestClient
                .get()
                .uri("/api/public/frontend")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .returnResult()
                .responseBody

        val responseJson = Mapper.get().readTree(responseBodyContent)

        assertEquals(frontendConfigurationProperties.properties.myAddressResearchUrl, responseJson.requiredAt("/properties/myAddressResearchUrl").stringValue())
        assertEquals(
            frontendConfigurationProperties.properties.overviewCurrentCasesPreviewLength,
            responseJson.requiredAt("/properties/overviewCurrentCasesPreviewLength").intValue(),
        )
        assertEquals(
            false,
            responseJson.requiredAt("/toggles/casesContactMomentsEnabled").booleanValue(),
            "toggles come from the bundled flag file when no flag file is configured",
        )
        assertEquals(
            false,
            responseJson.requiredAt("/toggles/messageCountEnabled").booleanValue(),
            "message count comes from the bundled flag file when no flag file is configured",
        )
        assertEquals(
            true,
            responseJson.requiredAt("/modules/taak/enabled").booleanValue(),
        )
        assertEquals(
            false,
            responseJson.requiredAt("/modules/besluiten/enabled").booleanValue(),
        )
    }
}