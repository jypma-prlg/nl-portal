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

/**
 * Thrown on first use of a module whose required configuration is missing. Module beans are
 * registered without configuration, so validation happens here instead of at startup.
 */
class ModuleNotConfiguredException(
    module: String,
    setting: String,
) : IllegalStateException("Module '$module' is not configured: setting '$setting' is missing")

/** Returns the value, or throws [ModuleNotConfiguredException] when it is null or blank. */
fun <T : Any> T?.requireConfigured(
    module: String,
    setting: String,
): T {
    if (this == null || (this is CharSequence && this.isBlank())) {
        throw ModuleNotConfiguredException(module, setting)
    }
    return this
}
