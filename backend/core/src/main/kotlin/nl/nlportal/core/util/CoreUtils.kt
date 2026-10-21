/*
 * Copyright 2015-2024 Den Haag, Ritense, the Netherlands.
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

import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import org.apache.commons.codec.digest.DigestUtils

object CoreUtils {
    @JvmStatic
    fun extractId(url: String): UUID = UUID.fromString(url.substringAfterLast("/"))

    // shaVersion is kept for source/binary compatibility with existing callers and published
    // consumers; every version now resolves to SHA-512, so the value itself no longer matters.
    @JvmStatic
    fun createHash(
        input: String,
        shaVersion: String,
    ): String = DigestUtils.sha512Hex(input)

    @JvmStatic
    fun generateNonce(byteLength: Int = 16): String {
        val randomBytes = ByteArray(byteLength)
        SecureRandom().nextBytes(randomBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes)
    }
}