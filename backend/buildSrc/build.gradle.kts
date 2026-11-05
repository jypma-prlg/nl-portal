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
import org.gradle.kotlin.dsl.`kotlin-dsl`

plugins {
    `kotlin-dsl`
    id("com.diffplug.spotless") version "8.10.4"
}

repositories {
    mavenCentral()
}

// Copyright headers (ADR 0015), same rules as backend/build.gradle.kts (buildSrc cannot share that script):
// a file that already carries the Ritense header keeps it, every other file gets the PinkRoccade header.
// The PinkRoccade year is not bumped here; buildSrc has no PinkRoccade files yet.
val ritenseHeader = """\A[\x{FEFF}\s]*/\*\s*\n\s*\* Copyright [^\n]*Den Haag, Ritense"""
val notRitenseHeader = """\A(?![\x{FEFF}\s]*/\*\s*\n\s*\* Copyright [^\n]*Den Haag, Ritense)"""

fun copyrightHeaders(addHeaderStep: (File) -> com.diffplug.gradle.spotless.FormatExtension.LicenseHeaderConfig) {
    addHeaderStep(rootDir.parentFile.resolve("licenses/ritense.template")).named("ritense").onlyIfContentMatches(ritenseHeader)
    addHeaderStep(rootDir.parentFile.resolve("licenses/pinkroccade.template")).named("pinkroccade").onlyIfContentMatches(notRitenseHeader)
}

spotless {
    kotlin {
        ktlint()
        copyrightHeaders { licenseHeaderFile(it, "\\w.*") }
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
        copyrightHeaders { licenseHeaderFile(it, "\\w.*") }
    }
}