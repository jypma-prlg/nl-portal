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

/** An architecture rule that the multitenancy research (#146 §6) proposes; [methodName] is the method it gets in [ArchitectureRules]. */
data class ProposedRule(
    val methodName: String,
    val description: String,
)

data class RuleStatus(
    val rule: ProposedRule,
    val active: Boolean,
)

object ProposedRules {
    val all =
        listOf(
            ProposedRule("noConfigurationPropertiesOutsideAutoconfig", "configuratie alleen via het register, niet geïnjecteerd buiten de autoconfig"),
            ProposedRule("noOwnWebClient", "geen WebClient.create()/builder() buiten core en zgw-client"),
            ProposedRule("noJvmGlobalState", "geen JVM-globale stores (InMemorySecretKeyStore, ThreadLocal, OpenFeatureAPI buiten core.featureflag)"),
            ProposedRule("noNewCoroutineScopes", "geen GlobalScope, CoroutineScope(...) of runBlocking: de tenant valt dan uit de Reactor Context"),
            ProposedRule("tenantRegistryComplete", "elke tenant levert bij het opstarten een volledig register op"),
        )

    fun status(existingRuleNames: Set<String>): List<RuleStatus> = all.map { RuleStatus(it, it.methodName in existingRuleNames) }

    /** Rules that exist today: the public methods of [ArchitectureRules] that return an ArchRule. */
    fun existingRuleNames(): Set<String> =
        ArchitectureRules::class.java.methods
            .filter { com.tngtech.archunit.lang.ArchRule::class.java.isAssignableFrom(it.returnType) }
            .map { it.name }
            .toSet()
}