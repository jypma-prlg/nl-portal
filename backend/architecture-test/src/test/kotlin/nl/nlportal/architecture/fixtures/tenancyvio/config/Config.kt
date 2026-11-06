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
package nl.nlportal.architecture.fixtures.tenancyvio.config

import org.springframework.boot.context.properties.ConfigurationProperties

/** Nabootsing van het echte patroon: een @ConfigurationProperties-klasse met een geneste properties-klasse. */
@ConfigurationProperties(prefix = "fixture")
class FixtureConfig {
    var properties: FixtureProperties = FixtureProperties()

    class FixtureProperties {
        var url: String = ""
    }
}

/** OVERTREDING: een service leest de geneste properties-klasse (zoals ZakenApiService ZakenApiConfigProperties leest). */
class ViolatingNestedService(
    private val properties: FixtureConfig.FixtureProperties,
) {
    fun url() = properties.url
}

/** OVERTREDING: een service leest de @ConfigurationProperties-klasse zelf. */
class ViolatingDirectService(
    private val config: FixtureConfig,
) {
    fun url() = config.properties.url
}