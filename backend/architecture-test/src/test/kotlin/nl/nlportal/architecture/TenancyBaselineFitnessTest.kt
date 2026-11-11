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

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Path

/** Keeps docs/tenancy-baseline.yaml and the code in step, in both directions. */
class TenancyBaselineFitnessTest {
    @Test
    fun `the tenancy baseline matches the signals in the code`() {
        val classes =
            ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("nl.nlportal")
        val file = Path.of(System.getProperty("tenancy.baseline.file"))

        val failures =
            TenancyBaseline.compare(
                detected = TenancySignals.of(classes, ModuleLayers.production),
                baseline = TenancyBaseline.load(file),
                modules = ModuleLayers.production.map { it.modules.first() },
            )

        assertThat(failures).describedAs("tenancy-baseline (docs/tenancy-baseline.yaml) wijkt af van de code").isEmpty()
    }
}