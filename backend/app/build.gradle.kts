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
import org.apache.tools.ant.taskdefs.condition.Os

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":zgw:openklant"))
    // implementation(project(":form"))
    implementation(project(":case"))
    implementation(project(":haalcentraal-hr"))
    implementation(project(":haalcentraal2"))
    implementation(project(":zgw:taak"))
    implementation(project(":zgw:documenten-api"))
    implementation(project(":zgw:berichten"))
    implementation(project(":zgw:besluiten"))
    implementation(project(":payment-direct"))
    implementation(project(":zgw:zaken"))
    implementation(project(":zgw:openproduct"))
    implementation(project(":zgw:verificatie"))
    implementation(project(":case"))

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    api("org.postgresql:postgresql")

    // Silences the netty macOS DNS resolver stacktrace on Apple Silicon at startup.
    // developmentOnly = bootRun classpath only, never packaged into the bootJar.
    if (Os.isFamily(Os.FAMILY_MAC) && Os.isArch("aarch64")) {
        developmentOnly("io.netty:netty-resolver-dns-native-macos::osx-aarch_64")
    }

    testImplementation(project(":zgw:common-ground-authentication-test"))
    testImplementation(TestDependencies.postgresql)
    testImplementation(TestDependencies.springBootTest)
    // testImplementation(TestDependencies.springSecurityTest)
    testImplementation(TestDependencies.springBootWebClientTest)
    testImplementation(TestDependencies.springBootTestWebClient)
    testImplementation(TestDependencies.springGraphQLTest)
    testImplementation(TestDependencies.assertJCore)
    testImplementation(TestDependencies.kotlinCoroutines)
    testImplementation(TestDependencies.okHttpMockWebserver)
    testImplementation("org.springframework.graphql:spring-graphql-test")
}

// This module is the shipped application: produce a runnable Spring Boot fat jar,
// disable the plain jar, and never publish it to Maven.
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
    archiveFileName.set("app.jar")
    mainClass.set("nl.nlportal.app.NlPortalApplicationKt")
}

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.withType<PublishToMavenRepository>().configureEach {
    enabled = false
}

tasks.withType<PublishToMavenLocal>().configureEach {
    enabled = false
}

dockerCompose {
    setProjectName("$name-test")
    isRequiredBy(tasks.getByName("integrationTest"))
    useComposeFiles.addAll("../docker-resources/docker-compose-base-test.yml", "docker-compose-override.yml")
}

// Local development config for a from-source run. `.env.properties` is the ONLY
// config source (gitignored; copy it from .env.properties.example). It is
// independent of docker-compose/imports/backend.env, which configures the
// containerised backend under the compose local/remote profiles.
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    val envFile = project.file(".env.properties")

    if (envFile.exists() && envFile.isFile) {
        environment.putAll(
            envFile
                .readLines()
                .filterNot { it.startsWith("#") || it.startsWith("//") || it.isBlank() }
                .associate { line ->
                    val entry = line.split("=", limit = 2)
                    entry.first().trim() to entry.last().trim()
                },
        )
    } else {
        doFirst {
            throw GradleException(
                "backend/app/.env.properties not found. Copy .env.properties.example to " +
                    ".env.properties (it is gitignored) and adjust it for your local stack " +
                    "before running :app:bootRun.",
            )
        }
    }
}