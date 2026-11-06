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

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * PROTOTYPE (wayfinder #204). Bindt `nl-portal.tenants.<oin>...` en `nl-portal.tenancy...`.
 * Alleen de autoconfiguratie mag deze klasse kennen; services lezen via [TenantRegistry].
 *
 * Let op: OIN-sleutels in YAML moeten tussen aanhalingstekens (`"00000001003214345000":`), anders
 * leest YAML ze als getal en verdwijnen de voorloopnullen.
 */
@ConfigurationProperties(prefix = "nl-portal")
class TenancyProperties {
    var tenancy: Tenancy = Tenancy()
    var tenants: Map<String, TenantProperties> = emptyMap()

    class Tenancy {
        var enabled: Boolean = false

        /** Issuers moeten onder deze basis-URL vallen (publieke broker); de private broker valt er structureel buiten. */
        var publicBrokerBaseUrl: String? = null
    }

    class TenantProperties {
        var domains: List<String> = emptyList()
        var issuers: List<String> = emptyList()

        /** Per module een platte instellingenmap; `permission: true` is de stub voor de permission-flag. */
        var modules: Map<String, Map<String, String>> = emptyMap()

        fun toDefinition() = TenantDefinition(domains, issuers, modules)
    }

    fun definitions(): Map<String, TenantDefinition> = tenants.mapValues { it.value.toDefinition() }
}