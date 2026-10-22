/*
 * Copyright 2015-2025 Den Haag, Ritense, the Netherlands.
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
package nl.nlportal.openproduct.service

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import nl.nlportal.commonground.authentication.AuthenticationMachtigingsDienstService
import nl.nlportal.commonground.authentication.BedrijfAuthentication
import nl.nlportal.commonground.authentication.BurgerAuthentication
import nl.nlportal.commonground.authentication.CommonGroundAuthentication
import nl.nlportal.core.util.CoreUtils
import nl.nlportal.core.util.CoreUtils.extractId
import nl.nlportal.documentenapi.domain.Document
import nl.nlportal.documentenapi.service.DocumentenApiService
import nl.nlportal.openproduct.client.OpenProductClient
import nl.nlportal.openproduct.client.OpenProductTypeClient
import nl.nlportal.openproduct.client.domain.OpenProductActie
import nl.nlportal.openproduct.client.domain.OpenProductActiesFilters
import nl.nlportal.openproduct.client.domain.OpenProductBestand
import nl.nlportal.openproduct.client.domain.OpenProductBestandenFilters
import nl.nlportal.openproduct.client.domain.OpenProductContact
import nl.nlportal.openproduct.client.domain.OpenProductContactenFilters
import nl.nlportal.openproduct.client.domain.OpenProductContentElement
import nl.nlportal.openproduct.client.domain.OpenProductLink
import nl.nlportal.openproduct.client.domain.OpenProductLinksFilters
import nl.nlportal.openproduct.client.domain.OpenProductLocatie
import nl.nlportal.openproduct.client.domain.OpenProductLocatiesFilters
import nl.nlportal.openproduct.client.domain.OpenProductOrganisatie
import nl.nlportal.openproduct.client.domain.OpenProductOrganisatiesFilters
import nl.nlportal.openproduct.client.domain.OpenProductPrijs
import nl.nlportal.openproduct.client.domain.OpenProductPrijzenFilters
import nl.nlportal.openproduct.client.domain.OpenProductProduct
import nl.nlportal.openproduct.client.domain.OpenProductProductType
import nl.nlportal.openproduct.client.domain.OpenProductProductTypesFilters
import nl.nlportal.openproduct.client.domain.OpenProductProductUpdate
import nl.nlportal.openproduct.client.domain.OpenProductProductenFilters
import nl.nlportal.openproduct.client.domain.OpenProductSchema
import nl.nlportal.openproduct.client.domain.OpenProductSchemasFilters
import nl.nlportal.openproduct.client.domain.OpenProductThema
import nl.nlportal.openproduct.client.domain.OpenProductThemasFilters
import nl.nlportal.openproduct.client.domain.OpenProductToegestaneStatus
import nl.nlportal.openproduct.client.domain.OpenProductUrl
import nl.nlportal.openproduct.client.domain.ResultPage
import nl.nlportal.openproduct.client.path.Acties
import nl.nlportal.openproduct.client.path.Bestanden
import nl.nlportal.openproduct.client.path.Contacten
import nl.nlportal.openproduct.client.path.Links
import nl.nlportal.openproduct.client.path.Locaties
import nl.nlportal.openproduct.client.path.Organisaties
import nl.nlportal.openproduct.client.path.Prijzen
import nl.nlportal.openproduct.client.path.ProductTypes
import nl.nlportal.openproduct.client.path.Producten
import nl.nlportal.openproduct.client.path.Schemas
import nl.nlportal.openproduct.client.path.Themas
import nl.nlportal.openproduct.graphql.domain.OpenProductThemaHierarchy
import nl.nlportal.zakenapi.domain.Zaak
import nl.nlportal.zakenapi.service.ZakenApiService
import nl.nlportal.zgw.objectenapi.client.ObjectsApiClient
import nl.nlportal.zgw.objectenapi.domain.Comparator
import nl.nlportal.zgw.objectenapi.domain.ObjectSearchParameter
import nl.nlportal.zgw.objectenapi.domain.ObjectsApiObject
import nl.nlportal.zgw.taak.autoconfigure.TaakConfig.TaakConfigProperties
import nl.nlportal.zgw.taak.domain.TaakObjectV2
import nl.nlportal.zgw.taak.domain.TaakV2
import nl.nlportal.zgw.taak.graphql.TaakPageV2
import nl.nlportal.zgw.taak.service.TaakService
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

@Suppress("UNCHECKED_CAST")
class OpenProductService(
    private val openProductClient: OpenProductClient,
    private val openProductTypeClient: OpenProductTypeClient,
    private val objectsApiTaskConfigProperties: TaakConfigProperties,
    private val zakenApiService: ZakenApiService,
    private val authenticationMachtigingsDienstService: AuthenticationMachtigingsDienstService,
    private val documentenApiService: DocumentenApiService,
    private val taakService: TaakService,
) {
    /**
     * Get published themas
     * @param: pageNumber, number for pagination
     * @param: pageSize, size for pagination
     * @param: extraSearchVariables, extra search variables to extend the search
     * @return: Result of themas
     */
    suspend fun getThemas(
        authentication: CommonGroundAuthentication,
        pageNumber: Int,
        pageSize: Int,
        extraSearchVariables: List<Pair<OpenProductThemasFilters, Any>> = emptyList(),
    ): ResultPage<OpenProductThema> {
        val searchVariables =
            mutableListOf<Pair<OpenProductThemasFilters, Any>>(
                OpenProductThemasFilters.PAGE to pageNumber.toString(),
                OpenProductThemasFilters.PAGE_SIZE to pageSize.toString(),
                OpenProductThemasFilters.GEPUBLICEERD to true,
            )

        if (extraSearchVariables.isNotEmpty()) {
            searchVariables.addAll(extraSearchVariables)
        }

        authenticationMachtigingsDienstService.productTypes(authentication)?.let {
            searchVariables.add(OpenProductThemasFilters.PRODUCTTYPEN_UUID_IN to it.joinToString(","))
        }

        return openProductTypeClient.path<Themas>().get(searchVariables)
    }

    /**
     * Get only published hoofd themas
     * @return: List of hoofd themas
     */
    suspend fun getHoofdThemas(
        authentication: CommonGroundAuthentication,
    ): List<OpenProductThema> =
        getThemas(
            authentication = authentication,
            pageNumber = 1,
            pageSize = 999,
        ).results.filter { it.hoofdThema == null }

    /**
     * Get published hoofd themas based of the products of the authenticated user
     * @param: authentication, authenticated user
     * @return: List of themas
     */
    suspend fun getHoofdThemasByProducten(
        authentication: CommonGroundAuthentication,
    ): List<OpenProductThema> {
        val producten =
            getProducten(
                authentication = authentication,
                pageNumber = 1,
                pageSize = 999,
            ).results

        if (producten.isEmpty()) {
            return emptyList()
        }

        val hoofdThemas = mutableSetOf<OpenProductThema>()
        val themas =
            getThemas(
                authentication = authentication,
                pageNumber = 1,
                pageSize = 999,
            ).results

        // loop through producten to get via the producttypen the hoofdthemas
        producten.forEach { product ->
            themas
                .filter { thema ->
                    thema.producttypen.any { productType ->
                        productType.uuid == product.producttype.uuid
                    }
                }.forEach { thema ->
                    collectThemaHierarchyUpFromSubThema(
                        themaId = thema.uuid,
                        themas = themas,
                    ).filter { collectThemaHierarchyUpFromSubThema -> collectThemaHierarchyUpFromSubThema.hoofdThema == null }
                        .let { hoofdThemaList ->
                            hoofdThemas.addAll(hoofdThemaList)
                        }
                }
        }

        return hoofdThemas.toList()
    }

    /**
     * Get hierarchy of all published themas
     * @return: List of thema hierarchy
     */
    suspend fun getThemasHierarchy(
        authentication: CommonGroundAuthentication,
    ): List<OpenProductThemaHierarchy> {
        val themasHierarchy = mutableListOf<OpenProductThemaHierarchy>()
        val themas =
            getThemas(
                authentication = authentication,
                pageNumber = 1,
                pageSize = 999,
            ).results
        val hoofdThemas = themas.filter { it.hoofdThema == null }

        hoofdThemas.forEach {
            themasHierarchy.add(
                searchSubThemasHierarchy(
                    thema = it,
                    themas = themas,
                ),
            )
        }

        return themasHierarchy
    }

    /**
     * Get thema
     * @param: id, uuid of the thema
     * @return: thema of null (an upstream not-found becomes null; every other failure propagates)
     */
    suspend fun getThema(
        authentication: CommonGroundAuthentication,
        id: UUID,
    ): OpenProductThema? {
        val thema =
            notFoundAsNull {
                openProductTypeClient.path<Themas>().get(id = id)
            } ?: return null

        if (!authenticationMachtigingsDienstService.isAllowedProductTypes(
                authentication = authentication,
                productTypeUUIDs = thema.producttypen.map { it.uuid },
            )
        ) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access denied to this thema")
        }

        return thema
    }

    /**
     * Get thema hierarchy only for the requested thema
     * @param: id, uuid of thema
     * @return: List of thema hierarchy
     */
    suspend fun getThemaHierarchy(
        authentication: CommonGroundAuthentication,
        id: UUID,
    ): List<OpenProductThemaHierarchy> {
        val thema =
            notFoundAsNull {
                openProductTypeClient.path<Themas>().get(id = id)
            } ?: return emptyList()

        return buildThemaHierachy(
            authentication = authentication,
            thema = thema,
        )
    }

    /**
     * Get productTypes
     * @param: pageNumber, number for pagination
     * @param: pageSize, size for pagination
     * @param: language, language of the producttype, default is NL
     * @param: extraSearchVariables, extra search variables to extend the search
     * @return: Result of productTypes
     */
    suspend fun getProductTypes(
        pageNumber: Int,
        pageSize: Int,
        language: String? = null,
        extraSearchVariables: List<Pair<OpenProductProductTypesFilters, Any>> = emptyList(),
    ): ResultPage<OpenProductProductType> {
        val searchVariables =
            mutableListOf<Pair<OpenProductProductTypesFilters, Any>>(
                OpenProductProductTypesFilters.PAGE to pageNumber.toString(),
                OpenProductProductTypesFilters.PAGE_SIZE to pageSize.toString(),
                OpenProductProductTypesFilters.GEPUBLICEERD to true,
            )

        if (extraSearchVariables.isNotEmpty()) {
            searchVariables.addAll(extraSearchVariables)
        }
        return openProductTypeClient.path<ProductTypes>().get(
            searchFilters = searchVariables,
            language = language,
        )
    }

    /**
     * Get producttype
     * @param: id, uuid of the producttype
     * @param: language, language of productype, is optional
     * @return: Producttype or null (an upstream not-found becomes null; every other failure propagates)
     */
    suspend fun getProductType(
        id: UUID,
        language: String? = null,
    ): OpenProductProductType? =
        notFoundAsNull {
            openProductTypeClient.path<ProductTypes>().get(
                id = id,
                language = language,
            )
        }

    /**
     * Get acties
     * @param: pageNumber, number for pagination
     * @param: pageSize, size for pagination
     * @param: extraSearchVariables, extra search variables to extend the search
     * @return: Result of acties
     */
    suspend fun getActies(
        pageNumber: Int,
        pageSize: Int,
        extraSearchVariables: List<Pair<OpenProductActiesFilters, Any>> = emptyList(),
    ): ResultPage<OpenProductActie> {
        val searchVariables =
            mutableListOf<Pair<OpenProductActiesFilters, Any>>(
                OpenProductActiesFilters.PAGE to pageNumber.toString(),
                OpenProductActiesFilters.PAGE_SIZE to pageSize.toString(),
            )

        if (extraSearchVariables.isNotEmpty()) {
            searchVariables.addAll(extraSearchVariables)
        }
        return openProductTypeClient.path<Acties>().get(searchVariables)
    }

    /**
     * Get actie
     * @param: id, uuid of actie
     * @return: actie or null (an upstream not-found becomes null; every other failure propagates)
     */
    suspend fun getActie(id: UUID): OpenProductActie? =
        notFoundAsNull {
            openProductTypeClient.path<Acties>().get(id = id)
        }

    /**
     * Get bestanden
     * @param: pageNumber, number for pagination
     * @param: pageSize, size for pagination
     * @param: naam, naam of bestand, is optional
     * @param: extraSearchVariables, extra search variables to extend the search
     * @return: Result of bestanden
     */
    suspend fun getBestanden(
        pageNumber: Int,
        pageSize: Int,
        naam: String? = null,
        extraSearchVariables: List<Pair<OpenProductBestandenFilters, Any>> = emptyList(),
    ): ResultPage<OpenProductBestand> {
        val searchVariables =
            mutableListOf<Pair<OpenProductBestandenFilters, Any>>(
                OpenProductBestandenFilters.PAGE to pageNumber.toString(),
                OpenProductBestandenFilters.PAGE_SIZE to pageSize.toString(),
            )

        naam?.let {
            searchVariables.add(OpenProductBestandenFilters.NAAM_CONTAINS to it)
        }

        if (extraSearchVariables.isNotEmpty()) {
            searchVariables.addAll(extraSearchVariables)
        }
        return openProductTypeClient.path<Bestanden>().get(searchVariables)
    }

    /**
     * Get bestand
     * @param: id, uuid of bestand
     * @return: bestand or null (an upstream not-found becomes null; every other failure propagates)
     */
    suspend fun getBestand(id: UUID): OpenProductBestand? =
        notFoundAsNull {
            openProductTypeClient.path<Bestanden>().get(id = id)
        }

    /**
     * Get conatcten
     * @param: pageNumber, number for pagination
     * @param: pageSize, size for pagination
     * @param: naam, naam of contact, is optional
     * @param: extraSearchVariables, extra search variables to extend the search
     * @return: Result of contacten
     */
    suspend fun getContacten(
        pageNumber: Int,
        pageSize: Int,
        naam: String? = null,
        extraSearchVariables: List<Pair<OpenProductContactenFilters, Any>> = emptyList(),
    ): ResultPage<OpenProductContact> {
        val searchVariables =
            mutableListOf<Pair<OpenProductContactenFilters, Any>>(
                OpenProductContactenFilters.PAGE to pageNumber.toString(),
                OpenProductContactenFilters.PAGE_SIZE to pageSize.toString(),
            )

        naam?.let {
            searchVariables.add(OpenProductContactenFilters.NAAM to it)
        }

        if (extraSearchVariables.isNotEmpty()) {
            searchVariables.addAll(extraSearchVariables)
        }
        return openProductTypeClient.path<Contacten>().get(searchVariables)
    }

    /**
     * Get contact
     * @param: id, uuid of contact
     * @return: contact or null (an upstream not-found becomes null; every other failure propagates)
     */
    suspend fun getContact(id: UUID): OpenProductContact? =
        notFoundAsNull {
            openProductTypeClient.path<Contacten>().get(id = id)
        }

    /**
     * Get locaties
     * @param: pageNumber, number for pagination
     * @param: pageSize, size for pagination
     * @param: naam, naam of locatie, is optional
     * @param: extraSearchVariables, extra search variables to extend the search
     * @return: Result of locaties
     */
    suspend fun getLocaties(
        pageNumber: Int,
        pageSize: Int,
        naam: String? = null,
        extraSearchVariables: List<Pair<OpenProductLocatiesFilters, Any>> = emptyList(),
    ): ResultPage<OpenProductLocatie> {
        val searchVariables =
            mutableListOf<Pair<OpenProductLocatiesFilters, Any>>(
                OpenProductLocatiesFilters.PAGE to pageNumber.toString(),
                OpenProductLocatiesFilters.PAGE_SIZE to pageSize.toString(),
            )

        naam?.let {
            searchVariables.add(OpenProductLocatiesFilters.NAAM_EXACT to it)
        }
        if (extraSearchVariables.isNotEmpty()) {
            searchVariables.addAll(extraSearchVariables)
        }
        return openProductTypeClient.path<Locaties>().get(searchVariables)
    }

    /**
     * Get locatie
     * @param: id, uuid of locatie
     * @return: locatie or null (an upstream not-found becomes null; every other failure propagates)
     */
    suspend fun getLocatie(id: UUID): OpenProductLocatie? =
        notFoundAsNull {
            openProductTypeClient.path<Locaties>().get(id = id)
        }

    /**
     * Get organisaties
     * @param: pageNumber, number for pagination
     * @param: pageSize, size for pagination
     * @param: naam, naam of organisatie, is optional
     * @param: extraSearchVariables, extra search variables to extend the search
     * @return: Result of organisaties
     */
    suspend fun getOrganisaties(
        pageNumber: Int,
        pageSize: Int,
        naam: String? = null,
        extraSearchVariables: List<Pair<OpenProductOrganisatiesFilters, Any>> = emptyList(),
    ): ResultPage<OpenProductOrganisatie> {
        val searchVariables =
            mutableListOf<Pair<OpenProductOrganisatiesFilters, Any>>(
                OpenProductOrganisatiesFilters.PAGE to pageNumber.toString(),
                OpenProductOrganisatiesFilters.PAGE_SIZE to pageSize.toString(),
            )

        naam?.let {
            searchVariables.add(OpenProductOrganisatiesFilters.NAAM_EXACT to it)
        }
        if (extraSearchVariables.isNotEmpty()) {
            searchVariables.addAll(extraSearchVariables)
        }
        return openProductTypeClient.path<Organisaties>().get(searchVariables)
    }

    /**
     * Get organisatie
     * @param: id, uuid of organisatie
     * @return: organisatie or null (an upstream not-found becomes null; every other failure propagates)
     */
    suspend fun getOrganisatie(id: UUID): OpenProductOrganisatie? =
        notFoundAsNull {
            openProductTypeClient.path<Organisaties>().get(id = id)
        }

    /**
     * Get prijzen
     * @param: pageNumber, number for pagination
     * @param: pageSize, size for pagination
     * @param: extraSearchVariables, extra search variables to extend the search
     * @return: Result of prijzen
     */
    suspend fun getPrijzen(
        pageNumber: Int,
        pageSize: Int,
        extraSearchVariables: List<Pair<OpenProductPrijzenFilters, Any>> = emptyList(),
    ): ResultPage<OpenProductPrijs> {
        val searchVariables =
            mutableListOf<Pair<OpenProductPrijzenFilters, Any>>(
                OpenProductPrijzenFilters.PAGE to pageNumber.toString(),
                OpenProductPrijzenFilters.PAGE_SIZE to pageSize.toString(),
            )
        if (extraSearchVariables.isNotEmpty()) {
            searchVariables.addAll(extraSearchVariables)
        }
        return openProductTypeClient.path<Prijzen>().get(searchVariables)
    }

    /**
     * Get prijs
     * @param: id, uuid if prijs
     * @return: prijs or null (an upstream not-found becomes null; every other failure propagates)
     */
    suspend fun getPrijs(id: UUID): OpenProductPrijs? =
        notFoundAsNull {
            openProductTypeClient.path<Prijzen>().get(id = id)
        }

    /**
     * Get schemas
     * @param: pageNumber, number for pagination
     * @param: pageSize, size for pagination
     * @param: extraSearchVariables, extra search variables to extend the search
     * @return: Result of schemas
     */
    suspend fun getSchemas(
        pageNumber: Int,
        pageSize: Int,
        extraSearchVariables: List<Pair<OpenProductSchemasFilters, Any>> = emptyList(),
    ): ResultPage<OpenProductSchema> {
        val searchVariables =
            mutableListOf<Pair<OpenProductSchemasFilters, Any>>(
                OpenProductSchemasFilters.PAGE to pageNumber.toString(),
                OpenProductSchemasFilters.PAGE_SIZE to pageSize.toString(),
            )
        if (extraSearchVariables.isNotEmpty()) {
            searchVariables.addAll(extraSearchVariables)
        }
        return openProductTypeClient.path<Schemas>().get(searchVariables)
    }

    /**
     * Get schema
     * @param: id, uuid of schema
     * @return: schema or null (an upstream not-found becomes null; every other failure propagates)
     */
    suspend fun getSchema(id: UUID): OpenProductSchema? =
        notFoundAsNull {
            openProductTypeClient.path<Schemas>().get(id = id)
        }

    /**
     * Get links
     * @param: pageNumber, number for pagination
     * @param: pageSize, size for pagination
     * @param: extraSearchVariables, extra search variables to extend the search
     * @return: Result of links
     */
    suspend fun getLinks(
        pageNumber: Int,
        pageSize: Int,
        extraSearchVariables: List<Pair<OpenProductLinksFilters, Any>> = emptyList(),
    ): ResultPage<OpenProductLink> {
        val searchVariables =
            mutableListOf<Pair<OpenProductLinksFilters, Any>>(
                OpenProductLinksFilters.PAGE to pageNumber.toString(),
                OpenProductLinksFilters.PAGE_SIZE to pageSize.toString(),
            )
        if (extraSearchVariables.isNotEmpty()) {
            searchVariables.addAll(extraSearchVariables)
        }
        return openProductTypeClient.path<Links>().get(searchVariables)
    }

    /**
     * Get link
     * @param: id, uuid of link
     * @return: link or null (an upstream not-found becomes null; every other failure propagates)
     */
    suspend fun getLink(id: UUID): OpenProductLink? =
        notFoundAsNull {
            openProductTypeClient.path<Links>().get(id = id)
        }

    /**
     * Get content of producttype
     * @param: productTypeId, producttype id
     * @return: List of producttype content (an upstream not-found becomes null; every other failure propagates)
     */
    suspend fun getProductTypeContent(productTypeId: UUID): List<OpenProductContentElement>? =
        notFoundAsNull {
            openProductTypeClient.path<ProductTypes>().get(id = productTypeId)
        }

    /**
     * Get producten
     * @param: pageNumber, number for pagination
     * @param: pageSize, size for pagination
     * @param: extraSearchVariables, extra search variables to extend the search
     * @param: status, status of product
     * @param: productTypeCode, producttype code
     * @param: productTypeId, producttype uuid
     * @return: Result of producten
     */
    suspend fun getProducten(
        authentication: CommonGroundAuthentication,
        pageNumber: Int,
        pageSize: Int,
        extraSearchVariables: List<Pair<OpenProductProductenFilters, String>> = emptyList(),
        status: OpenProductToegestaneStatus? = null,
        productTypeCode: String? = null,
        productTypeId: String? = null,
        productTypeCodes: List<String>? = null,
        productTypeIds: List<UUID>? = null,
    ): ResultPage<OpenProductProduct> {
        val searchVariables =
            mutableListOf(
                OpenProductProductenFilters.PAGE to pageNumber.toString(),
                OpenProductProductenFilters.PAGE_SIZE to pageSize.toString(),
                OpenProductProductenFilters.GEPUBLICEERD to true,
            )

        searchVariables.addAll(getEigenaarFilter(authentication))

        if (extraSearchVariables.isNotEmpty()) {
            searchVariables.addAll(extraSearchVariables)
        }

        status?.let {
            searchVariables.add(OpenProductProductenFilters.STATUS to it.toString().lowercase())
        }

        productTypeCode?.let {
            searchVariables.add(OpenProductProductenFilters.PRODUCTTYPE_CODE to it)
        }

        productTypeCodes?.let {
            searchVariables.add(OpenProductProductenFilters.PRODUCTTYPE_CODE_IN to it.joinToString(","))
        }

        productTypeId?.let {
            searchVariables.add(OpenProductProductenFilters.PRODUCTTYPE_UUID to it)
        }

        val allowedProductTypes =
            authenticationMachtigingsDienstService.filterAllowedProductTypes(
                authentication = authentication,
                productTypeUUIDs = productTypeIds,
            )

        if (allowedProductTypes.isNotEmpty()) {
            searchVariables.add(OpenProductProductenFilters.PRODUCTTYPE_UUID_IN to allowedProductTypes.joinToString(","))
        }

        return openProductClient.path<Producten>().get(
            searchFilters = searchVariables,
        )
    }

    /**
     * Get product
     * @param: id, uuid of product
     * @return: product or null (an upstream not-found becomes null; every other failure, including
     *   the authorization checks below, propagates)
     */
    suspend fun getProduct(
        authentication: CommonGroundAuthentication,
        id: UUID,
    ): OpenProductProduct? {
        val product =
            notFoundAsNull {
                openProductClient.path<Producten>().get(id = id)
            } ?: return null

        if (!isAuthorizedForProduct(authentication = authentication, product = product)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authorized")
        }

        if (!authenticationMachtigingsDienstService.isAllowedProductType(
                authentication = authentication,
                productTypeUUID = product.producttype.uuid,
            )
        ) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access denied to this thema")
        }

        return product
    }

    /**
     * Get producten by thema
     * @param: authentication, authenticated user
     * @param: themaId, uuid of the thema
     * @return: producten list of thema
     */
    suspend fun getProductenByThema(
        authentication: CommonGroundAuthentication,
        themaId: UUID,
    ): List<OpenProductProduct> {
        val thema =
            getThema(
                authentication = authentication,
                id = themaId,
            ) ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not find thema with id: $themaId")

        return getThemaProducten(
            authentication = authentication,
            thema = thema,
        )
    }

    /**
     * Get producten by thema
     * @param: authentication, authenticated user
     * @param: thema
     * @return: producten list of thema
     */
    suspend fun getThemaProducten(
        authentication: CommonGroundAuthentication,
        thema: OpenProductThema,
    ): List<OpenProductProduct> {
        val searchVariables =
            listOf(
                OpenProductProductenFilters.THEMA_UUID to thema.uuid.toString(),
            )
        return getProducten(
            authentication = authentication,
            pageNumber = 1,
            pageSize = 999,
            extraSearchVariables = searchVariables,
        ).results
    }

    suspend fun getThemaLinks(
        thema: OpenProductThema,
    ): List<OpenProductLink> {
        val links = mutableListOf<OpenProductLink>()

        thema.producttypen.forEach { productType ->
            val searchVariables =
                listOf<Pair<OpenProductLinksFilters, Any>>(
                    OpenProductLinksFilters.PAGE to 1,
                    OpenProductLinksFilters.PAGE_SIZE to 999,
                    OpenProductLinksFilters.PRODUCTTYPE_UUID to productType.uuid.toString(),
                )

            val linkResponse = openProductTypeClient.path<Links>().get(searchVariables)
            if (linkResponse.results.isNotEmpty()) {
                links.addAll(linkResponse.results)
            }
        }

        return links
    }

    /**
     * Update product verbruiksobject or dataobject
     * @param: authentication, authenticated user
     * @param: productUpdate, product to update
     * @return: updated product
     */
    suspend fun updateProduct(
        authentication: CommonGroundAuthentication,
        productUpdate: OpenProductProductUpdate,
    ): OpenProductProduct? {
        // get product, only to check if user authorized to perform this update
        getProduct(
            authentication = authentication,
            id = productUpdate.uuid,
        )

        return openProductClient.path<Producten>().patch(productUpdate = productUpdate)
    }

    /**
     * Get zaken of a thema
     * @param: authentication, authenticated user
     * @param: isOpen, boolean if a zaak is still open
     * @param: id, uuid of thema,
     * @param: language, language of the producttype, default is NL
     * @param: themaList, list of parent themas
     * @param: themas: all published themas
     * @return: list of zaken
     */
    suspend fun getThemaZaken(
        authentication: CommonGroundAuthentication,
        isOpen: Boolean? = null,
        id: UUID,
        language: String? = null,
        themasList: List<OpenProductThema>? = null,
        themas: List<OpenProductThema> = emptyList(),
        pageSize: Int? = null,
    ): List<Zaak> {
        val themasAll = mutableListOf<OpenProductThema>()
        val collectedThemaList = mutableListOf<OpenProductThema>()
        // only do get the collectThemaHierarchyUpFromSubThema if themasList == null otherwise it is unnecessary
        if (themasList == null) {
            if (themas.isEmpty()) {
                themasAll.addAll(
                    getThemas(
                        authentication = authentication,
                        pageNumber = 1,
                        pageSize = 999,
                    ).results,
                )
            } else {
                themasAll.addAll(themas)
            }

            collectedThemaList.addAll(
                collectThemaHierarchyUpFromSubThema(
                    themaId = id,
                    themas = themasAll,
                ),
            )
        } else {
            collectedThemaList.addAll(themasList)
        }
        // 2. loop through productTypes and get zaakTypes
        val zaakTypes = mutableSetOf<UUID>()

        collectedThemaList.forEach { thema ->
            thema.producttypen.forEach {
                getProductType(
                    id = it.uuid,
                    language = language,
                )?.zaaktypen?.forEach { zaakType ->
                    zaakTypes.add(CoreUtils.extractId(zaakType.url!!))
                }
            }
        }

        // 3. get Zaken with filters
        if (!authenticationMachtigingsDienstService.isAllowedZaakTypes(authentication, zaakTypes.toList())) {
            return emptyList()
        }

        return zakenApiService
            .getZaken(
                authentication = authentication,
                page = 1,
                pageSize = pageSize,
                isOpen = isOpen,
                zaakTypeUUIDs = zaakTypes.toList(),
            ).content
    }

    /**
     * Get taken of a thema
     * @param: authentication, authenticated user
     * @param: id, uuid of thema,
     * @param: language, language of the producttype, default is NL
     * @return: list of taken
     */
    suspend fun getThemaTaken(
        authentication: CommonGroundAuthentication,
        id: UUID,
        language: String? = null,
        pageSize: Int? = null,
    ): List<TaakV2> {
        val themas =
            getThemas(
                authentication = authentication,
                pageNumber = 1,
                pageSize = 999,
            ).results
        val collectedThemas =
            collectThemaHierarchyUpFromSubThema(
                themaId = id,
                themas = themas,
            )
        val taken =
            taakService
                .getTakenV2(
                    authentication = authentication,
                    pageNumber = 1,
                    pageSize = pageSize ?: 999,
                ).content

        // when no tasks are found, just return immediately
        if (taken.isEmpty()) {
            return taken
        }

        val zaken =
            getThemaZaken(
                authentication = authentication,
                id = id,
                language = language,
                themasList = collectedThemas,
                themas = themas,
            )

        val searchVariables =
            listOf(
                OpenProductProductenFilters.THEMA_UUID_IN to themas.joinToString { it.uuid.toString() },
            )

        val producten =
            getProducten(
                authentication = authentication,
                pageNumber = 1,
                pageSize = 999,
                extraSearchVariables = searchVariables,
            ).results

        // filter out the taak which is not connected to a zaak
        return taken
            .filterNot { task ->
                !zaken.any { it.uuid.toString() == task.koppeling.value } &&
                    !producten.any { it.uuid.toString() == task.koppeling.value }
            }.sortedBy { it.verloopdatum }
    }

    /**
     * Get zaken of a product
     * @param: zaken, list of zaken
     * @return: list of zaken. One zaak that fails to load is logged and skipped (deliberate
     *   resilience - see ADR 0003) rather than failing the whole list.
     */
    suspend fun getProductZaken(
        authentication: CommonGroundAuthentication,
        zaken: List<OpenProductUrl>,
    ): List<Zaak> =
        mapSkippingFailures(
            items = zaken,
            logger = logger,
            describe = { "zaak ${it.url}" },
        ) { zaak ->
            zakenApiService.getZaak(
                authentication = authentication,
                id = extractId(zaak.url!!),
            )
        }

    /**
     * Get taken of a product
     * @param: taken, list of taken
     * @return: list of taken. One taak that fails to load is logged and skipped (deliberate
     *   resilience - see ADR 0003) rather than failing the whole list.
     */
    suspend fun getProductTaken(
        authentication: CommonGroundAuthentication,
        taken: List<OpenProductUrl>,
    ): List<TaakV2> =
        mapSkippingFailures(
            items = taken,
            logger = logger,
            describe = { "taak ${it.url}" },
        ) { taak ->
            taakService.getTaakByIdV2(
                authentication = authentication,
                id = extractId(taak.url!!),
            )
        }.distinct()

    /**
     * Get acties of a product
     * @param: productTypeId, uuid of producttype
     * @param: naam, naam of the acties
     * @return: list of acties
     */
    suspend fun getProductActies(
        productTypeId: UUID,
        naam: String? = null,
    ): List<OpenProductActie> {
        val extraSearchVariables =
            mutableListOf<Pair<OpenProductActiesFilters, Any>>(
                OpenProductActiesFilters.PRODUCTTYPE_UUID to productTypeId.toString(),
            )

        naam?.let {
            extraSearchVariables.add(
                OpenProductActiesFilters.NAAM to naam,
            )
        }
        return getActies(
            pageNumber = 1,
            pageSize = 999,
            extraSearchVariables = extraSearchVariables,
        ).results
    }

    suspend fun getProductDocumentContent(
        authentication: CommonGroundAuthentication,
        productId: UUID,
        documentId: UUID,
    ): Pair<Document, Flow<DataBuffer>> {
        try {
            // get product, if not authorized throws an unauthorized
            val product =
                getProduct(
                    authentication = authentication,
                    id = productId,
                )

            // if product is not found, return a 204
            if (product == null) {
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found")
            }

            val documentUrl =
                product.documenten.firstOrNull { extractId(it.url!!) == documentId }
                    ?: run {
                        logger.debug {
                            "Access denied to document $documentId on product $productId for user ${authentication.userId}"
                        }
                        throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access denied")
                    }
            val document =
                documentenApiService.getDocument(
                    documentUrl = documentUrl.url!!,
                )
            val content =
                documentenApiService.getDocumentContentStreaming(
                    informatieobejctUrl = documentUrl.url,
                )
            return document to content
        } catch (e: Exception) {
            logger.error(e) { "Error getting product document: " + e.message }
            if (e is NullPointerException) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST)
            }
            throw e
        }
    }

    /**
     * @return: documents of the product. One document that fails to load is logged and skipped
     *   (deliberate resilience - see ADR 0003) rather than failing the whole list.
     */
    suspend fun getOpenProductDocumenten(
        openProductProduct: OpenProductProduct,
    ): List<Document> {
        val documents =
            mapSkippingFailures(
                items = openProductProduct.documenten,
                logger = logger,
                describe = { "document ${it.url}" },
            ) { documentUrl ->
                documentenApiService.getDocument(documentUrl.url!!)
            }
        return documentenApiService.filterDocuments(documents)
    }

    /**
     * Collect thema hierarchy up from subthema
     * @param: themaId, uuid of thema
     * @param: themas, all published themas
     * @return: list of themas
     */
    private fun collectThemaHierarchyUpFromSubThema(
        themaId: UUID,
        themas: List<OpenProductThema>? = emptyList(),
    ): List<OpenProductThema> {
        // 1. get themas, including the hoofdthema and may be their hoofdthema
        val collectedThemas = mutableSetOf<OpenProductThema>()

        val thema =
            themas?.find { it.uuid == themaId }

        if (thema == null) {
            return emptyList()
        } else {
            collectedThemas.add(thema)
        }

        // 1.5 get all the hoofdthema's
        collectedThemas.addAll(
            searchFromSubThemaUpToHoofdThema(
                thema = thema,
                themas = themas,
            ),
        )

        return collectedThemas.toList()
    }

    /**
     * Search themas from subthema all the way up to the hoofd thema
     * @param: thema
     * @param: themas, all published themas
     * @return: Results of themas
     */
    private fun searchFromSubThemaUpToHoofdThema(
        thema: OpenProductThema,
        themas: List<OpenProductThema>? = emptyList(),
    ): List<OpenProductThema> {
        val hoofdThemas = mutableListOf<OpenProductThema>()
        if (thema.hoofdThema != null) {
            val hoofdThema =
                themas?.find { it.uuid == thema.hoofdThema }
            if (hoofdThema != null) {
                hoofdThemas.add(hoofdThema)
                hoofdThemas.addAll(
                    searchFromSubThemaUpToHoofdThema(
                        thema = hoofdThema,
                        themas = themas,
                    ),
                )
            }
        }

        return hoofdThemas
    }

    /**
     * Get the eigenaar filter used for producten
     * @param: authentication, authenticated user
     * @param: themas, all published themas
     * @return: list of filters
     */
    private fun getEigenaarFilter(authentication: CommonGroundAuthentication): List<Pair<OpenProductProductenFilters, String>> =
        when (authentication) {
            is BurgerAuthentication -> {
                listOf(
                    Pair(
                        OpenProductProductenFilters.EIGENAREN_BSN,
                        authentication.userId,
                    ),
                )
            }

            is BedrijfAuthentication -> {
                val vestigingsNummer = authentication.getVestigingsNummer()
                if (vestigingsNummer != null) {
                    listOf(
                        Pair(
                            OpenProductProductenFilters.EIGENAREN_VESTIGINGSNUMMER,
                            vestigingsNummer,
                        ),
                    )
                } else {
                    listOf(
                        Pair(
                            OpenProductProductenFilters.EIGENAREN_KVKNUMMER,
                            authentication.userId,
                        ),
                    )
                }
            }

            else -> {
                throw IllegalArgumentException("Authentication not supported")
            }
        }

    /**
     * Is user authorized to view the product
     * @param: authentication, authenticated user
     * @param: product, the product to be checked
     * @return: true or fale
     */
    private fun isAuthorizedForProduct(
        authentication: CommonGroundAuthentication,
        product: OpenProductProduct,
    ): Boolean =
        when (authentication) {
            is BurgerAuthentication -> {
                product.eigenaren.firstOrNull { it.bsn == authentication.userId } != null
            }

            is BedrijfAuthentication -> {
                val vestigingsNummer = authentication.getVestigingsNummer()
                if (vestigingsNummer != null) {
                    product.eigenaren.firstOrNull {
                        it.kvkNummer == authentication.userId &&
                            it.vestigingsnummer == vestigingsNummer
                    } != null
                } else {
                    product.eigenaren.firstOrNull { it.kvkNummer == authentication.userId } != null
                }
            }

            else -> {
                false
            }
        }

    /**
     * Build a thema hierarchy from a specific thema
     * @param: thema
     * @return: list of thema hierarchy
     */
    private suspend fun buildThemaHierachy(
        authentication: CommonGroundAuthentication,
        thema: OpenProductThema,
    ): List<OpenProductThemaHierarchy> {
        val themasHierarchy = mutableListOf<OpenProductThemaHierarchy>()
        val themas =
            getThemas(
                authentication = authentication,
                pageNumber = 1,
                pageSize = 999,
            ).results

        themasHierarchy.add(
            searchSubThemasHierarchy(
                thema = thema,
                themas = themas,
            ),
        )

        return themasHierarchy
    }

    /**
     * Search
     * @param: thema
     * @param: themas
     * @return: thema hierarchy
     */
    private fun searchSubThemasHierarchy(
        thema: OpenProductThema,
        themas: List<OpenProductThema>,
    ): OpenProductThemaHierarchy {
        val themasHierarchy = mutableListOf<OpenProductThemaHierarchy>()

        val subThemas =
            themas.toList().filter { it.hoofdThema == thema.uuid }

        subThemas.forEach {
            themasHierarchy.add(
                searchSubThemasHierarchy(
                    thema = it,
                    themas = themas,
                ),
            )
        }

        return OpenProductThemaHierarchy(
            thema = thema,
            subThemas = themasHierarchy,
        )
    }

    companion object {
        val logger = KotlinLogging.logger {}
    }
}