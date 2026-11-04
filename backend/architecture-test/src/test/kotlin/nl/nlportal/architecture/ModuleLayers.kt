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
package nl.nlportal.architecture

enum class Layer { CORE, SHARED_INFRA, FEATURE, APP }

/**
 * Maps one or more Gradle modules to the package prefix they own and the layer they belong to.
 * Several modules may share one entry when they share a package (app and app-dev).
 */
data class ModuleMapping(
    val modules: List<String>,
    val packagePrefix: String,
    val layer: Layer,
) {
    constructor(module: String, packagePrefix: String, layer: Layer) : this(listOf(module), packagePrefix, layer)

    fun owns(packageName: String) = packageName == packagePrefix || packageName.startsWith("$packagePrefix.")
}

/**
 * Single source of truth for the module -> package -> layer relation. The layer rules, the cycle
 * slices and the completeness check are all derived from this list. When several prefixes match
 * a package the longest wins, so zgw:objectenapi and zgw:taak (both below `nl.nlportal.zgw`)
 * stay distinct.
 */
object ModuleLayers {
    val production: List<ModuleMapping> =
        listOf(
            ModuleMapping(":core", "nl.nlportal.core", Layer.CORE),
            ModuleMapping(":data", "nl.nlportal.data", Layer.SHARED_INFRA),
            ModuleMapping(":graphql", "nl.nlportal.graphql", Layer.SHARED_INFRA),
            ModuleMapping(":portal-authentication", "nl.nlportal.portal.authentication", Layer.SHARED_INFRA),
            ModuleMapping(":zgw:zaken-api", "nl.nlportal.zakenapi", Layer.FEATURE),
            ModuleMapping(":zgw:zaken", "nl.nlportal.zaken", Layer.FEATURE),
            ModuleMapping(":zgw:berichten", "nl.nlportal.berichten", Layer.FEATURE),
            ModuleMapping(":zgw:besluiten", "nl.nlportal.besluiten", Layer.FEATURE),
            ModuleMapping(":zgw:catalogi-api", "nl.nlportal.catalogiapi", Layer.FEATURE),
            ModuleMapping(":zgw:documenten-api", "nl.nlportal.documentenapi", Layer.FEATURE),
            ModuleMapping(":zgw:openklant", "nl.nlportal.openklant", Layer.FEATURE),
            ModuleMapping(":zgw:openproduct", "nl.nlportal.openproduct", Layer.FEATURE),
            ModuleMapping(":zgw:verificatie", "nl.nlportal.verificatie", Layer.FEATURE),
            ModuleMapping(":zgw:idtoken-authentication", "nl.nlportal.idtokenauthentication", Layer.FEATURE),
            ModuleMapping(":zgw:common-ground-authentication", "nl.nlportal.commonground", Layer.FEATURE),
            ModuleMapping(":zgw:objectenapi", "nl.nlportal.zgw.objectenapi", Layer.FEATURE),
            ModuleMapping(":zgw:taak", "nl.nlportal.zgw.taak", Layer.FEATURE),
            ModuleMapping(":case", "nl.nlportal.case", Layer.FEATURE),
            ModuleMapping(":form", "nl.nlportal.form", Layer.FEATURE),
            ModuleMapping(":payment-direct", "nl.nlportal.payment.direct", Layer.FEATURE),
            ModuleMapping(":haalcentraal2", "nl.nlportal.haalcentraal2", Layer.FEATURE),
            ModuleMapping(":haalcentraal-hr", "nl.nlportal.haalcentraal.hr", Layer.FEATURE),
            // app and app-dev share one package; telling them apart is out of scope.
            ModuleMapping(listOf(":app", ":app-dev"), "nl.nlportal.app", Layer.APP),
        )

    /** Modules whose mutual dependencies must be acyclic: every zgw module plus form. */
    fun isCycleChecked(mapping: ModuleMapping) = mapping.modules.any { it.startsWith(":zgw:") || it == ":form" }
}