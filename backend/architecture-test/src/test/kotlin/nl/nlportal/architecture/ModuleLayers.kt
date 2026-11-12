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

enum class Layer(
    val label: String,
    val explanation: String,
) {
    CORE("kern", "de kern mag niet afhangen van gedeelde infrastructuur of functionaliteit; alles mag van de kern afhangen"),
    SHARED_INFRA("gedeelde infrastructuur", "mag niet afhangen van functionaliteit; functionaliteit mag er wel van afhangen"),
    FEATURE("functionaliteit", "losse functionele modules; de kern en gedeelde infrastructuur mogen er niet van afhangen"),
    APP("applicatie", "de samenstelling die alles bij elkaar brengt; niets mag ervan afhangen"),
}

/**
 * Maps one or more Gradle modules to the package prefix they own and the layer they belong to.
 * Several modules may share one entry when they share a package (app and app-dev).
 */
data class ModuleMapping(
    val modules: List<String>,
    val packagePrefix: String,
    val layer: Layer,
    /** One Dutch sentence: what the module does for citizens or the municipality. Mandatory for [ModuleLayers.production]. */
    val description: String = "",
) {
    constructor(module: String, packagePrefix: String, layer: Layer, description: String = "") :
        this(listOf(module), packagePrefix, layer, description)

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
            ModuleMapping(
                ":core",
                "nl.nlportal.core",
                Layer.CORE,
                "de basis van het portaal: configuratie, beveiligingsregels en gedeelde bouwstenen voor alle andere modules",
            ),
            ModuleMapping(
                ":graphql",
                "nl.nlportal.graphql",
                Layer.SHARED_INFRA,
                "de GraphQL-koppeling waarmee de frontend gegevens van het portaal opvraagt",
            ),
            ModuleMapping(
                ":portal-authentication",
                "nl.nlportal.portal.authentication",
                Layer.SHARED_INFRA,
                "inloggen en herkennen van burgers en bedrijven in het portaal",
            ),
            ModuleMapping(
                ":zgw:zaken-api",
                "nl.nlportal.zakenapi",
                Layer.FEATURE,
                "de koppeling met de Zaken API om zaken van burgers en bedrijven op te halen",
            ),
            ModuleMapping(
                ":zgw:zaken",
                "nl.nlportal.zaken",
                Layer.FEATURE,
                "het tonen van zaken van burgers en bedrijven in het portaal",
            ),
            ModuleMapping(
                ":zgw:berichten",
                "nl.nlportal.berichten",
                Layer.FEATURE,
                "het tonen van berichten van de gemeente aan burgers en bedrijven",
            ),
            ModuleMapping(
                ":zgw:besluiten",
                "nl.nlportal.besluiten",
                Layer.FEATURE,
                "het tonen van besluiten die de gemeente over een zaak heeft genomen",
            ),
            ModuleMapping(
                ":zgw:catalogi-api",
                "nl.nlportal.catalogiapi",
                Layer.FEATURE,
                "de koppeling met de Catalogi API voor zaaktypen en statussen",
            ),
            ModuleMapping(
                ":zgw:documenten-api",
                "nl.nlportal.documentenapi",
                Layer.FEATURE,
                "het ophalen en tonen van documenten die bij een zaak horen",
            ),
            ModuleMapping(
                ":zgw:openklant",
                "nl.nlportal.openklant",
                Layer.FEATURE,
                "het ophalen en bijhouden van contactgegevens en contactmomenten van klanten via Open Klant",
            ),
            ModuleMapping(
                ":zgw:openproduct",
                "nl.nlportal.openproduct",
                Layer.FEATURE,
                "het tonen van producten die een burger of bedrijf bij de gemeente heeft via Open Product",
            ),
            ModuleMapping(
                ":zgw:verificatie",
                "nl.nlportal.verificatie",
                Layer.FEATURE,
                "het verifiëren van contactgegevens van burgers, zoals een e-mailadres of telefoonnummer",
            ),
            ModuleMapping(
                // common-ground-authentication-test shares the package prefix: it is test support
                // for authentication and ships production classes below nl.nlportal.commonground.
                listOf(":zgw:common-ground-authentication", ":zgw:common-ground-authentication-test"),
                "nl.nlportal.commonground",
                Layer.FEATURE,
                "de beveiliging van API-aanroepen naar Common Ground-registraties",
            ),
            ModuleMapping(
                ":zgw:objectenapi",
                "nl.nlportal.zgw.objectenapi",
                Layer.FEATURE,
                "het opslaan en ophalen van gegevens in de Objecten API",
            ),
            ModuleMapping(
                ":zgw:taak",
                "nl.nlportal.zgw.taak",
                Layer.FEATURE,
                "het tonen en afhandelen van taken die de gemeente aan een burger of bedrijf geeft",
            ),
            ModuleMapping(
                ":form",
                "nl.nlportal.form",
                Layer.FEATURE,
                "formulieren afhandelen en inzendingen van burgers doorgeven",
            ),
            ModuleMapping(
                ":payment-direct",
                "nl.nlportal.payment.direct",
                Layer.FEATURE,
                "betalingen van burgers rechtstreeks afhandelen",
            ),
            ModuleMapping(
                ":haalcentraal-brp",
                "nl.nlportal.haalcentraalbrp",
                Layer.FEATURE,
                "het ophalen van persoonsgegevens uit de Basisregistratie Personen via Haal Centraal",
            ),
            ModuleMapping(
                ":kvk",
                "nl.nlportal.kvk",
                Layer.FEATURE,
                "het ophalen van bedrijfsgegevens uit het Handelsregister via de KVK-API",
            ),
            // app and app-dev share one package; telling them apart is out of scope.
            ModuleMapping(
                listOf(":app", ":app-dev"),
                "nl.nlportal.app",
                Layer.APP,
                "de applicatie die alle modules samenbrengt en als portaal draait",
            ),
        )

    /**
     * Gradle projects deliberately outside the layering, each with the reason. Only for projects
     * without production classes under nl.nlportal; a project that does ship such classes must get
     * a mapping above, with a deliberately chosen layer.
     */
    val excluded: Map<String, String> =
        mapOf(
            ":gradle" to "Aggregator voor build-tooling; bevat geen code.",
            ":gradle:license-report" to "Gradle-project dat alleen het licentierapport van de build maakt; geen productiecode.",
            ":zgw" to "Aggregator voor de zgw-submodules; bevat zelf geen code.",
        )

    /** Modules whose mutual dependencies must be acyclic: every zgw module plus form. */
    fun isCycleChecked(mapping: ModuleMapping) = mapping.modules.any { it.startsWith(":zgw:") || it == ":form" }
}