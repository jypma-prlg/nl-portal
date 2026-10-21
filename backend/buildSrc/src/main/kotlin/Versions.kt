/*
 * Copyright 2015-2023 Den Haag, Ritense, the Netherlands.
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
object Versions {
    const val everitJsonSchema = "1.14.6"
    const val jsonPath = "2.10.0"
    const val kotlinCoroutines = "1.10.2"
    const val apacheTikaVersion = "3.3.0"
    const val clamAvVersion = "2.1.2"
    const val apacheCommons = "3.20.0"
    const val commonsIo = "2.21.0"
    const val commonsCodec = "1.22.0"
    const val graphqlJava = "25.0"
    const val graphqlJavaScalars = "24.0"
    const val kotlinLogging = "7.0.13"
    const val springCloud = "5.0.2"
    const val httpCore5 = "5.4.4" // override Boot 4.1.0 BOM (5.4.2), CVE-2026-54399/-54428 fix
    const val log4j2 = "2.26.1" // override Boot 4.1.0 BOM (2.25.4), CVE-2026-49844 fix
    const val netty = "4.2.18.Final" // override Boot 4.1.0 BOM (4.2.15.Final), CVE-2026-44891/-55831/-55833 fix
    const val bouncyCastle = "1.85" // override Spring Cloud 5.0.2's bcprov-jdk18on (1.81.1), CVE-2026-8763 fix

    // Test versions
    const val hamcrest = "3.0"
    const val mockitoKotlin = "6.1.0"
    const val okHttp3 = "5.2.0"
    const val postgresql = "42.7.13"
    const val h2 = "2.4.240"
}