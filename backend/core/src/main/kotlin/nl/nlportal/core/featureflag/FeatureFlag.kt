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

/**
 * A boolean feature flag. [default] is what the application falls back to when the flag cannot be evaluated;
 * a [ToggleType.PERMISSION] flag is fail-closed and can therefore never default to enabled.
 */
data class FeatureFlag(
    val key: String,
    val type: ToggleType,
    val default: Boolean = false,
) {
    init {
        require(type != ToggleType.PERMISSION || !default) { "Permission flag '$key' must default to disabled" }
    }
}