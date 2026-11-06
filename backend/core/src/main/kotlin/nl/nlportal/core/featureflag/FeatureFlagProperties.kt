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

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "nl-portal.feature-flags")
class FeatureFlagProperties {
    /** Path of a flagd flag definition file. Defaults to the flag file bundled with NL Portal. */
    var file: String? = null

    /** How often the flag file is checked for changes, in milliseconds. */
    var pollIntervalMs: Int = 5000

    /** Fixed parts of the evaluation context; see [FeatureContextFactory]. */
    var context: Context = Context()

    class Context {
        /** The tenant this instance serves. Single-tenant for now; resolution per request follows with multi-tenancy. */
        var tenant: String = "default"

        /** The environment, for example `ontwikkel`, `test`, `acceptatie` or `productie`. Omitted when not set. */
        var environment: String? = null

        /**
         * Secret key of the tenant for the opaque user id (HMAC of the identity provider subject).
         * Without a secret the context holds no user id. Comes from the environment, never from version control.
         */
        var userIdSecret: String? = null
    }
}