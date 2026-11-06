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

import dev.openfeature.sdk.EvaluationContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

class FeatureContextFactoryTest {
    private val factory =
        FeatureContextFactory(
            FeatureFlagProperties.Context().apply {
                tenant = "gemeente-a"
                environment = "test"
                userIdSecret = "tenant-a-secret"
            },
        )

    /** A citizen's token as NL Portal receives it: the subject plus BSN, KVK number and name claims. */
    private class CitizenAuthentication(
        jwt: Jwt,
    ) : JwtAuthenticationToken(jwt, listOf(SimpleGrantedAuthority("SCOPE_openid"))),
        UserTypeAware {
        override val userType = "bsn"
    }

    private val citizen =
        CitizenAuthentication(
            Jwt
                .withTokenValue("token")
                .header("alg", "none")
                .subject(SUBJECT)
                .claim("aanvrager", mapOf("bsn" to BSN))
                .claim("bsn", BSN)
                .claim("kvk", KVK)
                .claim("name", NAME)
                .claim("given_name", "Jan")
                .claim("family_name", "Jansen")
                .build(),
        )

    @Test
    fun `an authenticated request carries tenant, environment and user type`() {
        val context = factory.create(citizen)

        assertEquals("gemeente-a", context.getValue("tenant").asString())
        assertEquals("test", context.getValue("environment").asString())
        assertEquals("bsn", context.getValue("userType").asString())
    }

    @Test
    fun `the user id is a keyed HMAC of the identity provider subject, not a plain hash`() {
        val context = factory.create(citizen)

        // HMAC-SHA256("tenant-a-secret", SUBJECT), computed independently with openssl.
        assertEquals("65de9070206620f654db1780eeb04a2d7cc1d3273e8a397b7e546d828e3915ec", context.targetingKey)
        // SHA-256(SUBJECT): a plain hash can be reversed by brute force over the subject space.
        assertFalse(context.targetingKey == "db3855a227f2aa115bb78a8818d433bb00815539f05e386e512977355137f6ec")
    }

    @Test
    fun `BSN, KVK number and name never reach the context`() {
        val values = allValues(factory.create(citizen))

        listOf(BSN, KVK, NAME, "Jan", "Jansen", SUBJECT).forEach { personalData ->
            assertFalse(values.any { it.contains(personalData) }, "'$personalData' found in evaluation context: $values")
        }
    }

    @Test
    fun `without a configured secret the context holds no user id`() {
        val context = FeatureContextFactory(FeatureFlagProperties.Context().apply { tenant = "gemeente-a" }).create(citizen)

        assertNull(context.targetingKey)
        assertEquals("gemeente-a", context.getValue("tenant").asString())
    }

    @Test
    fun `a blank secret counts as no secret`() {
        val context = FeatureContextFactory(FeatureFlagProperties.Context().apply { userIdSecret = "  " }).create(citizen)

        assertNull(context.targetingKey)
    }

    @Test
    fun `an anonymous or unauthenticated authentication yields no user attributes`() {
        val anonymous = AnonymousAuthenticationToken("key", "anonymousUser", listOf(SimpleGrantedAuthority("ROLE_ANONYMOUS")))
        val unauthenticated = CitizenAuthentication(citizen.token).apply { isAuthenticated = false }

        listOf(anonymous, unauthenticated).forEach {
            val context = factory.create(it)
            assertNull(context.targetingKey)
            assertNull(context.getValue("userType"))
        }
    }

    @Test
    fun `an authentication without a token yields no user id`() {
        val withoutToken = UsernamePasswordAuthenticationToken.authenticated("c0ffee00", null, emptyList())

        assertNull(factory.create(withoutToken).targetingKey)
    }

    @Test
    fun `an anonymous request carries tenant and environment only`() {
        val context = factory.create(null)

        assertNull(context.targetingKey)
        assertNull(context.getValue("userType"))
        assertEquals("gemeente-a", context.getValue("tenant").asString())
    }

    @Test
    fun `the current context follows the reactive security context across threads`() {
        val context =
            factory
                .current()
                .publishOn(Schedulers.boundedElastic())
                .flatMap { first -> factory.current().map { second -> first to second } }
                .subscribeOn(Schedulers.parallel())
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(citizen))
                .block()!!

        assertEquals("bsn", context.first.getValue("userType").asString())
        assertEquals(context.first.targetingKey, context.second.targetingKey)
    }

    @Test
    fun `the current context is anonymous outside an authenticated request`() {
        val context = Mono.defer { factory.current() }.block()!!

        assertNull(context.targetingKey)
        assertEquals("gemeente-a", context.getValue("tenant").asString())
    }

    private fun allValues(context: EvaluationContext): List<String> = listOfNotNull(context.targetingKey) + context.asObjectMap().values.map { it.toString() }

    private companion object {
        const val SUBJECT = "c0ffee00-0000-4000-8000-000000000001"
        const val BSN = "999990755"
        const val KVK = "69599084"
        const val NAME = "Jan Jansen"
    }
}