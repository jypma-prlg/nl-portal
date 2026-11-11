/*
 * Copyright 2015-2026 Den Haag, Ritense, the Netherlands.
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
package nl.nlportal.besluiten.client

import nl.nlportal.core.util.requireConfigured
import nl.nlportal.besluiten.client.BesluitenApiConfig.BesluitenApiConfigProperties
import nl.nlportal.besluiten.domain.Besluit
import nl.nlportal.besluiten.domain.BesluitAuditTrail
import nl.nlportal.besluiten.domain.BesluitDocument
import nl.nlportal.besluiten.domain.ResultPage
import nl.nlportal.core.koppeling.ZgwJwtGenerator
import org.springframework.http.HttpStatus
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import java.util.UUID

class BesluitenApiClient(
    private val besluitenApiConfig: BesluitenApiConfigProperties,
    webClientBuilder: WebClient.Builder,
) {
    val webClient: WebClient by lazy {
        webClientBuilder
            .clone()
            .baseUrl(besluitenApiConfig.url.requireConfigured("besluitenapi", "url"))
            .filter(
                ExchangeFilterFunction.ofRequestProcessor {
                    Mono.just(
                        ClientRequest.from(it).header("Authorization", "Bearer ${getToken()}").build(),
                    )
                },
            ).defaultHeader("Accept-Crs", "EPSG:4326")
            .defaultHeader("Content-Crs", "EPSG:4326")
            .build()
    }

    suspend fun getBesluiten(
        besluitType: String?,
        identificatie: String?,
        page: Int?,
        verantwoordelijkeOrganisatie: String?,
        zaak: String?,
    ): List<Besluit> {
        val params = LinkedMultiValueMap<String, String>()
        besluitType?.let { params.add("besluittype", it) }
        identificatie?.let { params.add("identificatie", it) }
        page?.let { params.add("page", it.toString()) }
        verantwoordelijkeOrganisatie?.let { params.add("verantwoordelijkeOrganisatie", it) }
        zaak?.let { params.add("zaak", it) }
        return webClient
            .get()
            .uri {
                it
                    .path("/besluiten/api/v1/besluiten")
                    .queryParams(params)
                    .build()
            }.retrieve()
            .handleStatus()
            .awaitBody<ResultPage<Besluit>>()
            .results
    }

    suspend fun getBesluitAuditTrails(besluitId: UUID): List<BesluitAuditTrail> =
        webClient
            .get()
            .uri("/besluiten/api/v1/besluiten/$besluitId/audittrail")
            .retrieve()
            .handleStatus()
            .awaitBody<List<BesluitAuditTrail>>()

    suspend fun getBesluitAuditTrail(
        besluitId: UUID,
        auditTrailId: UUID,
    ): BesluitAuditTrail =
        webClient
            .get()
            .uri("/besluiten/api/v1/besluiten/$besluitId/audittrail/$auditTrailId")
            .retrieve()
            .handleStatus()
            .awaitBody<BesluitAuditTrail>()

    suspend fun getBesluitDocumenten(
        besluit: String,
        informatieobject: String?,
    ): List<BesluitDocument> {
        val params = LinkedMultiValueMap<String, String>()
        params.add("besluit", besluit)
        informatieobject?.let { params.add("informatieobject", it) }
        return webClient
            .get()
            .uri {
                it
                    .path("/besluiten/api/v1/besluitinformatieobjecten")
                    .queryParams(params)
                    .build()
            }.retrieve()
            .handleStatus()
            .awaitBody<List<BesluitDocument>>()
    }

    private fun getToken(): String =
        ZgwJwtGenerator().generateToken(
            besluitenApiConfig.secret.requireConfigured("besluitenapi", "secret"),
            besluitenApiConfig.clientId.requireConfigured("besluitenapi", "clientId"),
        )

    fun WebClient.ResponseSpec.handleStatus() =
        this
            .onStatus(
                { httpStatus -> HttpStatus.NOT_FOUND == httpStatus },
                { throw ResponseStatusException(HttpStatus.NOT_FOUND) },
            ).onStatus(
                { httpStatus -> HttpStatus.UNAUTHORIZED == httpStatus },
                { throw ResponseStatusException(HttpStatus.UNAUTHORIZED) },
            ).onStatus(
                { httpStatus -> HttpStatus.INTERNAL_SERVER_ERROR == httpStatus },
                {
                    throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)
                },
            )
}