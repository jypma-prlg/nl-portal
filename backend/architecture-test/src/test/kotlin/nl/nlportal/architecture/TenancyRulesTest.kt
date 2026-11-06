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

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.ArchRule
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

/**
 * PROTOTYPE (wayfinder #204). Drie soorten bewijs voor de tenancy-regels:
 * 1. fixtures met een opzettelijke overtreding: de regel MOET falen (en alleen op de bedoelde klasse);
 * 2. de nieuwe tenancy-code zelf is schoon;
 * 3. de bestaande code: baseline van overtredingen per regel (ratchet) en bewijs dat de regels de bekende
 *    problemen uit het onderzoek echt vinden.
 */
class TenancyRulesTest {
    private val base = "nl.nlportal.architecture.fixtures.tenancyvio"
    private val rules = TenancyRules()

    private fun fixture(pkg: String): JavaClasses = ClassFileImporter().importPackages("$base.$pkg")

    private fun violationOf(
        rule: ArchRule,
        classes: JavaClasses,
    ): String = assertThatThrownBy { rule.check(classes) }.isInstanceOf(AssertionError::class.java).actual().message!!

    // 1. Bewijs dat de regels vangen

    @Test
    fun `configuratie-regel vangt een service die de geneste properties of de config zelf leest`() {
        val message = violationOf(rules.noConfigurationPropertiesOutsideAutoconfig(), fixture("config"))
        assertThat(message).contains("ViolatingNestedService", "ViolatingDirectService")
        // autoconfigure-pakket en *AutoConfiguration-klassen blijven toegestaan
        assertThat(message).doesNotContain("FixtureAutoConfiguration", "FixtureFactory")
    }

    @Test
    fun `webclient-regel vangt create en builder, maar niet een geinjecteerde builder`() {
        val message = violationOf(rules.noWebClientFactoryOutsideCore(), fixture("webclient"))
        assertThat(message).contains("ViolatingWebClientCreate", "ViolatingWebClientBuilder").doesNotContain("InjectedBuilderUser")
    }

    @Test
    fun `webclient-regel laat het toegestane pakket door`() {
        // 'clean' wordt wel getoetst (anders faalt de regel op "geen klassen"), 'webclient' is uitgezonderd.
        TenancyRules(webClientAllowed = arrayOf("$base.webclient.."))
            .noWebClientFactoryOutsideCore()
            .check(ClassFileImporter().importPackages("$base.webclient", "$base.clean"))
    }

    @Test
    fun `globale-state-regels vangen elk hun eigen overtreding`() {
        val state = fixture("state")
        assertThat(violationOf(rules.noThreadLocal(), state)).contains("ThreadLocal")
        assertThat(violationOf(rules.noInMemorySecretKeyStore(), state)).contains("InMemorySecretKeyStore")
        assertThat(violationOf(rules.noGlobalScope(), state)).contains("GlobalScope")
        assertThat(violationOf(rules.noRunBlocking(), state)).contains("runBlocking")
        assertThat(violationOf(rules.noNewCoroutineScope(), state)).contains("CoroutineScope", "MainScope")
        assertThat(violationOf(rules.noOpenFeatureSingletonOutsideFeatureflag(), state)).contains("OpenFeatureAPI.getInstance")
    }

    @Test
    fun `openfeature-regel laat core featureflag door`() {
        TenancyRules(openFeatureAllowed = arrayOf("$base.state.."))
            .noOpenFeatureSingletonOutsideFeatureflag()
            .check(ClassFileImporter().importPackages("$base.state", "$base.clean"))
    }

    @Test
    fun `schone code slaat geen enkele regel aan`() {
        // 'clean' heeft geen van de verboden constructies; allowEmptyShould(false) vereist wel dat er klassen getoetst worden.
        val clean = fixture("clean")
        val ruleSet = rules.all().filterKeys { it !in setOf("webClientFactory", "openFeatureSingleton", "configurationProperties") }
        ruleSet.forEach { (name, rule) -> runCatching { rule.check(clean) }.onFailure { error("regel $name sloeg aan op schone code: ${it.message}") } }
    }

    // 2. De nieuwe tenancy-code is schoon

    private val production =
        ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("nl.nlportal")

    @Test
    fun `de tenancy-seam en de zakenapi-slice overtreden geen enkele regel`() {
        val seam =
            ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("nl.nlportal.core.tenancy", "nl.nlportal.zakenapi.prototype")
        rules.all().forEach { (name, rule) ->
            runCatching { rule.check(seam) }.onFailure { error("regel $name: ${it.message}") }
        }
    }

    // 3. Bestaande code: baseline (ratchet) en herkenning van bekende problemen

    private fun violations(rule: ArchRule): List<String> =
        rule
            .evaluate(production)
            .failureReport
            .details
            .map { it.toString() }

    @Test
    fun `baseline van bestaande overtredingen per regel mag alleen dalen`() {
        // Gemeten op main (775f21874). Verlaag de waarden bij elke migratie; verhogen mag nooit.
        val baseline =
            mapOf(
                "configurationProperties" to BASELINE_CONFIGURATION_PROPERTIES,
                "webClientFactory" to BASELINE_WEBCLIENT,
                "threadLocal" to 0,
                "inMemorySecretKeyStore" to BASELINE_SECRET_STORE,
                "openFeatureSingleton" to 0,
                "globalScope" to 0,
                "runBlocking" to 0,
                "newCoroutineScope" to 0,
            )
        val actual = rules.all().mapValues { (_, rule) -> violations(rule).size }
        println("PROTOTYPE-BASELINE overtredingen per regel op de bestaande code: $actual")
        actual.forEach { (name, count) -> assertThat(count).describedAs("regel $name").isLessThanOrEqualTo(baseline.getValue(name)) }
    }

    @Test
    fun `de regels vinden de bekende problemen uit het onderzoek in de echte code`() {
        assertThat(violations(rules.noInMemorySecretKeyStore()).joinToString()).contains("DirectPaymentWebhookService")
        assertThat(violations(rules.noWebClientFactoryOutsideCore()).joinToString()).contains("CommonGroundAuthenticationConverter")
        assertThat(violations(rules.noConfigurationPropertiesOutsideAutoconfig()).joinToString()).contains("ZakenApiService")
    }

    private companion object {
        // Gemeten op main + prototype: aantal overtredingen (afhankelijkheden), niet aantal klassen.
        const val BASELINE_CONFIGURATION_PROPERTIES = 289
        const val BASELINE_WEBCLIENT = 10
        const val BASELINE_SECRET_STORE = 3
    }
}