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

import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.dependencies.SliceAssignment
import com.tngtech.archunit.library.dependencies.SliceIdentifier
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import org.junit.jupiter.api.Test

/**
 * Enforces the layering described in site/architectuur/arc42: core -> shared infra (data, graphql,
 * portal-authentication) -> feature modules (zgw:*, case, form, payment-direct, haalcentraal*)
 * -> app/app-dev. A violation here fails the build instead of surfacing only at code review.
 *
 * Package names in this codebase don't share a common "nl.nlportal.zgw" prefix per Gradle
 * module (e.g. zgw:zaken is `nl.nlportal.zaken`, zgw:taak is `nl.nlportal.zgw.taak`), so the
 * feature-package list below is enumerated explicitly from the actual top-level packages.
 */
class LayerArchitectureTest {
    private val classes =
        ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("nl.nlportal")

    private val featurePackages =
        arrayOf(
            "nl.nlportal.zgw..",
            "nl.nlportal.zaken..",
            "nl.nlportal.zakenapi..",
            "nl.nlportal.berichten..",
            "nl.nlportal.besluiten..",
            "nl.nlportal.catalogiapi..",
            "nl.nlportal.documentenapi..",
            "nl.nlportal.openklant..",
            "nl.nlportal.openproduct..",
            "nl.nlportal.verificatie..",
            "nl.nlportal.idtokenauthentication..",
            "nl.nlportal.commonground..",
            "nl.nlportal.case..",
            "nl.nlportal.form..",
            "nl.nlportal.payment.direct..",
            "nl.nlportal.haalcentraal..",
        )

    @Test
    fun `core does not depend on a feature module`() {
        noClasses()
            .that()
            .resideInAPackage("nl.nlportal.core..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(*featurePackages)
            .check(classes)
    }

    @Test
    fun `shared infra modules do not depend on a feature module`() {
        val sharedInfraPackages =
            arrayOf(
                "nl.nlportal.data..",
                "nl.nlportal.graphql..",
                "nl.nlportal.portal.authentication..",
            )

        noClasses()
            .that()
            .resideInAnyPackage(*sharedInfraPackages)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(*featurePackages)
            .check(classes)
    }

    @Test
    fun `nothing outside the app packages depends on app or app-dev`() {
        noClasses()
            .that()
            .resideOutsideOfPackage("nl.nlportal.app..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("nl.nlportal.app..")
            .check(classes)
    }

    @Test
    fun `zgw submodules have no cyclic dependencies`() {
        val zgwSlicePackages =
            mapOf(
                "zaken-api" to "nl.nlportal.zakenapi.",
                "zaken" to "nl.nlportal.zaken.",
                "berichten" to "nl.nlportal.berichten.",
                "besluiten" to "nl.nlportal.besluiten.",
                "catalogi-api" to "nl.nlportal.catalogiapi.",
                "documenten-api" to "nl.nlportal.documentenapi.",
                "openklant" to "nl.nlportal.openklant.",
                "openproduct" to "nl.nlportal.openproduct.",
                "verificatie" to "nl.nlportal.verificatie.",
                "idtoken-authentication" to "nl.nlportal.idtokenauthentication.",
                "common-ground-authentication" to "nl.nlportal.commonground.",
                "objectenapi" to "nl.nlportal.zgw.objectenapi.",
                "taak" to "nl.nlportal.zgw.taak.",
            )

        val zgwSliceAssignment =
            object : SliceAssignment {
                override fun getIdentifierOf(javaClass: JavaClass): SliceIdentifier {
                    val packageName = javaClass.packageName + "."
                    val slice =
                        zgwSlicePackages.entries
                            .firstOrNull { (_, prefix) -> packageName.startsWith(prefix) }
                    return slice?.let { SliceIdentifier.of(it.key) } ?: SliceIdentifier.ignore()
                }

                override fun getDescription() = "zgw submodules"
            }

        slices()
            .assignedFrom(zgwSliceAssignment)
            .should()
            .beFreeOfCycles()
            .check(classes)
    }
}