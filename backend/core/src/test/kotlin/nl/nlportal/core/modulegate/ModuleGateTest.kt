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
package nl.nlportal.core.modulegate

import dev.openfeature.sdk.ImmutableContext
import dev.openfeature.sdk.OpenFeatureAPI
import dev.openfeature.sdk.providers.memory.Flag
import dev.openfeature.sdk.providers.memory.InMemoryProvider
import nl.nlportal.core.featureflag.FeatureContextFactory
import nl.nlportal.core.featureflag.FeatureFlagProperties
import nl.nlportal.core.featureflag.OpenFeatureFeatureGate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class ModuleGateTest {
    private val berichten = ModuleFlag("berichten")
    private val taak = ModuleFlag("taak")

    private fun gateWith(flags: Map<String, Boolean>): ModuleGate {
        val api = OpenFeatureAPI.getInstance()
        api.setProviderAndWait(
            InMemoryProvider(
                flags.mapValues { (_, enabled) ->
                    Flag
                        .builder<Boolean>()
                        .variant("v", enabled)
                        .defaultVariant("v")
                        .build()
                },
            ),
        )
        return ModuleGate(
            featureGate = OpenFeatureFeatureGate(api.client),
            featureContextFactory = FeatureContextFactory(FeatureFlagProperties.Context()),
            modules = listOf(berichten, taak),
        )
    }

    @Test
    fun `a module flag is an ops flag named after the module`() {
        assertEquals("ops.berichten.enabled", berichten.flag.key)
    }

    @Test
    fun `a module is enabled when its flag is on`() {
        val gate = gateWith(mapOf("ops.berichten.enabled" to true))

        assertTrue(gate.isEnabled(berichten, ImmutableContext()))
        assertDoesNotThrow { gate.require(berichten, null) }
    }

    @Test
    fun `a module is disabled when its flag is off or unknown`() {
        val gate = gateWith(mapOf("ops.berichten.enabled" to false))

        assertFalse(gate.isEnabled(berichten, ImmutableContext()))
        assertFalse(gate.isEnabled(taak, ImmutableContext()))
    }

    @Test
    fun `requiring a disabled module throws a not-available error naming the module`() {
        val gate = gateWith(mapOf("ops.berichten.enabled" to false))

        val error = assertThrows<ModuleNotAvailableException> { gate.require(berichten, null) }

        assertEquals("berichten", error.module)
        assertEquals("Module 'berichten' is niet beschikbaar", error.message)
    }

    @Test
    fun `the status lists every registered module with its flag outcome`() {
        val gate = gateWith(mapOf("ops.berichten.enabled" to true, "ops.taak.enabled" to false))

        assertEquals(mapOf("berichten" to true, "taak" to false), gate.status(ImmutableContext()))
    }
}