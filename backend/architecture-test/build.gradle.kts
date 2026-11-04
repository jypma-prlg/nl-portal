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
plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    // Depends on every other module so ArchUnit can scan the full module classpath
    // for layering violations. This module ships no production code of its own.
    testImplementation(project(":core"))
    testImplementation(project(":data"))
    testImplementation(project(":graphql"))
    testImplementation(project(":portal-authentication"))
    testImplementation(project(":case"))
    testImplementation(project(":form"))
    testImplementation(project(":haalcentraal-hr"))
    testImplementation(project(":haalcentraal2"))
    testImplementation(project(":payment-direct"))
    testImplementation(project(":zgw:idtoken-authentication"))
    testImplementation(project(":zgw:common-ground-authentication"))
    testImplementation(project(":zgw:objectenapi"))
    testImplementation(project(":zgw:catalogi-api"))
    testImplementation(project(":zgw:openklant"))
    testImplementation(project(":zgw:taak"))
    testImplementation(project(":zgw:documenten-api"))
    testImplementation(project(":zgw:berichten"))
    testImplementation(project(":zgw:besluiten"))
    testImplementation(project(":zgw:zaken-api"))
    testImplementation(project(":zgw:zaken"))
    testImplementation(project(":zgw:openproduct"))
    testImplementation(project(":zgw:verificatie"))
    testImplementation(project(":app"))
    testImplementation(project(":app-dev"))

    testImplementation(TestDependencies.archUnitJunit5)
    testImplementation(TestDependencies.junitJupiterTest)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val jar: Jar by tasks
val bootJar: org.springframework.boot.gradle.tasks.bundling.BootJar by tasks
bootJar.enabled = false
jar.enabled = false