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

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

/**
 * PROTOTYPE (wayfinder #204). Staat achter `nl-portal.tenancy.enabled=true` zodat bestaande applicaties en
 * tests ongemoeid blijven.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "nl-portal.tenancy", name = ["enabled"], havingValue = "true")
@EnableConfigurationProperties(TenancyProperties::class)
class TenancyAutoConfiguration {
    @Bean
    fun tenantDirectory(properties: TenancyProperties) = TenantDirectory.from(properties.definitions(), properties.tenancy.publicBrokerBaseUrl)

    @Bean
    fun tenantWebFilter(directory: TenantDirectory) = TenantWebFilter(directory)

    @Bean
    fun tenantIssuerConsistencyFilter(directory: TenantDirectory) = TenantIssuerConsistencyFilter(directory)

    /** STUB: leest `permission: true` uit de modulemap van de tenant. */
    @Bean
    @ConditionalOnMissingBean(ModulePermission::class)
    fun modulePermission(directory: TenantDirectory) =
        ModulePermission { oin, module ->
            directory.tenants[oin]
                ?.modules
                ?.get(module)
                ?.get("permission") == "true"
        }
}