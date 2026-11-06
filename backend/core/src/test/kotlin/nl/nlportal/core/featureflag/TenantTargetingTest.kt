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
package nl.nlportal.core.featureflag

import nl.nlportal.core.util.Mapper
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.test.web.reactive.server.WebTestClient

/**
 * A flagd targeting rule on tenant, evaluated end to end through the bundled flagd file provider.
 *
 * Note: OpenFeature's API is a JVM-wide singleton, so this context replaces the provider for other cached
 * contexts too. The flag file is chosen so that it yields the bundled defaults for any other tenant.
 */
@SpringBootTest(
    properties = [
        "nl-portal.feature-flags.file=src/test/resources/feature-flags/tenant-targeting.flagd.json",
        "nl-portal.feature-flags.context.tenant=gemeente-a",
    ],
)
@AutoConfigureWebTestClient
class TenantTargetingTest(
    @Autowired private val webTestClient: WebTestClient,
    @Autowired private val featureGate: FeatureGate,
) {
    @Test
    fun `the frontend toggle follows the targeting rule for the configured tenant`() {
        val body =
            webTestClient
                .get()
                .uri("/api/public/frontend")
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .returnResult()
                .responseBody

        assertTrue(
            Mapper
                .get()
                .readTree(body)
                .requiredAt("/toggles/messageCountEnabled")
                .booleanValue(),
        )
    }

    @Test
    fun `another tenant does not match the targeting rule`() {
        val otherTenant =
            FeatureContextFactory(FeatureFlagProperties.Context().apply { tenant = "gemeente-b" }).create(null)

        assertFalse(featureGate.isEnabled(FeatureFlag("ops.frontend.message-count", ToggleType.OPS), otherTenant))
    }
}