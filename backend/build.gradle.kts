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
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import io.spring.gradle.dependencymanagement.org.codehaus.plexus.interpolation.os.Os.FAMILY_MAC
import org.apache.tools.ant.taskdefs.condition.Os
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import java.net.URI
import kotlin.io.encoding.Base64.Default.decode

val sonatypeCentralStagingDir: String = project.property("sonatypeCentralStagingDir").toString()

plugins {
    java

    // IntelliJ
    idea

    // Apply the Kotlin JVM plugin to add support for Kotlin on the JVM.
    kotlin("jvm")

    // Classes annotated with @Configuration, @Controller, @RestController, @Service or @Repository are automatically opened
    // https://kotlinlang.org/docs/reference/compiler-plugins.html#spring-support

    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    kotlin("plugin.allopen")

    // Allows to package executable jar or war archives, run Spring Boot applications,
    // and use the dependency management
    // https://docs.spring.io/spring-boot/docs/current/gradle-plugin/reference/html/
    id("org.springframework.boot")

    // A Gradle plugin that provides Maven-like dependency management and exclusions
    // https://docs.spring.io/dependency-management-plugin/docs/current/reference/html/
    id("io.spring.dependency-management")

    // For dependency version upgrades "gradle dependencyUpdates -Drevision=release"
    id("com.github.ben-manes.versions")

    // Checkstyle
    id("com.diffplug.spotless")

    // Docker-compose plugin
    id("com.avast.gradle.docker-compose")

    id("com.github.jk1.dependency-license-report")

    id("org.sonarqube")

    `maven-publish`
    signing
}

spotless {
    kotlinGradle {
        target("*.gradle.kts", "gradle/*.gradle.kts")
        ktlint()
        licenseHeaderFile(
            rootProject.file("licenses/kts.template"),
            "\\w.*",
        )
    }
}

allprojects {
    repositories {
        mavenCentral()
        maven(URI("https://repository.jboss.org/nexus/content/repositories/releases"))
        maven(URI("https://oss.sonatype.org/content/repositories/releases"))
        maven(URI("https://app.camunda.com/nexus/content/groups/public"))
        maven(URI("https://s01.oss.sonatype.org/content/groups/staging/"))
        maven(URI("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
    }
}

subprojects {
    println("Enabling com.avast.gradle.docker-compose plugin in project ${project.name}...")
    apply(plugin = "com.avast.gradle.docker-compose")

    println("Enabling com.diffplug.spotless plugin in project ${project.name}...")
    apply(plugin = "com.diffplug.spotless")

    apply(plugin = "java")

    apply(plugin = "maven-publish")

    var signingConfigSet = false
    if (System.getenv("SIGNING_KEY") != null &&
        System.getenv("SIGNING_KEY_PASSWORD") != null
    ) {
        signingConfigSet = true
        apply(plugin = "signing")
    }

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            ktlint()
            licenseHeaderFile(rootProject.file("licenses/kt.template"))
        }
        kotlinGradle {
            target("*.gradle.kts", "gradle/*.gradle.kts")
            ktlint()
            licenseHeaderFile(
                rootProject.file("licenses/kts.template"),
                "\\w.*",
            )
        }
    }

    java {
        withSourcesJar()
        withJavadocJar()
    }

    if (!(project.path.contains("gradle"))) {
        println("Enabling Spring Boot plugin in project ${project.name}...")
        apply(plugin = "org.springframework.boot")

        println("Enabling Spring Boot Dependency Management in project ${project.name}...")
        apply(plugin = "io.spring.dependency-management")

        configure<DependencyManagementExtension> {
            imports {
                mavenBom("io.netty:netty-bom:${Versions.netty}")
            }
            dependencies {
                // Security overrides on top of the Spring Boot 4.1.0 BOM.
                dependency("org.postgresql:postgresql:${Versions.postgresql}")
                dependency("org.apache.httpcomponents.core5:httpcore5:${Versions.httpCore5}")
                dependency("org.apache.httpcomponents.core5:httpcore5-h2:${Versions.httpCore5}")
                dependency("org.apache.logging.log4j:log4j-api:${Versions.log4j2}")
                dependency("org.apache.logging.log4j:log4j-to-slf4j:${Versions.log4j2}")
                dependency("org.bouncycastle:bcprov-jdk18on:${Versions.bouncyCastle}")
                dependency("tools.jackson.core:jackson-databind:${Versions.jackson}")
                dependency("tools.jackson.core:jackson-core:${Versions.jackson}")
            }
        }
    }

    println("Enabling Kotlin Spring plugin in project ${project.name}...")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")

    println("Enabling Kotlin JPA plugin in project ${project.name}...")
    apply(plugin = "org.jetbrains.kotlin.plugin.jpa")

    println("Enabling Kotlin All-open plugin in project ${project.name}...")
    apply(plugin = "org.jetbrains.kotlin.plugin.allopen")

    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.add("-Xjsr305=strict")
            freeCompilerArgs.add("-Xemit-jvm-type-annotations")
        }
    }

    publishing {
        repositories {
            maven {
                name = "Sonatype"
                url =
                    uri(
                        project.layout.buildDirectory
                            .dir(sonatypeCentralStagingDir)
                            .get()
                            .asFile.path,
                    )
            }
        }

        publications {
            register<MavenPublication>("default") {
                groupId = "nl.nl-portal"
                pom {
                    url = "https://github.com/nl-portal/nl-portal-backend-libraries"
                    licenses {
                        license {
                            name = "Licensed under EUPL, Version 1.2 (the \"License\");"
                            url = "https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12"
                        }
                    }
                    scm {
                        connection = "git@github.com:nl-portal/nl-portal-backend-libraries.git"
                        developerConnection = "git@github.com:nl-portal/nl-portal-backend-libraries.git"
                        url = "https://github.com/nl-portal/nl-portal-backend-libraries"
                    }
                }
                from(components["java"])
            }
        }
    }

    apply(from = "${rootProject.projectDir}/gradle/testing.gradle.kts")

    sonar {
        properties {
            property(
                "sonar.coverage.jacoco.xmlReportPaths",
                "${layout.buildDirectory.get()}/reports/jacoco/test/jacocoTestReport.xml",
            )
        }
    }

    if (Os.isFamily(FAMILY_MAC)) {
        println("Configure docker compose for macOs")
        dockerCompose {
            executable = "/usr/local/bin/docker-compose"
            dockerExecutable = "/usr/local/bin/docker"
        }
    }
}

sonar {
    properties {
        property("sonar.projectKey", "pinkroccade_nl-portal-backend")
        property("sonar.projectName", "nl-portal-backend")
        property("sonar.organization", "pinkroccade")
        property("sonar.token", System.getenv("SONAR_TOKEN"))
    }
}

listOf(":app", ":app-dev", ":gradle:license-report").forEach { modulePath ->
    project(modulePath) {
        sonar {
            isSkipProject = true
        }
    }
}

tasks.register<HtmlDependencyReportTask>("htmlDependencyReport")

tasks.named<HtmlDependencyReportTask>("htmlDependencyReport") {
    projects = project.allprojects
    reports.html.outputLocation = file("build/reports/project/dependencies")
}

tasks.bootJar {
    enabled = false
}

fun getSigningKey(signingKeyBase64: String): ByteArray = decode(signingKeyBase64.subSequence(0, signingKeyBase64.length))

tasks.withType<PublishToMavenRepository> {
    enabled = false
}
tasks.withType<PublishToMavenLocal> {
    enabled = false
}