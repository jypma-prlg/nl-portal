/*
 * Copyright 2015-2024 Den Haag, Ritense, the Netherlands.
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
package nl.nlportal.openklant.service

import io.github.oshai.kotlinlogging.KotlinLogging
import nl.nlportal.commonground.authentication.BedrijfAuthentication
import nl.nlportal.commonground.authentication.BurgerAuthentication
import nl.nlportal.commonground.authentication.CommonGroundAuthentication
import nl.nlportal.openklant.autoconfigure.OpenKlantModuleConfiguration.OpenKlantConfigurationProperties
import nl.nlportal.openklant.client.OpenKlant2KlantinteractiesClient
import nl.nlportal.openklant.client.domain.Contactnaam
import nl.nlportal.openklant.client.domain.ContactpersoonIdentificatie
import nl.nlportal.openklant.client.domain.OpenKlant2DigitaleAdres
import nl.nlportal.openklant.client.domain.OpenKlant2DigitaleAdresUpdate
import nl.nlportal.openklant.client.domain.OpenKlant2DigitaleAdressenFilters
import nl.nlportal.openklant.client.domain.OpenKlant2Identificator
import nl.nlportal.openklant.client.domain.OpenKlant2IdentificeerdePartij
import nl.nlportal.openklant.client.domain.OpenKlant2Klantcontact
import nl.nlportal.openklant.client.domain.OpenKlant2KlantcontactenFilters
import nl.nlportal.openklant.client.domain.OpenKlant2Partij
import nl.nlportal.openklant.client.domain.OpenKlant2PartijIdentificator
import nl.nlportal.openklant.client.domain.OpenKlant2PartijIdentificatorenFilters
import nl.nlportal.openklant.client.domain.OpenKlant2PartijenFilters
import nl.nlportal.openklant.client.domain.OpenKlant2SubIdentificatorVan
import nl.nlportal.openklant.client.domain.OpenKlant2UUID
import nl.nlportal.openklant.client.domain.OrganisatieIdentificatie
import nl.nlportal.openklant.client.domain.PartijIdentificatie
import nl.nlportal.openklant.client.domain.PartijIdentificatorCodeRegister
import nl.nlportal.openklant.client.domain.PartijIdentificatorCodeSoort
import nl.nlportal.openklant.client.domain.PartijIdentificatorCodeType
import nl.nlportal.openklant.client.domain.PersoonsIdentificatie
import nl.nlportal.openklant.client.domain.asSoortPartij
import nl.nlportal.openklant.client.domain.asSoortPartijEnum
import nl.nlportal.openklant.client.path.DigitaleAdressen
import nl.nlportal.openklant.client.path.KlantContacten
import nl.nlportal.openklant.client.path.PartijIdentificatoren
import nl.nlportal.openklant.client.path.Partijen
import nl.nlportal.openklant.graphql.domain.OnderwerpObjectIndentificatorType
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

class OpenKlant2Service(
    private val openKlant2Client: OpenKlant2KlantinteractiesClient,
    private val openKlantConfigurationProperties: OpenKlantConfigurationProperties,
) {
    suspend fun findPartijByAuthentication(authentication: CommonGroundAuthentication): OpenKlant2Partij? {
        val searchVariables = searchVariablesPartij(authentication)

        try {
            return openKlant2Client.path<Partijen>().get(searchVariables)?.singleOrNull()
        } catch (ex: WebClientResponseException) {
            logger.warn(ex) { "Failed to find Partij: ${ex.responseBodyAsString}" }
            return null
        }
    }

    suspend fun getPartij(partijId: UUID): OpenKlant2Partij? {
        try {
            return openKlant2Client.path<Partijen>().get(partijId)
        } catch (ex: WebClientResponseException) {
            logger.error(ex) { "Failed to find Partij: ${ex.responseBodyAsString}" }
            return null
        }
    }

    suspend fun createPartijWithIdentificator(
        partijIdentificator: OpenKlant2PartijIdentificator,
        partij: OpenKlant2Partij,
    ): OpenKlant2Partij? {
        val partijResponse =
            try {
                openKlant2Client
                    .path<Partijen>()
                    .create(partij)
                    .also {
                        try {
                            openKlant2Client
                                .path<PartijIdentificatoren>()
                                .create(
                                    partijIdentificator
                                        .copy(
                                            identificeerdePartij = OpenKlant2IdentificeerdePartij(it.uuid!!),
                                        ),
                                )
                        } catch (ex: WebClientResponseException) {
                            logger.error(ex) { "Failed to create PartijIdentificator: ${ex.responseBodyAsString}" }
                            openKlant2Client.path<Partijen>().delete(it.uuid!!)
                            throw ex
                        }
                    }
            } catch (ex: WebClientResponseException) {
                logger.error(ex) { "Failed to create Partij: ${ex.responseBodyAsString}" }
                return null
            }
        return getPartij(partijResponse.uuid!!)
    }

    suspend fun createPartijWithIdentificatorFromAuthentication(
        authentication: CommonGroundAuthentication,
        partij: OpenKlant2Partij,
    ): OpenKlant2Partij? {
        var partijIdentificator =
            OpenKlant2PartijIdentificator(
                partijIdentificator =
                    createPartijIndicator(authentication),
            )

        val vestigingsNummer = authentication.getVestigingsNummer()
        // if vestigingsnummer is not null, find the partijIdentificator of the kvknummer or create it (including a partij)
        if (vestigingsNummer != null) {
            val partijIdentificatorKvk =
                findPartijIdentificatoren(
                    listOf(
                        OpenKlant2PartijIdentificatorenFilters.PAGE to 1,
                        OpenKlant2PartijIdentificatorenFilters.PARTIJ_IDENTIFICATOR_CODEREGISTER to PartijIdentificatorCodeRegister.HR.register,
                        OpenKlant2PartijIdentificatorenFilters.PARTIJ_IDENTIFICATOR_CODEOBJECTTYPE to PartijIdentificatorCodeType.NIETNATUURLIJKPERSOON.type,
                        OpenKlant2PartijIdentificatorenFilters.PARTIJ_IDENTIFICATOR_CODESOORTOBJECTID to PartijIdentificatorCodeSoort.KVKNUMMER.soort,
                        OpenKlant2PartijIdentificatorenFilters.PARTIJ_IDENTIFICATOR_OBJECTID to authentication.userId,
                    ),
                )?.firstOrNull()

            if (partijIdentificatorKvk == null) {
                // if partij identificator of kvk could not be found, make one including the partij
                val partijKvk =
                    createPartijWithIdentificator(
                        partijIdentificator =
                            OpenKlant2PartijIdentificator(
                                partijIdentificator =
                                    OpenKlant2Identificator(
                                        objectId = authentication.userId,
                                        codeSoortObjectId = PartijIdentificatorCodeSoort.KVKNUMMER.soort,
                                        codeObjecttype = PartijIdentificatorCodeType.NIETNATUURLIJKPERSOON.type,
                                        codeRegister = PartijIdentificatorCodeRegister.HR.register,
                                    ),
                            ),
                        partij = partij,
                    )
                partijIdentificator =
                    partijIdentificator.copy(
                        subIdentificatorVan = OpenKlant2SubIdentificatorVan(partijKvk?.partijIdentificatoren?.first()?.uuid!!),
                    )
            } else {
                partijIdentificator =
                    partijIdentificator.copy(
                        subIdentificatorVan = OpenKlant2SubIdentificatorVan(partijIdentificatorKvk.uuid!!),
                    )
            }
        }

        return createPartijWithIdentificator(
            partijIdentificator = partijIdentificator,
            partij = partij,
        )
    }

    suspend fun updatePartij(
        authentication: CommonGroundAuthentication,
        partij: OpenKlant2Partij,
    ): OpenKlant2Partij? {
        val previousPartij = findPartijByAuthentication(authentication)
        if (previousPartij != null) {
            val updatedPartij =
                previousPartij.copy(
                    indicatieGeheimhouding = partij.indicatieGeheimhouding,
                    indicatieActief = partij.indicatieActief,
                    soortPartij = partij.soortPartij,
                    partijIdentificatie = partij.partijIdentificatie,
                )
            val partijResponse =
                try {
                    openKlant2Client
                        .path<Partijen>()
                        .put(updatedPartij)
                } catch (ex: WebClientResponseException) {
                    logger.error(ex) { "Failed to update Partij: ${ex.responseBodyAsString}" }
                    return null
                }

            return partijResponse
        }

        logger.warn { "Failed to update Partij: No existing Partij found. Creating new Partij" }
        return createPartijWithIdentificatorFromAuthentication(authentication, partij)
    }

    suspend fun findPartijIdentificatorenFromAuthentication(authentication: CommonGroundAuthentication): List<OpenKlant2PartijIdentificator>? {
        val searchFilters: List<Pair<OpenKlant2PartijIdentificatorenFilters, Any>> =
            when (authentication) {
                is BurgerAuthentication -> {
                    listOf(
                        OpenKlant2PartijIdentificatorenFilters.PAGE to 1,
                        OpenKlant2PartijIdentificatorenFilters.PARTIJ_IDENTIFICATOR_CODEREGISTER to PartijIdentificatorCodeRegister.BRP.register,
                        OpenKlant2PartijIdentificatorenFilters.PARTIJ_IDENTIFICATOR_CODEOBJECTTYPE to PartijIdentificatorCodeType.NATUURLIJKPERSOON.type,
                        OpenKlant2PartijIdentificatorenFilters.PARTIJ_IDENTIFICATOR_CODESOORTOBJECTID to PartijIdentificatorCodeSoort.BSN.soort,
                        OpenKlant2PartijIdentificatorenFilters.PARTIJ_IDENTIFICATOR_OBJECTID to authentication.userId,
                    )
                }

                is BedrijfAuthentication -> {
                    val vestigingsNummer = authentication.getVestigingsNummer()
                    if (vestigingsNummer != null) {
                        listOf(
                            OpenKlant2PartijIdentificatorenFilters.PAGE to 1,
                            OpenKlant2PartijIdentificatorenFilters.PARTIJ_IDENTIFICATOR_CODEREGISTER to PartijIdentificatorCodeRegister.HR.register,
                            OpenKlant2PartijIdentificatorenFilters.PARTIJ_IDENTIFICATOR_CODEOBJECTTYPE to PartijIdentificatorCodeType.VESTIGING.type,
                            OpenKlant2PartijIdentificatorenFilters.PARTIJ_IDENTIFICATOR_CODESOORTOBJECTID to PartijIdentificatorCodeSoort.VESTIGINGSNUMMER.soort,
                            OpenKlant2PartijIdentificatorenFilters.PARTIJ_IDENTIFICATOR_OBJECTID to vestigingsNummer,
                        )
                    } else {
                        listOf(
                            OpenKlant2PartijIdentificatorenFilters.PAGE to 1,
                            OpenKlant2PartijIdentificatorenFilters.PARTIJ_IDENTIFICATOR_CODEREGISTER to PartijIdentificatorCodeRegister.HR.register,
                            OpenKlant2PartijIdentificatorenFilters.PARTIJ_IDENTIFICATOR_CODEOBJECTTYPE to PartijIdentificatorCodeType.NIETNATUURLIJKPERSOON.type,
                            OpenKlant2PartijIdentificatorenFilters.PARTIJ_IDENTIFICATOR_CODESOORTOBJECTID to PartijIdentificatorCodeSoort.KVKNUMMER.soort,
                            OpenKlant2PartijIdentificatorenFilters.PARTIJ_IDENTIFICATOR_OBJECTID to authentication.userId,
                        )
                    }
                }

                else -> {
                    throw IllegalArgumentException("Unsupported authentication type: ${authentication::class.qualifiedName}")
                }
            }

        return findPartijIdentificatoren(searchFilters)
    }

    suspend fun findPartijIdentificatoren(
        searchFilters: List<Pair<OpenKlant2PartijIdentificatorenFilters, Any>>,
    ): List<OpenKlant2PartijIdentificator>? {
        try {
            return openKlant2Client.path<PartijIdentificatoren>().get(searchFilters)
        } catch (ex: WebClientResponseException) {
            logger.error(ex) { "Failed to find Partij Identificatoren: ${ex.responseBodyAsString}" }
            return null
        }
    }

    suspend fun findDigitaleAdressen(
        authentication: CommonGroundAuthentication,
        isGeverifieerd: Boolean? = false,
        isStandaardAdres: Boolean? = false,
    ): List<OpenKlant2DigitaleAdres> {
        val searchVariables = searchVariablesDigitaleAdressen(authentication).toMutableList()

        openKlantConfigurationProperties.digitalAdressenReferentie?.let {
            searchVariables.add(OpenKlant2DigitaleAdressenFilters.REFERENTIE to it)
        }

        if (isGeverifieerd == true) {
            searchVariables.add(OpenKlant2DigitaleAdressenFilters.ISGEVERIFIEERD to isGeverifieerd)
        }

        if (isStandaardAdres == true) {
            searchVariables.add(OpenKlant2DigitaleAdressenFilters.ISSTANDAARDADRES to isStandaardAdres)
        }

        val response =
            try {
                openKlant2Client.path<DigitaleAdressen>().get(searchVariables)
            } catch (ex: WebClientResponseException) {
                logger.error(ex) { "Failed to get Partij with DigitaleAdressen: ${ex.responseBodyAsString}" }
                return emptyList()
            }

        return response
    }

    suspend fun createDigitaleAdres(
        authentication: CommonGroundAuthentication,
        digitaleAdres: OpenKlant2DigitaleAdres,
    ): OpenKlant2DigitaleAdres? {
        var userPartijId =
            findPartijIdentificatorenFromAuthentication(authentication)
                ?.firstOrNull()
                ?.identificeerdePartij
                ?.uuid

        if (userPartijId == null) {
            logger.debug { "Authenticated User does not have a Partij, let's create a new partij" }
            val partij =
                createPartijWithIdentificatorFromAuthentication(
                    authentication = authentication,
                    partij =
                        OpenKlant2Partij(
                            indicatieActief = true,
                            soortPartij = authentication.asSoortPartijEnum(),
                            partijIdentificatie =
                                createPartijIdentificatie(
                                    authentication = authentication,
                                ),
                        ),
                )

            if (partij == null) {
                logger.debug { "Creating new partij failed for digitale adressen" }
                return null
            }

            userPartijId = partij.uuid
        }

        return openKlant2Client
            .path<DigitaleAdressen>()
            .create(
                digitaleAdres
                    .copy(
                        verstrektDoorPartij = OpenKlant2UUID(userPartijId!!),
                        referentie = openKlantConfigurationProperties.digitalAdressenReferentie ?: "",
                    ),
            )
    }

    suspend fun findDigitalAdres(
        uuid: UUID,
    ): OpenKlant2DigitaleAdres? {
        try {
            val digitaleAdresResponse =
                try {
                    openKlant2Client
                        .path<DigitaleAdressen>()
                        .get(uuid = uuid)
                } catch (ex: WebClientResponseException) {
                    logger.debug(ex) { "Failed to find DigitaleAdres: ${ex.responseBodyAsString}" }
                    return null
                }
            return digitaleAdresResponse
        } catch (ex: WebClientResponseException) {
            logger.error(ex) { "Failed to find DigitaleAdres for uuid - $uuid: ${ex.responseBodyAsString}" }
            return null
        }
    }

    suspend fun updateDigitaleAdresById(
        authentication: CommonGroundAuthentication,
        digitaleAdres: OpenKlant2DigitaleAdresUpdate,
    ): OpenKlant2DigitaleAdres? {
        val previousDigitaleAdres = findDigitaleAdressen(authentication).singleOrNull { it.uuid == digitaleAdres.uuid }
        if (previousDigitaleAdres == null) {
            logger.debug { "Failed to update DigitaleAdres: No DigitaleAdres exists with provided Id" }
            return null
        }
        val digitaleAdresResponse =
            try {
                openKlant2Client
                    .path<DigitaleAdressen>()
                    .patch(
                        digitaleAdres,
                    )
            } catch (ex: WebClientResponseException) {
                logger.debug(ex) { "Failed to update DigitaleAdres: ${ex.responseBodyAsString}" }
                return null
            }

        return digitaleAdresResponse
    }

    suspend fun deleteDigitaleAdresById(
        authentication: CommonGroundAuthentication,
        digitaleAdresId: UUID,
    ) {
        val userDigitaleAdressen = findDigitaleAdressen(authentication)

        if (userDigitaleAdressen.none { it.uuid == digitaleAdresId }) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Access denied to this digitale adres")
        }

        try {
            openKlant2Client
                .path<DigitaleAdressen>()
                .delete(digitaleAdresId)
        } catch (ex: WebClientResponseException) {
            logger.error(ex) { "Failed to delete DigitaleAdres: ${ex.responseBodyAsString}" }
            return
        }

        return
    }

    suspend fun findKlantContacten(
        authentication: CommonGroundAuthentication,
        identificatorType: OnderwerpObjectIndentificatorType? = null,
        identificatorId: UUID? = null,
    ): List<OpenKlant2Klantcontact> {
        val searchVariables = searchVariablesKlantcontacten(authentication).toMutableList()

        identificatorId?.let {
            searchVariables.add(OpenKlant2KlantcontactenFilters.ONDERWERPOBJECT_ONDERWERPOBJECTIDENTIFICATOR_OBJECTID to it)
            searchVariables.add(OpenKlant2KlantcontactenFilters.ONDERWERPOBJECT_ONDERWERPOBJECTIDENTIFICATOR_CODESOORTOBJECTID to "uuid")
        }
        identificatorType?.let {
            searchVariables.add(OpenKlant2KlantcontactenFilters.ONDERWERPOBJECT_ONDERWERPOBJECTIDENTIFICATOR_CODEOBJECTTYPE to it.name.lowercase())
        }

        getKlantContactCodeRegister(
            identificatorType = identificatorType,
        )?.let {
            searchVariables.add(OpenKlant2KlantcontactenFilters.ONDERWERPOBJECT_ONDERWERPOBJECTIDENTIFICATOR_CODEREGISTER to it)
        }

        return try {
            openKlant2Client.path<KlantContacten>().get(searchVariables)
        } catch (ex: WebClientResponseException) {
            logger.error(ex) { "Failed to get Partij with Klantcontacten: ${ex.responseBodyAsString}" }
            emptyList()
        }
    }

    private fun createPartijIndicator(authentication: CommonGroundAuthentication): OpenKlant2Identificator =
        when (authentication) {
            is BurgerAuthentication -> {
                OpenKlant2Identificator(
                    objectId = authentication.userId,
                    codeSoortObjectId = PartijIdentificatorCodeSoort.BSN.soort,
                    codeObjecttype = PartijIdentificatorCodeType.NATUURLIJKPERSOON.type,
                    codeRegister = PartijIdentificatorCodeRegister.BRP.register,
                )
            }

            is BedrijfAuthentication -> {
                val vestigingsNummer = authentication.getVestigingsNummer()
                if (vestigingsNummer != null) {
                    OpenKlant2Identificator(
                        objectId = vestigingsNummer,
                        codeSoortObjectId = PartijIdentificatorCodeSoort.VESTIGINGSNUMMER.soort,
                        codeObjecttype = PartijIdentificatorCodeType.VESTIGING.type,
                        codeRegister = PartijIdentificatorCodeRegister.HR.register,
                    )
                } else {
                    OpenKlant2Identificator(
                        objectId = authentication.userId,
                        codeSoortObjectId = PartijIdentificatorCodeSoort.KVKNUMMER.soort,
                        codeObjecttype = PartijIdentificatorCodeType.NIETNATUURLIJKPERSOON.type,
                        codeRegister = PartijIdentificatorCodeRegister.HR.register,
                    )
                }
            }

            else -> {
                throw IllegalArgumentException("Unsupported authentication type: ${authentication::class.qualifiedName}")
            }
        }

    private fun searchVariablesPartij(authentication: CommonGroundAuthentication): List<Pair<OpenKlant2PartijenFilters, String>> =
        when (authentication) {
            is BurgerAuthentication -> {
                listOf(
                    OpenKlant2PartijenFilters.SOORT_PARTIJ to authentication.asSoortPartij(),
                    OpenKlant2PartijenFilters.PARTIJ_IDENTIFICATOR_OBJECT_ID to authentication.userId,
                )
            }

            is BedrijfAuthentication -> {
                val vestigingsNummer = authentication.getVestigingsNummer()
                if (vestigingsNummer != null) {
                    listOf(
                        OpenKlant2PartijenFilters.SOORT_PARTIJ to authentication.asSoortPartij(),
                        OpenKlant2PartijenFilters.PARTIJ_IDENTIFICATOR_OBJECT_ID to vestigingsNummer,
                    )
                } else {
                    listOf(
                        OpenKlant2PartijenFilters.SOORT_PARTIJ to authentication.asSoortPartij(),
                        OpenKlant2PartijenFilters.PARTIJ_IDENTIFICATOR_OBJECT_ID to authentication.userId,
                    )
                }
            }

            else -> {
                throw IllegalArgumentException("Unsupported authentication type: ${authentication::class.qualifiedName}")
            }
        }

    private fun searchVariablesDigitaleAdressen(
        authentication: CommonGroundAuthentication,
    ): List<Pair<OpenKlant2DigitaleAdressenFilters, Any>> =
        when (authentication) {
            is BurgerAuthentication -> {
                listOf(
                    OpenKlant2DigitaleAdressenFilters.PAGE to 1,
                    OpenKlant2DigitaleAdressenFilters.VERSTREKTDOORPARTIJ_PARTIJ_IDENTIFICATOR_CODEREGISTER to PartijIdentificatorCodeRegister.BRP.register,
                    OpenKlant2DigitaleAdressenFilters.VERSTREKTDOORPARTIJ_PARTIJ_IDENTIFICATOR_CODEOBJECTTYPE to PartijIdentificatorCodeType.NATUURLIJKPERSOON.type,
                    OpenKlant2DigitaleAdressenFilters.VERSTREKTDOORPARTIJ_PARTIJ_IDENTIFICATOR_CODESOORTOBJECTID to PartijIdentificatorCodeSoort.BSN.soort,
                    OpenKlant2DigitaleAdressenFilters.VERSTREKTDOORPARTIJ_PARTIJ_IDENTIFICATOR_OBJECTID to authentication.userId,
                )
            }

            is BedrijfAuthentication -> {
                val vestigingsNummer = authentication.getVestigingsNummer()
                if (vestigingsNummer != null) {
                    listOf(
                        OpenKlant2DigitaleAdressenFilters.PAGE to 1,
                        OpenKlant2DigitaleAdressenFilters.VERSTREKTDOORPARTIJ_PARTIJ_IDENTIFICATOR_CODEREGISTER to PartijIdentificatorCodeRegister.HR.register,
                        OpenKlant2DigitaleAdressenFilters.VERSTREKTDOORPARTIJ_PARTIJ_IDENTIFICATOR_CODEOBJECTTYPE to PartijIdentificatorCodeType.VESTIGING.type,
                        OpenKlant2DigitaleAdressenFilters.VERSTREKTDOORPARTIJ_PARTIJ_IDENTIFICATOR_CODESOORTOBJECTID to PartijIdentificatorCodeSoort.VESTIGINGSNUMMER.soort,
                        OpenKlant2DigitaleAdressenFilters.VERSTREKTDOORPARTIJ_PARTIJ_IDENTIFICATOR_OBJECTID to vestigingsNummer,
                    )
                } else {
                    listOf(
                        OpenKlant2DigitaleAdressenFilters.PAGE to 1,
                        OpenKlant2DigitaleAdressenFilters.VERSTREKTDOORPARTIJ_PARTIJ_IDENTIFICATOR_CODEREGISTER to PartijIdentificatorCodeRegister.HR.register,
                        OpenKlant2DigitaleAdressenFilters.VERSTREKTDOORPARTIJ_PARTIJ_IDENTIFICATOR_CODEOBJECTTYPE to PartijIdentificatorCodeType.NIETNATUURLIJKPERSOON.type,
                        OpenKlant2DigitaleAdressenFilters.VERSTREKTDOORPARTIJ_PARTIJ_IDENTIFICATOR_CODESOORTOBJECTID to PartijIdentificatorCodeSoort.KVKNUMMER.soort,
                        OpenKlant2DigitaleAdressenFilters.VERSTREKTDOORPARTIJ_PARTIJ_IDENTIFICATOR_OBJECTID to authentication.userId,
                    )
                }
            }

            else -> {
                throw IllegalArgumentException("Unsupported authentication type: ${authentication::class.qualifiedName}")
            }
        }

    private fun searchVariablesKlantcontacten(
        authentication: CommonGroundAuthentication,
    ): List<Pair<OpenKlant2KlantcontactenFilters, Any>> =
        when (authentication) {
            is BurgerAuthentication -> {
                listOf(
                    OpenKlant2KlantcontactenFilters.PAGE to 1,
                    OpenKlant2KlantcontactenFilters.HADBETROKKENE_PARTIJ_IDENTIFICATOR_CODEREGISTER to PartijIdentificatorCodeRegister.BRP.register,
                    OpenKlant2KlantcontactenFilters.HADBETROKKENE_PARTIJ_IDENTIFICATOR_CODEOBJECTTYPE to PartijIdentificatorCodeType.NATUURLIJKPERSOON.type,
                    OpenKlant2KlantcontactenFilters.HADBETROKKENE_PARTIJ_IDENTIFICATOR_CODESOORTOBJECTID to PartijIdentificatorCodeSoort.BSN.soort,
                    OpenKlant2KlantcontactenFilters.HADBETROKKENE_PARTIJ_IDENTIFICATOR_OBJECTID to authentication.userId,
                )
            }

            is BedrijfAuthentication -> {
                val vestigingsNummer = authentication.getVestigingsNummer()
                if (vestigingsNummer != null) {
                    listOf(
                        OpenKlant2KlantcontactenFilters.PAGE to 1,
                        OpenKlant2KlantcontactenFilters.HADBETROKKENE_PARTIJ_IDENTIFICATOR_CODEREGISTER to PartijIdentificatorCodeRegister.HR.register,
                        OpenKlant2KlantcontactenFilters.HADBETROKKENE_PARTIJ_IDENTIFICATOR_CODEOBJECTTYPE to PartijIdentificatorCodeType.VESTIGING.type,
                        OpenKlant2KlantcontactenFilters.HADBETROKKENE_PARTIJ_IDENTIFICATOR_CODESOORTOBJECTID to PartijIdentificatorCodeSoort.VESTIGINGSNUMMER.soort,
                        OpenKlant2KlantcontactenFilters.HADBETROKKENE_PARTIJ_IDENTIFICATOR_OBJECTID to vestigingsNummer,
                    )
                } else {
                    listOf(
                        OpenKlant2KlantcontactenFilters.PAGE to 1,
                        OpenKlant2KlantcontactenFilters.HADBETROKKENE_PARTIJ_IDENTIFICATOR_CODEREGISTER to PartijIdentificatorCodeRegister.HR.register,
                        OpenKlant2KlantcontactenFilters.HADBETROKKENE_PARTIJ_IDENTIFICATOR_CODEOBJECTTYPE to PartijIdentificatorCodeType.NIETNATUURLIJKPERSOON.type,
                        OpenKlant2KlantcontactenFilters.HADBETROKKENE_PARTIJ_IDENTIFICATOR_CODESOORTOBJECTID to PartijIdentificatorCodeSoort.KVKNUMMER.soort,
                        OpenKlant2KlantcontactenFilters.HADBETROKKENE_PARTIJ_IDENTIFICATOR_OBJECTID to authentication.userId,
                    )
                }
            }

            else -> {
                throw IllegalArgumentException("Unsupported authentication type: ${authentication::class.qualifiedName}")
            }
        }

    private fun createPartijIdentificatie(authentication: CommonGroundAuthentication): PartijIdentificatie =
        when (authentication) {
            is BurgerAuthentication -> {
                PersoonsIdentificatie(
                    contactnaam = Contactnaam(),
                )
            }

            is BedrijfAuthentication -> {
                OrganisatieIdentificatie(
                    naam = authentication.userId,
                )
            }

            else -> {
                ContactpersoonIdentificatie(
                    uuid = UUID.randomUUID(),
                )
            }
        }

    private fun getKlantContactCodeRegister(
        identificatorType: OnderwerpObjectIndentificatorType? = null,
    ): String? =
        when (identificatorType) {
            OnderwerpObjectIndentificatorType.PRODUCT -> openKlantConfigurationProperties.klantContactCodeRegister?.product
            OnderwerpObjectIndentificatorType.ZAAK -> openKlantConfigurationProperties.klantContactCodeRegister?.zaak
            else -> null
        }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}