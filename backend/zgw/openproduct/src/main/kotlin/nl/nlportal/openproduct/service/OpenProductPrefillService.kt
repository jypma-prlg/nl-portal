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
package nl.nlportal.openproduct.service

import com.github.wnameless.json.unflattener.JsonUnflattener
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.Any
import kotlin.Pair
import nl.nlportal.commonground.authentication.BedrijfAuthentication
import nl.nlportal.commonground.authentication.BurgerAuthentication
import nl.nlportal.commonground.authentication.CommonGroundAuthentication
import nl.nlportal.core.util.Mapper
import nl.nlportal.openproduct.autoconfigure.OpenProductModuleConfiguration.OpenProductConfigurationProperties.OpenProductPrefillConfigurationProperties
import nl.nlportal.openproduct.client.domain.OpenProductActiesFilters
import nl.nlportal.openproduct.client.domain.OpenProductPrefillObject
import nl.nlportal.openproduct.client.domain.OpenProductPrefillObjectIdentificatie
import nl.nlportal.openproduct.client.domain.OpenProductPrefillObjectIdentificatieType
import nl.nlportal.openproduct.client.domain.OpenProductPrefillResponse
import nl.nlportal.openproduct.client.domain.OpenProductProductenFilters
import nl.nlportal.zgw.objectenapi.client.ObjectsApiClient
import nl.nlportal.zgw.objectenapi.domain.CreateObjectsApiObjectRequest
import nl.nlportal.zgw.objectenapi.domain.CreateObjectsApiObjectRequestRecord
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import tools.jackson.databind.node.ObjectNode

class OpenProductPrefillService(
    val objectsApiClient: ObjectsApiClient,
    val openProductService: OpenProductService,
    val openProductPrefillConfigurationProperties: OpenProductPrefillConfigurationProperties,
) {
    suspend fun prefill(
        authentication: CommonGroundAuthentication,
        productId: UUID,
        naam: String,
    ): OpenProductPrefillResponse {
        // check if authenticated user is authorized for this product
        val product =
            openProductService.getProduct(
                authentication = authentication,
                id = productId,
            )

        if (product == null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Product kan niet gevonden worden voor $productId")
        }

        // get the actie
        val searchVariables =
            listOf<Pair<OpenProductActiesFilters, Any>>(
                OpenProductActiesFilters.NAAM to naam,
                OpenProductActiesFilters.PRODUCTTYPE_UUID to product.producttype.uuid,
            )
        val acties =
            openProductService
                .getActies(
                    pageNumber = 1,
                    pageSize = 1,
                    extraSearchVariables = searchVariables,
                ).results

        if (acties.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Prefill actie kan niet gevonden worden voor$naam")
        }

        val prefillData = mutableMapOf<String, Any>()
        prefillData.put(PRODUCT_UUID, productId.toString())
        val json = JsonUnflattener.unflatten(prefillData)
        return hashAndCreateObject(
            json = json,
            formulierUrl = acties[0].url,
            authentication = authentication,
        )
    }

    private suspend fun hashAndCreateObject(
        json: String,
        formulierUrl: String,
        authentication: CommonGroundAuthentication,
    ): OpenProductPrefillResponse {
        val nonce = PrefillSecurity.generateNonce()
        val hash = PrefillSecurity.createHash(nonce + json)

        val prefill =
            OpenProductPrefillObject(
                nonce = nonce,
                identificatie = getPrefillIndentificatie(authentication = authentication),
                data = Mapper.get().readValue(json, ObjectNode::class.java),
            )
        val createRequest =
            CreateObjectsApiObjectRequest(
                UUID.randomUUID(),
                openProductPrefillConfigurationProperties.typeUrl,
                CreateObjectsApiObjectRequestRecord(
                    typeVersion = 1,
                    data = prefill,
                    startAt = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                ),
            )

        val prefillObject = objectsApiClient.createObject(createRequest)
        return OpenProductPrefillResponse(
            objectId = prefillObject.uuid,
            hash = hash,
            formulierUrl = formulierUrl,
        )
    }

    /**
     * Get the prefill indentificatie
     * @param: authentication, authenticated user
     * @param: themas, all published themas
     * @return: OpenProductPrefillObjectIdentificatie
     */
    private fun getPrefillIndentificatie(authentication: CommonGroundAuthentication): OpenProductPrefillObjectIdentificatie =
        when (authentication) {
            is BurgerAuthentication -> {
                OpenProductPrefillObjectIdentificatie(
                    type = OpenProductPrefillObjectIdentificatieType.BSN,
                    value = authentication.userId,
                )
            }

            is BedrijfAuthentication -> {
                OpenProductPrefillObjectIdentificatie(
                    type = OpenProductPrefillObjectIdentificatieType.KVK,
                    value = authentication.userId,
                )
            }

            else -> {
                throw IllegalArgumentException("Authentication not supported")
            }
        }

    companion object {
        val logger = KotlinLogging.logger {}
        const val PRODUCT_UUID: String = "productUUID"
        const val SOURCE_MAPPING_FAILED: String =
            "Source mapping failed for Prefill, check prefillmapping of productType: "
    }
}