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

import com.jayway.jsonpath.JsonPath
import io.github.oshai.kotlinlogging.KotlinLogging
import nl.nlportal.commonground.authentication.CommonGroundAuthentication
import nl.nlportal.core.util.Mapper
import nl.nlportal.openproduct.client.OpenProductDmnClient
import nl.nlportal.openproduct.client.domain.OpenProductActie
import nl.nlportal.openproduct.client.domain.OpenProductActieMappingVariable
import nl.nlportal.openproduct.client.domain.OpenProductDmnRequest
import nl.nlportal.openproduct.client.domain.OpenProductDmnRequestMapping
import nl.nlportal.openproduct.client.domain.OpenProductDmnResponse
import nl.nlportal.openproduct.client.domain.OpenProductDmnVariable
import nl.nlportal.openproduct.client.domain.OpenProductProduct
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

class OpenProductDmnService(
    val openProductDmnClient: OpenProductDmnClient,
    val openProductService: OpenProductService,
) {
    /**
     * Get decision of a product
     * @param: product
     * @return: List of decisions. One actie whose decision fails to load is logged and skipped
     *   (deliberate resilience - see ADR 0003) rather than failing the whole product decision.
     */
    suspend fun getProductDecision(
        product: OpenProductProduct,
    ): List<Map<String, OpenProductDmnResponse>> {
        // only get a DMN decision if mapping is not null. In the future we need to check if soort/type = DMN
        val acties = openProductService.getProductActies(product.producttype.uuid).filter { it.mapping != null }

        return mapSkippingFailures(
            items = acties,
            logger = logger,
            describe = { "decision for actie ${it.naam}" },
        ) { actie ->
            getDecision(
                product = product,
                actie = actie,
            )
        }.flatten()
    }

    /**
     * Get decision directly of a product from an action
     * @param: authentication, authenticated user
     * @param: naam of the action
     * @param: productId, id of the product
     * @return: List of decisions
     */
    suspend fun getActieDecision(
        authentication: CommonGroundAuthentication,
        naam: String,
        productId: UUID,
    ): List<Map<String, OpenProductDmnResponse>> {
        val product =
            openProductService.getProduct(
                authentication = authentication,
                id = productId,
            )

        if (product == null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, NO_PRODUCT_FOUND_BY_ID + productId)
        }

        val actie =
            openProductService.getProductActies(
                productTypeId = product.producttype.uuid,
                naam = naam,
            )
        if (actie.isEmpty()) {
            logger.warn { NO_ACTIES_FOUND_BY_NAME + naam }
            return emptyList()
        }
        return getDecision(
            product = product,
            actie = actie[0],
        )
    }

    /**
     * Get decision from Camunda
     * @param: product, the source for the mapping
     * @param: actie, with the mapping of to construct the dmn variables
     * @return: List of decisions
     */
    suspend fun getDecision(
        product: OpenProductProduct,
        actie: OpenProductActie,
    ): List<Map<String, OpenProductDmnResponse>> {
        val variablesMapping = mutableMapOf<String, OpenProductDmnVariable>()
        // add the product mapping
        if (actie.mapping != null) {
            // handle the configured product variables, same pattern as the static mapping
            // below: a missing "product" key means no product variables to add, not a reason
            // to abort the whole decision (kotlin:S6611 - this used to be an unsafe `!!`).
            actie.mapping[ACTIE_MAPPING_KEY_PRODUCT]?.let {
                variablesMapping.putAll(
                    mapActieMappingVariables(
                        actieMappingVariables = it,
                        source = Mapper.get().writeValueAsString(product),
                    ),
                )
            }

            // add the static mapping
            // handle the configured static variables
            actie.mapping[ACTIE_MAPPING_KEY_STATIC]?.let {
                variablesMapping.putAll(
                    mapActieMappingVariables(
                        it,
                    ),
                )
            }
        }

        val dmnRequest =
            OpenProductDmnRequest(
                key = actie.naam,
                mapping =
                    OpenProductDmnRequestMapping(
                        variables = variablesMapping,
                    ),
            )

        return openProductDmnClient
            .getDecision(
                url = actie.url,
                dmnRequest = dmnRequest,
            ).filter { it.isNotEmpty() }
    }

    private fun mapActieMappingVariables(
        actieMappingVariables: List<OpenProductActieMappingVariable>,
        source: String? = null,
    ): Map<String, OpenProductDmnVariable> {
        val variablesMapping = mutableMapOf<String, OpenProductDmnVariable>()
        actieMappingVariables.forEach {
            if (it.regex != null && source != null) {
                variablesMapping[it.name] =
                    OpenProductDmnVariable(
                        findVariableInJson(it.regex, source),
                        it.classType,
                    )
            }

            if (it.value != null) {
                variablesMapping[it.name] =
                    OpenProductDmnVariable(
                        it.value,
                        it.classType,
                    )
            }
        }

        return variablesMapping
    }

    private fun findVariableInJson(
        regex: String,
        source: String,
    ): Any {
        try {
            val inputJsonPath = JsonPath.parse(source)
            return inputJsonPath.read<Any>(regex)
        } catch (ex: Exception) {
            logger.warn { "Problem with parsing variable: ${ex.message}" }
        }
        return ""
    }

    companion object {
        val logger = KotlinLogging.logger {}
        const val NO_ACTIES_FOUND: String =
            "Could not acties found: "
        const val NO_ACTIES_FOUND_BY_NAME: String =
            "Could not found an actie with name: "
        const val NO_PRODUCT_FOUND_BY_ID: String =
            "Could not found product with id: "
        const val SOURCE_MAPPING_FAILED: String =
            "Source mapping failed for DMN, check mapping of actie: "
        const val ACTIE_MAPPING_KEY_STATIC: String = "static"
        const val ACTIE_MAPPING_KEY_PRODUCT: String = "product"
    }
}