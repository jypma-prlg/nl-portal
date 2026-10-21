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

import kotlinx.coroutines.test.runTest
import nl.nlportal.openproduct.client.OpenProductDmnClient
import nl.nlportal.openproduct.client.domain.OpenProductActie
import nl.nlportal.openproduct.client.domain.OpenProductActieMappingVariable
import nl.nlportal.openproduct.client.domain.OpenProductDmnResponse
import nl.nlportal.openproduct.client.domain.OpenProductDmnVariableType
import nl.nlportal.openproduct.client.domain.OpenProductFrequentie
import nl.nlportal.openproduct.client.domain.OpenProductProduct
import nl.nlportal.openproduct.client.domain.OpenProductProductProductType
import nl.nlportal.openproduct.client.domain.OpenProductToegestaneStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime
import java.util.UUID

class OpenProductDmnServiceTest {
    @Mock
    private lateinit var openProductDmnClient: OpenProductDmnClient

    @Mock
    private lateinit var openProductService: OpenProductService

    private lateinit var openProductDmnService: OpenProductDmnService

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        openProductDmnService = OpenProductDmnService(openProductDmnClient, openProductService)
    }

    @Test
    fun `should still fetch a decision when an actie's mapping has no product key`() =
        runTest {
            // given: an actie that only configures a "static" mapping, no "product" mapping -
            // kotlin:S6611 flagged the unguarded actie.mapping[ACTIE_MAPPING_KEY_PRODUCT]!! that
            // used to NPE here, which getDecision's own catch(Exception) swallowed into a silent
            // emptyList() - no decision was ever fetched, even though a DMN decision only needs
            // the static mapping to be useful.
            val actie =
                OpenProductActie(
                    uuid = UUID.randomUUID(),
                    naam = "test-actie",
                    url = "http://localhost/dmn",
                    mapping =
                        mapOf(
                            OpenProductDmnService.ACTIE_MAPPING_KEY_STATIC to
                                listOf(
                                    OpenProductActieMappingVariable(
                                        name = "staticVar",
                                        classType = OpenProductDmnVariableType.STRING,
                                        regex = null,
                                        value = "staticValue",
                                    ),
                                ),
                        ),
                )
            val expectedDecision = listOf(mapOf("result" to OpenProductDmnResponse("outcome", "String")))
            whenever(openProductDmnClient.getDecision(any(), any())).thenReturn(expectedDecision)

            // when
            val decision = openProductDmnService.getDecision(testProduct(), actie)

            // then: the DMN client was actually called and its result returned, instead of the
            // caught-NPE empty list.
            assertEquals(expectedDecision, decision)
        }

    private fun testProduct(): OpenProductProduct {
        val now = ZonedDateTime.now()
        return OpenProductProduct(
            uuid = UUID.randomUUID(),
            naam = "test-product",
            aanmaakDatum = now,
            updateDatum = now,
            producttype =
                OpenProductProductProductType(
                    uuid = UUID.randomUUID(),
                    code = "TEST",
                    uniformeProductNaam = "test-uniform-naam",
                    aanmaakDatum = now,
                    updateDatum = now,
                ),
            status = OpenProductToegestaneStatus.ACTIEF,
            frequentie = OpenProductFrequentie.EENMALIG,
        )
    }
}