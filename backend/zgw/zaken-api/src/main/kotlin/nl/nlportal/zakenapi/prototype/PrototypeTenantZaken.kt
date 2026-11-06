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
package nl.nlportal.zakenapi.prototype

import kotlinx.coroutines.reactor.awaitSingle
import nl.nlportal.catalogiapi.client.CatalogiApiConfig.CatalogiApiConfigProperties
import nl.nlportal.core.tenancy.ModulePermission
import nl.nlportal.core.tenancy.TenantDirectory
import nl.nlportal.core.tenancy.TenantRegistry
import nl.nlportal.core.util.requireConfigured
import nl.nlportal.zakenapi.client.ZakenApiClient
import nl.nlportal.zakenapi.client.ZakenApiConfig.ZakenApiConfigProperties
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.function.client.WebClient
import java.util.UUID

/**
 * PROTOTYPE (wayfinder #204, weggooibaar, nooit mergen). Minimale, representatieve slice van zakenapi:
 * de client komt uit het [TenantRegistry] van de huidige tenant in plaats van uit een singleton-bean.
 */
class PrototypeTenantZaakService(
    private val clients: TenantRegistry<ZakenApiClient>,
) {
    /** Haalt de ruwe zaak-JSON op bij de Zaken API van de tenant van dit request. */
    suspend fun zaakJson(id: UUID): String =
        clients
            .current()
            .webClient
            .get()
            .uri("/zaken/api/v1/zaken/{id}", id)
            .retrieve()
            .bodyToMono(String::class.java)
            .awaitSingle()
}

@AutoConfiguration
@ConditionalOnProperty(prefix = "nl-portal.tenancy", name = ["enabled"], havingValue = "true")
class PrototypeZakenApiTenancyAutoConfiguration {
    @Bean
    fun zakenApiClientRegistry(
        directory: TenantDirectory,
        permission: ModulePermission,
        webClientBuilder: WebClient.Builder,
    ): TenantRegistry<ZakenApiClient> =
        TenantRegistry.build("zakenapi", directory, permission) { settings ->
            val properties =
                ZakenApiConfigProperties().apply {
                    url = settings["url"].requireConfigured("zakenapi", "url")
                    clientId = settings["client-id"].requireConfigured("zakenapi", "client-id")
                    secret = settings["secret"].requireConfigured("zakenapi", "secret")
                }
            ZakenApiClient(properties, CatalogiApiConfigProperties(), webClientBuilder).also { it.webClient } // WebClient nu bouwen, niet lazy
        }

    @Bean
    fun prototypeTenantZaakService(registry: TenantRegistry<ZakenApiClient>) = PrototypeTenantZaakService(registry)
}