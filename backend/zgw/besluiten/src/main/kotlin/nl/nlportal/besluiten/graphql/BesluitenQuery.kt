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
package nl.nlportal.besluiten.graphql

import nl.nlportal.besluiten.domain.Besluit
import nl.nlportal.besluiten.domain.BesluitAuditTrail
import nl.nlportal.besluiten.domain.BesluitDocument
import nl.nlportal.besluiten.service.BesluitenService
import nl.nlportal.catalogiapi.domain.BesluitType
import nl.nlportal.catalogiapi.service.CatalogiApiService
import nl.nlportal.commonground.authentication.CommonGroundAuthentication
import nl.nlportal.core.util.ZgwUrls
import nl.nlportal.documentenapi.domain.Document
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class BesluitenQuery(
    val besluitenService: BesluitenService,
    val catalogiApiService: CatalogiApiService,
) {
    @SchemaMapping(typeName = "Besluit", field = "audittrails")
    suspend fun auditTrails(
        authentication: CommonGroundAuthentication,
        besluit: Besluit,
    ): List<BesluitAuditTrail> = besluitenService.getBesluitAuditTrails(ZgwUrls.extractId(besluit.url))

    @SchemaMapping(typeName = "Besluit", field = "documenten")
    suspend fun documenten(
        authentication: CommonGroundAuthentication,
        besluit: Besluit,
    ): List<Document> =
        besluitenService.getBesluitDocumenten(
            authentication = authentication,
            besluit = besluit.url,
        )

    @SchemaMapping(typeName = "Besluit", field = "besluittype")
    suspend fun besluittype(
        authentication: CommonGroundAuthentication,
        besluit: Besluit,
    ): BesluitType =
        catalogiApiService.getBesluitType(
            besluitTypeUrl = besluit.besluittype,
        )
}