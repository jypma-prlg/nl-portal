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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ModuleConfigurationTest {
    @Test
    fun `returns the value when it is configured`() {
        assertEquals("secret", "secret".requireConfigured("openklant", "token"))
    }

    @Test
    fun `names module and setting when the value is missing`() {
        val ex = assertThrows<ModuleNotConfiguredException> { (null as String?).requireConfigured("openklant", "token") }

        assertTrue(ex.message!!.contains("openklant"))
        assertTrue(ex.message!!.contains("token"))
    }

    @Test
    fun `treats a blank string as missing`() {
        assertThrows<ModuleNotConfiguredException> { "  ".requireConfigured("openklant", "token") }
    }
}