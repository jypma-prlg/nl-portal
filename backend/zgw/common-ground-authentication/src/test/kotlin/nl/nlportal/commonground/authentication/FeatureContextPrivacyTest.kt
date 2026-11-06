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
package nl.nlportal.commonground.authentication

import dev.openfeature.sdk.EvaluationContext
import java.util.UUID
import nl.nlportal.core.featureflag.FeatureContextFactory
import nl.nlportal.core.featureflag.FeatureFlagProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

/** The evaluation context built from the real citizen and company authentications holds no BSN or KVK number. */
class FeatureContextPrivacyTest {
    private val factory =
        FeatureContextFactory(
            FeatureFlagProperties.Context().apply {
                tenant = "gemeente-a"
                userIdSecret = "tenant-a-secret"
            },
        )

    @Test
    fun `a citizen's BSN never reaches the evaluation context`() {
        val authentication = JwtBuilder().aanvragerBsn(BSN).gemachtigdeBsn(GEMACHTIGDE_BSN).buildBurgerAuthentication()

        val context = factory.create(authentication)

        assertEquals("bsn", context.getValue(FeatureContextFactory.USER_TYPE).asString())
        assertNoneOf(context, BSN, GEMACHTIGDE_BSN)
    }

    @Test
    fun `a company's KVK number never reaches the evaluation context`() {
        val authentication = JwtBuilder().aanvragerKvk(KVK).gemachtigdeKvk(GEMACHTIGDE_KVK).buildBedrijfAuthentication()

        val context = factory.create(authentication)

        assertEquals("kvk", context.getValue(FeatureContextFactory.USER_TYPE).asString())
        assertNoneOf(context, KVK, GEMACHTIGDE_KVK)
    }

    @Test
    fun `a Keycloak user's subject is only present as HMAC`() {
        val subject = UUID.fromString("c0ffee00-0000-4000-8000-000000000001")
        val context = factory.create(JwtBuilder().aanvragerUid(subject).buildKeycloakUserAuthentication())

        // HMAC-SHA256("tenant-a-secret", subject), computed independently with openssl.
        assertEquals("65de9070206620f654db1780eeb04a2d7cc1d3273e8a397b7e546d828e3915ec", context.targetingKey)
        assertNoneOf(context, subject.toString(), subject.toString().substring(0, 13))
    }

    private fun assertNoneOf(
        context: EvaluationContext,
        vararg personalData: String,
    ) {
        val values = listOfNotNull(context.targetingKey) + context.asObjectMap().values.map { it.toString() }
        personalData.forEach { data ->
            assertFalse(values.any { it.contains(data) }, "'$data' found in evaluation context: $values")
        }
    }

    private companion object {
        const val BSN = "999990755"
        const val GEMACHTIGDE_BSN = "999993653"
        const val KVK = "69599084"
        const val GEMACHTIGDE_KVK = "68750110"
    }
}