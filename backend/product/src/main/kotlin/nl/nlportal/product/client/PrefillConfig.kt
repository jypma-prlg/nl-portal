/*
 * Copyright 2015-2025 Den Haag, Ritense, the Netherlands.
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
package nl.nlportal.product.client

import nl.nlportal.core.util.ShaVersion
import org.springframework.boot.context.properties.ConfigurationProperties

@Deprecated("Use instead openproduct functionality")
@ConfigurationProperties(prefix = "nl-portal.config.prefill", ignoreUnknownFields = true)
class PrefillConfig {
    var enabled: Boolean = false
    var properties: PrefillConfigProperties = PrefillConfigProperties()

    class PrefillConfigProperties {
        var typeUrl: String = ""
        var prefillShaVersion: String = ShaVersion.SHA512.version
    }
}