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
package nl.nlportal.core.tenancy

import nl.nlportal.core.util.ModuleNotConfiguredException
import reactor.core.publisher.Mono

class ModuleNotAvailableException(
    module: String,
    oin: Oin,
) : IllegalStateException("Module '$module' is niet beschikbaar voor tenant $oin")

/** STUB voor de permission-flag (OpenFeature, standaard uit): beschikbaar = configuratie aanwezig én flag aan. */
fun interface ModulePermission {
    fun isGranted(
        oin: Oin,
        module: String,
    ): Boolean
}

/**
 * PROTOTYPE (wayfinder #204). Onveranderlijk per-tenant clientregister, bij opstarten gebouwd uit
 * `nl-portal.tenants.<oin>.modules.<module>`. Aanroepers gebruiken [current] (leest de [TenantContext], fail-closed).
 */
class TenantRegistry<T : Any> private constructor(
    val module: String,
    private val entries: Map<Oin, T>,
    private val permission: ModulePermission,
) {
    suspend fun current(): T = forTenant(TenantContext.current())

    fun currentMono(): Mono<T> = TenantContext.currentMono().map(::forTenant)

    fun forTenant(oin: Oin): T {
        val entry = entries[oin]
        if (entry == null || !permission.isGranted(oin, module)) throw ModuleNotAvailableException(module, oin)
        return entry
    }

    companion object {
        /**
         * Bouwt een entry voor elke tenant met configuratie voor [module]. Een onvolledige configuratie
         * (factory gooit [ModuleNotConfiguredException]) weigert het opstarten, met OIN in de melding.
         */
        fun <T : Any> build(
            module: String,
            directory: TenantDirectory,
            permission: ModulePermission,
            factory: (Map<String, String>) -> T,
        ): TenantRegistry<T> {
            val entries =
                directory.tenants.mapNotNull { (oin, tenant) ->
                    tenant.modules[module]?.let { settings ->
                        try {
                            oin to factory(settings)
                        } catch (e: ModuleNotConfiguredException) {
                            throw InvalidTenantConfigurationException("Tenant $oin: ${e.message}")
                        }
                    }
                }
            return TenantRegistry(module, entries.toMap(), permission)
        }
    }
}