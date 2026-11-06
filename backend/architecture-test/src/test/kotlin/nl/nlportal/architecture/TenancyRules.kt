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

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaMethodCall
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * PROTOTYPE (wayfinder #204, weggooibaar). ArchUnit-regels die de tenantscheiding bewaken (onderzoek §6).
 * Uitzonderingspakketten zijn parameters, zodat de bewijstest ze met fixtures kan oefenen.
 *
 * Wat deze regels NIET kunnen: runtime-gedrag (verloren Reactor Context door een vergeten `contextWrite`,
 * een issuer buiten de publieke broker, of een module die `registry.current()` niet gebruikt maar een
 * eigen veld met een client vasthoudt). Dat dekken de runtime-tests en het fail-fast bij opstarten.
 */
class TenancyRules(
    /** Pakketten waar `WebClient.create()/builder()` mag: core en de (toekomstige) zgw-client. */
    private val webClientAllowed: Array<String> = arrayOf("nl.nlportal.core..", "nl.nlportal.zgw.client.."),
    /** Pakketten waar `@ConfigurationProperties`-klassen gelezen mogen worden: autoconfiguratie. */
    private val configurationAllowed: Array<String> = arrayOf("..autoconfigure..", "..autoconfiguration.."),
    private val openFeatureAllowed: Array<String> = arrayOf("nl.nlportal.core.featureflag.."),
) {
    /** Een `@ConfigurationProperties`-klasse zelf, of een geneste klasse daarvan (zoals `ZakenApiConfigProperties`). */
    private val isConfigurationProperties =
        DescribedPredicate.describe<JavaClass>("@ConfigurationProperties-klasse (of geneste klasse daarvan)") { c ->
            c.isAnnotatedWith(ConfigurationProperties::class.java) ||
                c.enclosingClass.map { it.isAnnotatedWith(ConfigurationProperties::class.java) }.orElse(false)
        }

    private val isAutoConfigurationClass =
        DescribedPredicate.describe<JavaClass>("een *AutoConfiguration-klasse") { it.simpleName.endsWith("AutoConfiguration") }

    private fun callTo(
        description: String,
        ownerPackage: String,
        namePrefix: String,
    ) = DescribedPredicate.describe<JavaMethodCall>(description) { call ->
        call.targetOwner.packageName == ownerPackage && call.name.startsWith(namePrefix)
    }

    /** 1. Services lezen geen `@ConfigurationProperties`; alleen autoconfiguratie bindt ze, services gebruiken het register. */
    fun noConfigurationPropertiesOutsideAutoconfig(): ArchRule =
        noClasses()
            .that()
            .resideOutsideOfPackages(*configurationAllowed)
            .and(DescribedPredicate.not(isConfigurationProperties))
            .and(DescribedPredicate.not(isAutoConfigurationClass))
            .should()
            .dependOnClassesThat(isConfigurationProperties)
            .allowEmptyShould(false)
            .because("configuratie per tenant komt uit het TenantRegistry; een singleton-config in een service kiest stil één tenant")

    /** 2. Geen `WebClient.create()` of `WebClient.builder()` buiten core en zgw-client. */
    fun noWebClientFactoryOutsideCore(): ArchRule =
        noClasses()
            .that()
            .resideOutsideOfPackages(*webClientAllowed)
            .should()
            .callMethodWhere(
                DescribedPredicate.describe("WebClient.create()/builder() aanroept") { call: JavaMethodCall ->
                    call.targetOwner.fullName == "org.springframework.web.reactive.function.client.WebClient" &&
                        call.name in setOf("create", "builder")
                },
            ).allowEmptyShould(false)
            .because("clients horen uit het per-tenant register te komen; een los gebouwde WebClient kent geen tenant")

    /** 3a. Geen ThreadLocal: de tenant leeft in de Reactor Context. */
    fun noThreadLocal(): ArchRule =
        noClasses()
            .should()
            .dependOnClassesThat()
            .areAssignableTo(ThreadLocal::class.java)
            .allowEmptyShould(false)
            .because("ThreadLocal overleeft geen thread- of coroutinewissel in WebFlux")

    /** 3b. Geen JVM-globale secretstore van de payment-SDK (sleutels van tenants overschrijven elkaar stil). */
    fun noInMemorySecretKeyStore(): ArchRule =
        noClasses()
            .should()
            .dependOnClassesThat(DescribedPredicate.describe("InMemorySecretKeyStore") { it.simpleName == "InMemorySecretKeyStore" })
            .allowEmptyShould(false)
            .because("de JVM-singleton deelt één keyId-namespace over alle tenants")

    /** 3c. `OpenFeatureAPI.getInstance()` buiten core.featureflag (globale evaluatiecontext lekt tussen tenants). */
    fun noOpenFeatureSingletonOutsideFeatureflag(): ArchRule =
        noClasses()
            .that()
            .resideOutsideOfPackages(*openFeatureAllowed)
            .should()
            .callMethodWhere(
                DescribedPredicate.describe("OpenFeatureAPI.getInstance() aanroept") { call: JavaMethodCall ->
                    call.targetOwner.simpleName == "OpenFeatureAPI" && call.name == "getInstance"
                },
            ).allowEmptyShould(false)

    /** 4a. Geen `GlobalScope`: een nieuwe scope heeft geen Reactor Context en dus geen tenant. */
    fun noGlobalScope(): ArchRule =
        noClasses()
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("kotlinx.coroutines.GlobalScope")
            .allowEmptyShould(false)
            .because("GlobalScope verliest de Reactor Context")

    /** 4b. Geen `runBlocking`: blokkeert de event loop en kopieert de Reactor Context niet. */
    fun noRunBlocking(): ArchRule =
        noClasses()
            .should()
            .callMethodWhere(callTo("runBlocking aanroept", "kotlinx.coroutines", "runBlocking"))
            .allowEmptyShould(false)

    /** 4c. Geen nieuwe `CoroutineScope(...)`/`MainScope()`: gebruik `coroutineScope { }` of `withContext`. */
    fun noNewCoroutineScope(): ArchRule =
        noClasses()
            .should()
            .callMethodWhere(
                DescribedPredicate.describe("CoroutineScope(...) of MainScope() aanroept") { call: JavaMethodCall ->
                    call.targetOwner.packageName == "kotlinx.coroutines" && call.name in setOf("CoroutineScope", "MainScope")
                },
            ).allowEmptyShould(false)
            .because("een los gebouwde scope erft de Reactor Context, en dus de tenant, niet")

    /** Alle regels, met naam, voor de baseline- en bewijstest. */
    fun all(): Map<String, ArchRule> =
        linkedMapOf(
            "configurationProperties" to noConfigurationPropertiesOutsideAutoconfig(),
            "webClientFactory" to noWebClientFactoryOutsideCore(),
            "threadLocal" to noThreadLocal(),
            "inMemorySecretKeyStore" to noInMemorySecretKeyStore(),
            "openFeatureSingleton" to noOpenFeatureSingletonOutsideFeatureflag(),
            "globalScope" to noGlobalScope(),
            "runBlocking" to noRunBlocking(),
            "newCoroutineScope" to noNewCoroutineScope(),
        )
}