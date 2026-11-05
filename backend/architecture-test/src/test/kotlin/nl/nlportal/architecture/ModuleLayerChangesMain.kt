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
package nl.nlportal.architecture

import java.io.File

/**
 * Entry point of the Gradle task `moduleLayerChanges`. Arguments: the git ref to compare with
 * (usually origin/main) and the file to write the summary to. The file is left empty when there is
 * nothing to ask, so CI posts no comment.
 */
fun main(args: Array<String>) {
    val (baseRef, output) = args
    val base = baseModuleLayersSource(baseRef)?.let(ModuleLayersSource::parse) ?: emptyList()
    val summary = LayerChangeSummary.render(base, ModuleLayers.production)
    File(output).apply { parentFile.mkdirs() }.writeText(summary)
    println(summary.ifEmpty { "Geen nieuwe modules of laagwijzigingen ten opzichte van $baseRef." })
}

/** ModuleLayers.kt as it is on [ref], or null when that commit does not have the file yet. */
private fun baseModuleLayersSource(ref: String): String? {
    requireNotNull(git("rev-parse", "--verify", "--quiet", "$ref^{commit}")) {
        "Git-ref $ref bestaat niet; haal hem eerst op (git fetch origin main)."
    }
    return git("show", "$ref:./${ModuleLayersSource.PATH}")
}

/** Standard output of the git command, or null when it fails. */
private fun git(vararg args: String): String? {
    val process = ProcessBuilder("git", *args).redirectError(ProcessBuilder.Redirect.DISCARD).start()
    val out = process.inputStream.bufferedReader().readText()
    return out.takeIf { process.waitFor() == 0 }
}