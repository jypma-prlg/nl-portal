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

import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClasses

/** What in a module stands in the way of one runtime serving several tenants (research #146). */
enum class TenancySignal(
    val short: String,
    val label: String,
) {
    CONFIG_SINGLETON("config", "configuratie is een singleton buiten de autoconfig"),
    OWN_WEBCLIENT("webclient", "bouwt zelf een WebClient"),
    GLOBAL_STATE("globaal", "gebruikt JVM-globale toestand"),
}

object TenancySignals {
    private const val CONFIGURATION_PROPERTIES = "org.springframework.boot.context.properties.ConfigurationProperties"
    private const val WEB_CLIENT = "org.springframework.web.reactive.function.client.WebClient"
    private val globalStateTypes =
        setOf(
            "java.lang.ThreadLocal",
            "kotlinx.coroutines.GlobalScope",
            "com.onlinepayments.webhooks.InMemorySecretKeyStore",
            "dev.openfeature.sdk.OpenFeatureAPI",
        )

    fun of(
        classes: JavaClasses,
        mappings: List<ModuleMapping>,
    ): Map<String, Set<TenancySignal>> {
        val result = linkedMapOf<String, MutableSet<TenancySignal>>()
        for (source in classes) {
            val mapping =
                mappings
                    .filter { it.owns(source.packageName) }
                    .maxByOrNull { it.packagePrefix.length } ?: continue
            val found = signalsOf(source, mapping)
            if (found.isNotEmpty()) result.getOrPut(mapping.modules.first()) { linkedSetOf() }.addAll(found)
        }
        return result.toSortedMap()
    }

    private fun signalsOf(
        source: JavaClass,
        mapping: ModuleMapping,
    ): Set<TenancySignal> {
        val found = linkedSetOf<TenancySignal>()
        if (mapping.layer != Layer.CORE &&
            source.methodCallsFromSelf.any { it.targetOwner.name == WEB_CLIENT && it.name in setOf("create", "builder") }
        ) {
            found += TenancySignal.OWN_WEBCLIENT
        }
        if (source.directDependenciesFromSelf.any { it.targetClass.name in globalStateTypes }) {
            found += TenancySignal.GLOBAL_STATE
        }
        if (!isConfigurationSide(source) &&
            source.directDependenciesFromSelf.any { isConfigurationProperties(it.targetClass) }
        ) {
            found += TenancySignal.CONFIG_SINGLETON
        }
        return found
    }

    private fun isConfigurationProperties(javaClass: JavaClass): Boolean =
        javaClass.isAnnotatedWith(CONFIGURATION_PROPERTIES) ||
            javaClass.enclosingClass.map { isConfigurationProperties(it) }.orElse(false)

    /** The configuration classes themselves and the autoconfiguration that wires them are not consumers. */
    private fun isConfigurationSide(javaClass: JavaClass): Boolean = isConfigurationProperties(javaClass) || javaClass.packageName.split('.').contains("autoconfigure")
}