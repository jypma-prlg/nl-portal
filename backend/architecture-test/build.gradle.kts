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

    // Depends on every module that can contribute entry points so the fitness test can
    // scan the full backend classpath. This module ships no production code of its own.
    testImplementation(project(":core"))
    testImplementation(project(":graphql"))
    testImplementation(project(":portal-authentication"))
    testImplementation(project(":zgw:idtoken-authentication"))
    testImplementation(project(":zgw:common-ground-authentication"))
    testImplementation(project(":zgw:objectenapi"))
    testImplementation(project(":zgw:catalogi-api"))
    testImplementation(project(":zgw:openklant"))
    testImplementation(project(":case"))
    testImplementation(project(":data"))
    testImplementation(project(":form"))
    testImplementation(project(":haalcentraal-hr"))
    testImplementation(project(":haalcentraal2"))
    testImplementation(project(":zgw:taak"))
    testImplementation(project(":zgw:documenten-api"))
    testImplementation(project(":zgw:berichten"))
    testImplementation(project(":zgw:besluiten"))
    testImplementation(project(":payment-direct"))
    testImplementation(project(":zgw:zaken-api"))
    testImplementation(project(":zgw:openproduct"))
    testImplementation(project(":zgw:verificatie"))
    testImplementation(project(":zgw:zaken"))
    testImplementation(project(":app"))
    testImplementation(project(":app-dev"))

    testImplementation(TestDependencies.springBootTest)
    testImplementation(TestDependencies.junitJupiterTest)
    testImplementation(TestDependencies.assertJCore)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<Test>("test") {
    // The capability inventory lives outside the Gradle project; pass it in explicitly
    // so changes to it invalidate the test task's up-to-date check.
    val inventory = rootProject.projectDir.parentFile.resolve("docs/capabilities.yaml")
    inputs.file(inventory)
    systemProperty("capabilities.file", inventory.absolutePath)
}

val jar: Jar by tasks
val bootJar: org.springframework.boot.gradle.tasks.bundling.BootJar by tasks
bootJar.enabled = false
jar.enabled = false