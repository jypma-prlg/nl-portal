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
import nl.nlportal.besluiten.domain.BesluitDocument
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import nl.nlportal.commonground.authentication.CommonGroundAuthentication
import nl.nlportal.core.util.ZgwUrls.extractId
import nl.nlportal.documentenapi.domain.Document
import nl.nlportal.documentenapi.service.DocumentenApiService
import nl.nlportal.zakenapi.service.ZakenApiService
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class BesluitenService(
    private val besluitenApiClient: BesluitenApiClient,
    private val documentenApiService: DocumentenApiService,
    private val zakenApiService: ZakenApiService,
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

    suspend fun getBesluitDocumentContent(
        authentication: CommonGroundAuthentication,
        besluitId: UUID,
        documentId: UUID,
    ): Pair<Document, Flow<DataBuffer>> {
        val besluit =
            besluitenApiClient.getBesluit(
                besluitId = besluitId,
            )

        if (besluit.zaak.isNullOrBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Besluit is not related to a Zaak")
        }

        // get the zaak to check if authenticated user is authorized for zaak
        zakenApiService.getZaak(
            authentication = authentication,
            id = extractId(besluit.zaak),
        )

        val besluitDocument = besluitenApiClient.getBesluitDocument(documentId)
        if (besluitDocument.besluit != besluit.url) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Besluit document is not related to besluit")
        }

        val document = documentenApiService.getDocument(besluitDocument.url)
        val content = documentenApiService.getDocumentContentStreaming(besluitDocument.url)
        return document to content
    }

    suspend fun getBesluitDocument(documentId: UUID): BesluitDocument = besluitenApiClient.getBesluitDocument(documentId)
}