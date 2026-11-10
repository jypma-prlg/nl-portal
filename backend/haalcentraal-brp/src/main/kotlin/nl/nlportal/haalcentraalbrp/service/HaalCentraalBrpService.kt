/*
 * Copyright 2015-2023 Den Haag, Ritense, the Netherlands.
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
package nl.nlportal.haalcentraalbrp.service

import io.github.oshai.kotlinlogging.KotlinLogging
import nl.nlportal.commonground.authentication.BurgerAuthentication
import nl.nlportal.commonground.authentication.CommonGroundAuthentication
import nl.nlportal.haalcentraalbrp.autoconfiguration.HaalCentraalBrpModuleConfiguration.HaalCentraalBrpConfigurationProperties
import nl.nlportal.haalcentraalbrp.client.HaalCentraalBrpBewoningClient
import nl.nlportal.haalcentraalbrp.client.HaalCentraalBrpClient
import nl.nlportal.haalcentraalbrp.client.path.Bewoningen
import nl.nlportal.haalcentraalbrp.client.path.Personen
import nl.nlportal.haalcentraalbrp.domain.bewoning.BewoningenApiRequest
import nl.nlportal.haalcentraalbrp.domain.bewoning.BewoningenApiResponse
import nl.nlportal.haalcentraalbrp.domain.brp.AanduidingNaamGebruikBrpNaam
import nl.nlportal.haalcentraalbrp.domain.brp.BrpApiRequest
import nl.nlportal.haalcentraalbrp.domain.brp.BrpPersoon
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HaalCentraalBrpService(
    val haalCentraalBrpConfigurationProperties: HaalCentraalBrpConfigurationProperties,
    val haalCentraalBrpClient: HaalCentraalBrpClient,
    val haalCentraalBrpBewoningClient: HaalCentraalBrpBewoningClient,
) {
    suspend fun getPersoon(authentication: CommonGroundAuthentication): BrpPersoon? =
        if (authentication is BurgerAuthentication) {
            val brpApiRequest =
                BrpApiRequest(
                    burgerservicenummer = listOf(authentication.userId),
                    fields = haalCentraalBrpConfigurationProperties.brpFields,
                )
            haalCentraalBrpClient
                .path<Personen>()
                .post(
                    brpApiRequest,
                    authentication,
                ).personen
                .singleOrNull()
                ?.apply {
                    naam?.officialLastName = determineOfficialLastName(this)
                }
        } else {
            null
        }

    suspend fun getBewoningen(
        authentication: CommonGroundAuthentication,
        adresseerbaarObjectIdentificatie: String,
    ): BewoningenApiResponse? =
        try {
            if (authentication is BurgerAuthentication) {
                val bewoningenApiRequest =
                    BewoningenApiRequest(
                        type = "BewoningMetPeildatum",
                        peildatum = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        adresseerbaarObjectIdentificatie = adresseerbaarObjectIdentificatie,
                    )
                haalCentraalBrpBewoningClient.path<Bewoningen>().post(bewoningenApiRequest, authentication)
            } else {
                logger.warn { "Authentication is not supported" }
                null
            }
        } catch (ex: Exception) {
            logger.error(ex) { "Something went wrong with 'getBewoningen' - error: ${ex.message}" }
            null
        }

    suspend fun getBewonersAantal(
        authentication: CommonGroundAuthentication,
        adresseerbaarObjectIdentificatie: String,
        woonplaats: String? = null,
    ): Int? {
        if (allowedBewoningRequest(woonplaats)) {
            return getBewoningen(authentication, adresseerbaarObjectIdentificatie)
                ?.bewoningen
                ?.firstOrNull()
                ?.bewoners
                ?.size
        }
        return null
    }

    private fun determineOfficialLastName(persoon: BrpPersoon): String {
        if (persoon.naam == null) {
            return ""
        }
        if (persoon.naam.aanduidingNaamgebruik?.code == null) {
            return persoon.naam.lastName()
        }
        val lastNamePartner =
            persoon.partners
                ?.firstOrNull { it.naam != null }
                ?.naam
                ?.lastName()
                ?.trim() ?: ""

        return when (persoon.naam.aanduidingNaamgebruik.code) {
            AanduidingNaamGebruikBrpNaam.PARTNER.value -> {
                lastNamePartner
            }

            AanduidingNaamGebruikBrpNaam.PARTNER_EIGEN.value -> {
                "$lastNamePartner - ${persoon.naam.lastName()}"
            }

            AanduidingNaamGebruikBrpNaam.EIGEN_PARTNER.value -> {
                "${persoon.naam.lastName()} - $lastNamePartner"
            }

            else -> {
                persoon.naam.lastName()
            }
        }
    }

    private fun allowedBewoningRequest(
        woonplaats: String? = null,
    ): Boolean {
        if (haalCentraalBrpConfigurationProperties.allowedCitiesForBewoning.isNotEmpty()) {
            if (woonplaats == null) {
                return false
            }

            if (woonplaats !in haalCentraalBrpConfigurationProperties.allowedCitiesForBewoning) {
                return false
            }
        }

        return true
    }

    companion object {
        val logger = KotlinLogging.logger {}
    }
}