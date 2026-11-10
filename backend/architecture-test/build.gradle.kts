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
plugins {
    kotlin("jvm")
}

dependencies {
    // Depends on every module that can contribute entry points so the fitness test can
    // scan the full backend classpath. This module ships no production code of its own.
    rootProject.subprojects
        .filter { it.path != project.path && !it.path.startsWith(":gradle") }
        .forEach { testImplementation(project(it.path)) }

    testImplementation(TestDependencies.springBootTest)
    testImplementation(TestDependencies.archUnitJunit5)
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
    val tenancyBaseline = rootProject.projectDir.parentFile.resolve("docs/tenancy-baseline.yaml")
    inputs.file(tenancyBaseline)
    systemProperty("tenancy.baseline.file", tenancyBaseline.absolutePath)
    systemProperty(
        "architecture.diagram.file",
        rootProject.projectDir.parentFile
            .resolve("site/architectuur/img/plantuml/modules.puml")
            .absolutePath,
    )
    // Every Gradle project except this one is handed to the test: it must either be mapped to a layer
    // or be listed (with a reason) in ModuleLayers.excluded. Nothing is filtered out here, so a new
    // module cannot silently escape the check.
    val buildModules = rootProject.subprojects.filter { it.path != project.path }.sortedBy { it.path }
    inputs.property("buildModulePaths", buildModules.map { it.path })
    buildModules.forEach { inputs.files(fileTree(it.projectDir.resolve("src/main"))).withPropertyName("sources-${it.path}") }
    systemProperty("architecture.modules", buildModules.joinToString("|") { "${it.path}=${it.projectDir.absolutePath}" })
    // Make fitness-test warnings visible in the Gradle output.
    testLogging.showStandardStreams = true
}

// Compares ModuleLayers with the base branch and writes a plain-language summary of new modules
// and layer changes for the PR (#177). The file stays empty when there is nothing to ask.
// Base branch: -PmoduleLayersBaseRef=<ref>, default origin/main.
tasks.register<JavaExec>("moduleLayerChanges") {
    description = "Summarise new modules and layer changes in ModuleLayers compared with the base branch"
    group = "verification"
    val sourceSets = project.extensions.getByType<SourceSetContainer>()
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("nl.nlportal.architecture.ModuleLayerChangesMainKt")
    workingDir = projectDir
    val summary = layout.buildDirectory.file("reports/module-layers/pr-summary.md")
    args(providers.gradleProperty("moduleLayersBaseRef").getOrElse("origin/main"), summary.get().asFile.absolutePath)
    outputs.file(summary)
    // Depends on git state outside Gradle's view; always rerun.
    outputs.upToDateWhen { false }
}

val jar: Jar by tasks
val bootJar: org.springframework.boot.gradle.tasks.bundling.BootJar by tasks
bootJar.enabled = false
jar.enabled = false
// Writes the module diagram (PlantUML) to the docs site; the fitness test fails when the committed file is stale.
val diagramFile = rootProject.projectDir.parentFile.resolve("site/architectuur/img/plantuml/modules.puml")
tasks.register<JavaExec>("architectureDiagram") {
    description = "Generate the module/rule-coverage PlantUML diagram"
    group = "documentation"
    val sourceSets = project.extensions.getByType<SourceSetContainer>()
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("nl.nlportal.architecture.ArchitectureDiagramKt")
    args(
        diagramFile.absolutePath,
        rootProject.projectDir.parentFile
            .resolve("docs/tenancy-baseline.yaml")
            .absolutePath,
    )
    inputs.file(rootProject.projectDir.parentFile.resolve("docs/tenancy-baseline.yaml"))
    outputs.file(diagramFile)
}