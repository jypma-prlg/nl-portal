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

import com.nimbusds.jwt.SignedJWT
import org.junit.jupiter.api.Assertions.assertEquals
import nl.nlportal.core.koppeling.ZgwJwtTestHelper.TEST_CLIENT_ID
import nl.nlportal.core.koppeling.ZgwJwtTestHelper.TEST_ENCRYPTION_SECRET_INVALID
import nl.nlportal.core.koppeling.ZgwJwtTestHelper.TEST_ENCRYPTION_SECRET_VALID
import nl.nlportal.core.koppeling.ZgwJwtTestHelper.TEST_USER_ID
import nl.nlportal.core.koppeling.ZgwJwtTestHelper.TEST_USER_REPRESENTATION
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ZgwJwtGeneratorTest {
    private val zgwJwtGenerator = ZgwJwtGenerator()
    private lateinit var testClientId: String
    private lateinit var testUserId: String
    private lateinit var testUserRepresentation: Any

    @BeforeEach
    fun prepareTest() {
        testClientId = TEST_CLIENT_ID
        testUserId = TEST_USER_ID
        testUserRepresentation = TEST_USER_REPRESENTATION
    }

    @Test
    fun `should generate token with default user information`() {
        val testSecretKey = TEST_ENCRYPTION_SECRET_VALID

        val generatedToken =
            zgwJwtGenerator.generateToken(
                testSecretKey,
                testClientId,
            )

        val signedJWT = SignedJWT.parse(generatedToken)
        val claimsSet = signedJWT.getJWTClaimsSet()

        Assertions.assertThat(claimsSet.issuer).isEqualTo(testClientId)
        Assertions.assertThat(claimsSet.claims.get("client_id")).isEqualTo(testClientId)
        Assertions.assertThat(claimsSet.claims.get("user_id")).isEqualTo("nl-portal")
        Assertions.assertThat(claimsSet.claims.get("user_representation")).isEqualTo("nl-portal")
    }

    @Test
    fun `should generate token with custom user information`() {
        val testSecretKey = TEST_ENCRYPTION_SECRET_VALID

        val generatedToken =
            zgwJwtGenerator.generateToken(
                testSecretKey,
                testClientId,
                testUserId,
                testUserRepresentation,
            )

        val signedJWT = SignedJWT.parse(generatedToken)
        val claimsSet = signedJWT.getJWTClaimsSet()

        Assertions.assertThat(claimsSet.issuer).isEqualTo(testClientId)
        Assertions.assertThat(claimsSet.claims.get("client_id")).isEqualTo(testClientId)
        Assertions.assertThat(claimsSet.claims.get("user_id")).isEqualTo(testUserId)
    }

    @Test
    fun `tokens verlopen na vijftien minuten, ook met gebruikersinformatie`() {
        val zonderGebruiker = zgwJwtGenerator.generateToken(TEST_ENCRYPTION_SECRET_VALID, testClientId)
        val metGebruiker = zgwJwtGenerator.generateToken(TEST_ENCRYPTION_SECRET_VALID, testClientId, testUserId, testUserRepresentation)

        listOf(zonderGebruiker, metGebruiker).forEach {
            val claims = SignedJWT.parse(it).jwtClaimsSet
            val geldigheid = claims.expirationTime.time - claims.issueTime.time
            assertTrue(geldigheid in 899_000..901_000, "geldigheid $geldigheid ms")
        }
    }

    @Test
    fun `should throw error when secret is too short`() {
        val testSecretKey = TEST_ENCRYPTION_SECRET_INVALID

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                zgwJwtGenerator.generateToken(
                    testSecretKey,
                    testClientId,
                )
            }

        assertEquals("SecretKey needs to be at least 32 in length", exception.message)
    }
}