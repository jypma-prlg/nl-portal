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
package nl.nlportal.core.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ZgwUrlsTest {
    @Test
    fun `extractId parses the UUID off the end of a ZGW self-URL`() {
        val result = ZgwUrls.extractId("https://zaken-api.example.com/api/v1/zaken/3a1b2c3d-4e5f-6789-abcd-ef0123456789")

        assertEquals("3a1b2c3d-4e5f-6789-abcd-ef0123456789", result.toString())
    }
}