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
package nl.nlportal.core.featureflag

import dev.openfeature.sdk.EvaluationContext
import dev.openfeature.sdk.OpenFeatureAPI
import dev.openfeature.sdk.ProviderEvaluation
import dev.openfeature.sdk.providers.memory.Flag
import dev.openfeature.sdk.providers.memory.InMemoryProvider
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FeatureGateTest {
    private fun gateWith(flags: Map<String, Flag<*>>): FeatureGate {
        val api = OpenFeatureAPI.getInstance()
        api.setProviderAndWait(InMemoryProvider(flags))
        return OpenFeatureFeatureGate(api.client)
    }

    private fun booleanFlag(value: Boolean): Flag<Boolean> =
        Flag
            .builder<Boolean>()
            .variant("variant", value)
            .defaultVariant("variant")
            .build()

    @Test
    fun `a failing provider yields the default, and denies a permission flag`() {
        val failingProvider =
            object : InMemoryProvider(emptyMap()) {
                override fun getBooleanEvaluation(
                    key: String,
                    defaultValue: Boolean,
                    ctx: EvaluationContext,
                ): ProviderEvaluation<Boolean> = throw IllegalStateException("provider failure")
            }
        val api = OpenFeatureAPI.getInstance()
        api.setProviderAndWait(failingProvider)
        val gate = OpenFeatureFeatureGate(api.client)

        assertTrue(gate.isEnabled(FeatureFlag("ops.frontend.example", ToggleType.OPS, default = true)))
        assertFalse(gate.isEnabled(FeatureFlag("permission.frontend.example", ToggleType.PERMISSION)))
    }

    @Test
    fun `an unknown release or experiment flag falls back to its default`() {
        val gate = gateWith(emptyMap())

        assertTrue(gate.isEnabled(FeatureFlag("release.frontend.unknown", ToggleType.RELEASE, default = true)))
        assertTrue(gate.isEnabled(FeatureFlag("experiment.frontend.unknown", ToggleType.EXPERIMENT, default = true)))
    }

    @Test
    fun `an enabled flag evaluates to true`() {
        val gate = gateWith(mapOf("ops.frontend.example" to booleanFlag(true)))

        assertTrue(gate.isEnabled(FeatureFlag("ops.frontend.example", ToggleType.OPS)))
    }

    @Test
    fun `a disabled flag evaluates to false`() {
        val gate = gateWith(mapOf("ops.frontend.example" to booleanFlag(false)))

        assertFalse(gate.isEnabled(FeatureFlag("ops.frontend.example", ToggleType.OPS, default = true)))
    }

    @Test
    fun `an unknown ops flag falls back to its default`() {
        val gate = gateWith(emptyMap())

        assertTrue(gate.isEnabled(FeatureFlag("ops.frontend.unknown", ToggleType.OPS, default = true)))
        assertFalse(gate.isEnabled(FeatureFlag("ops.frontend.unknown", ToggleType.OPS, default = false)))
    }

    @Test
    fun `an unknown permission flag is denied`() {
        val gate = gateWith(emptyMap())

        assertFalse(gate.isEnabled(FeatureFlag("permission.frontend.unknown", ToggleType.PERMISSION)))
    }

    @Test
    fun `a permission flag cannot default to enabled`() {
        assertThrows(IllegalArgumentException::class.java) {
            FeatureFlag("permission.frontend.example", ToggleType.PERMISSION, default = true)
        }
    }
}