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
package nl.nlportal.core.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CoreUtilsTest {
    @Test
    fun `createHash upgrades the configured SHA1 version to a SHA-512 digest`() {
        val result = CoreUtils.createHash("abc", ShaVersion.SHA1.version)

        assertEquals(
            "ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f",
            result,
        )
    }

    @Test
    fun `createHash upgrades any unrecognized version to a SHA-512 digest`() {
        val result = CoreUtils.createHash("hello-world", "bogus-algorithm")

        assertEquals(
            "6aeefc29122a3962c90ef834f6caad0033bffcd62941b7a6205a695cc39e2767db7778a7ad76d173a083b9e14b210dc0212923f481b285c784ab1fe340d7ff4d",
            result,
        )
    }
}