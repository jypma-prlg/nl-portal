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

import dev.openfeature.sdk.Client
import dev.openfeature.sdk.EvaluationContext
import dev.openfeature.sdk.ImmutableStructure
import dev.openfeature.sdk.Value

class OpenFeatureFeatureGate(
    private val client: Client,
) : FeatureGate {
    override fun isEnabled(
        flag: FeatureFlag,
        context: EvaluationContext,
    ): Boolean = client.getBooleanValue(flag.key, flag.default, context)

    override fun booleanMap(
        key: String,
        context: EvaluationContext,
    ): Map<String, Boolean> =
        client
            .getObjectValue(key, Value(ImmutableStructure()), context)
            .asStructure()
            ?.asMap()
            .orEmpty()
            .mapNotNull { (name, value) -> value.asBoolean()?.let { name to it } }
            .toMap()
}