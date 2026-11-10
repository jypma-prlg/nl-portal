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
package nl.nlportal.besluiten.service

import nl.nlportal.besluiten.client.BesluitenApiClient
import nl.nlportal.besluiten.domain.Besluit
import nl.nlportal.besluiten.domain.BesluitAuditTrail
import java.util.UUID
import nl.nlportal.commonground.authentication.CommonGroundAuthentication
import nl.nlportal.documentenapi.domain.Document
import nl.nlportal.documentenapi.service.DocumentenApiService

class BesluitenService(
    private val besluitenApiClient: BesluitenApiClient,
    private val documentenApiService: DocumentenApiService,
) {
    suspend fun getBesluiten(
        besluitType: String? = null,
        identificatie: String? = null,
        page: Int? = 1,
        verantwoordelijkeOrganisatie: String? = null,
        zaak: String? = null,
    ): List<Besluit> {
        val besluiten =
            besluitenApiClient.getBesluiten(
                besluitType = besluitType,
                identificatie = identificatie,
                page = page,
                verantwoordelijkeOrganisatie = verantwoordelijkeOrganisatie,
                zaak = zaak,
            )

        return besluiten
    }

    suspend fun getBesluitAuditTrails(besluitId: UUID): List<BesluitAuditTrail> = besluitenApiClient.getBesluitAuditTrails(besluitId).sortedBy { it.aanmaakdatum }

    suspend fun getBesluitDocumenten(
        authentication: CommonGroundAuthentication,
        besluit: String,
        informatieobject: String? = null,
    ): List<Document> {
        val besluitDocuments =
            besluitenApiClient.getBesluitDocumenten(
                besluit = besluit,
                informatieobject = informatieobject,
            )
        return documentenApiService.filterDocuments(
            besluitDocuments.map {
                documentenApiService
                    .getDocument(it.informatieobject)
                    .copy(identificatie = authentication.userId)
            },
        )
    }
}