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

class InvalidTenantConfigurationException(
    message: String,
) : IllegalStateException(message)

/**
 * PROTOTYPE (wayfinder #204). Onveranderlijk domeinmodel van een tenant. De autoconfiguratie zet de
 * `@ConfigurationProperties` hierin om, zodat geen enkele service de properties-klassen hoeft te kennen.
 */
data class TenantDefinition(
    val domains: List<String>,
    val issuers: List<String>,
    val modules: Map<String, Map<String, String>> = emptyMap(),
)

/**
 * PROTOTYPE (wayfinder #204). Onveranderlijke allowlist domein -> OIN en issuer -> OIN, bij opstarten gebouwd.
 * Een dubbel domein, dubbele issuer, tenant zonder domein/issuer of issuer buiten de publieke broker weigert het opstarten.
 */
class TenantDirectory private constructor(
    val tenants: Map<Oin, TenantDefinition>,
    private val domains: Map<String, Oin>,
    private val issuers: Map<String, Oin>,
) {
    fun tenantForHost(host: String): Oin? = domains[host.lowercase()]

    fun tenantForIssuer(issuer: String?): Oin? = issuer?.let { issuers[it] }

    companion object {
        fun from(
            tenants: Map<String, TenantDefinition>,
            publicBrokerBaseUrl: String? = null,
        ): TenantDirectory {
            if (tenants.isEmpty()) throw InvalidTenantConfigurationException("Geen tenants geconfigureerd onder nl-portal.tenants")
            val domains = mutableMapOf<String, Oin>()
            val issuers = mutableMapOf<String, Oin>()
            val byOin = mutableMapOf<Oin, TenantDefinition>()
            tenants.forEach { (key, tenant) ->
                val oin = Oin(key)
                if (tenant.domains.isEmpty()) throw InvalidTenantConfigurationException("Tenant $oin heeft geen domein")
                if (tenant.issuers.isEmpty()) throw InvalidTenantConfigurationException("Tenant $oin heeft geen issuer")
                tenant.domains.map { it.lowercase() }.forEach { domain ->
                    domains.put(domain, oin)?.let {
                        throw InvalidTenantConfigurationException("Domein '$domain' hoort bij meerdere tenants ($it en $oin)")
                    }
                }
                tenant.issuers.forEach { issuer ->
                    if (publicBrokerBaseUrl != null && !issuer.startsWith(publicBrokerBaseUrl)) {
                        throw InvalidTenantConfigurationException("Issuer van tenant $oin valt niet onder de publieke broker")
                    }
                    issuers.put(issuer, oin)?.let {
                        throw InvalidTenantConfigurationException("Issuer '$issuer' hoort bij meerdere tenants ($it en $oin)")
                    }
                }
                byOin[oin] = tenant
            }
            return TenantDirectory(byOin.toMap(), domains.toMap(), issuers.toMap())
        }
    }
}