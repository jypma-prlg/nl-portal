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
package nl.nlportal.kvk.service

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import nl.nlportal.commonground.authentication.JwtBuilder
import nl.nlportal.kvk.client.KvkClient
import nl.nlportal.kvk.domain.Eigenaar
import nl.nlportal.kvk.domain.Embedded
import nl.nlportal.kvk.domain.Vestiging
import nl.nlportal.kvk.domain.MaatschappelijkeActiviteit
import nl.nlportal.kvk.domain.MaterieleRegistratie
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class KvkServiceTest {
    val kvkClient = mock<KvkClient>()
    val kvkService = KvkService(kvkClient)

    @Test
    fun `getMaatschappelijkeActiviteit calls client and gets MaatschappelijkeActiviteit`() =
        runTest {
            val authentication = JwtBuilder().aanvragerKvk("123").buildBedrijfAuthentication()
            whenever(kvkClient.getMaatschappelijkeActiviteit("123")).thenReturn(
                MaatschappelijkeActiviteit(
                    naam = "Test bedrijf",
                    "90012768",
                    "test",
                    "20230101",
                    MaterieleRegistratie("20020202"),
                    1,
                    "Test bedrijf",
                    listOf(),
                    listOf(),
                    null,
                ),
            )

            val bedrijf = kvkService.getMaatschappelijkeActiviteit(authentication)!!

            assertEquals("Test bedrijf", bedrijf.naam)
            verify(kvkClient).getMaatschappelijkeActiviteit("123")
        }

    @Test
    fun `getMaatschappelijkeActiviteit with invalid kvk`() =
        runTest {
            val authentication = JwtBuilder().aanvragerKvk("123").buildBedrijfAuthentication()

            val bedrijf = kvkService.getMaatschappelijkeActiviteit(authentication)

            assertNull(bedrijf)
        }

    @Test
    fun `getMaatschappelijkeActiviteit geeft null voor een burger`() =
        runTest {
            val authentication = JwtBuilder().aanvragerBsn("999993653").buildBurgerAuthentication()

            assertNull(kvkService.getMaatschappelijkeActiviteit(authentication))
            verifyNoInteractions(kvkClient)
        }

    @Test
    fun `getKvkData geeft null als de client faalt`() =
        runTest {
            whenever(kvkClient.getMaatschappelijkeActiviteit("123")).thenThrow(IllegalStateException("kapot"))

            assertNull(kvkService.getKvkData("123"))
        }

    @Test
    fun `getKvkData zet de opgehaalde vestiging als een vestigingsnummer is opgegeven`() =
        runTest {
            val hoofdvestiging = mock<Vestiging>()
            val vestiging = mock<Vestiging>()
            val embedded = Embedded(hoofdvestiging = hoofdvestiging, eigenaar = mock<Eigenaar>())
            val activiteit = mock<MaatschappelijkeActiviteit>()
            whenever(activiteit.embedded).thenReturn(embedded)
            whenever(kvkClient.getMaatschappelijkeActiviteit("123")).thenReturn(activiteit)
            whenever(kvkClient.getVestiging("456")).thenReturn(vestiging)

            kvkService.getKvkData("123", "456")

            assertSame(vestiging, embedded.vestiging)
        }

    @Test
    fun `getKvkData houdt de hoofdvestiging als de vestiging niet opgehaald kan worden`() =
        runTest {
            val hoofdvestiging = mock<Vestiging>()
            val embedded = Embedded(hoofdvestiging = hoofdvestiging, eigenaar = mock<Eigenaar>())
            val activiteit = mock<MaatschappelijkeActiviteit>()
            whenever(activiteit.embedded).thenReturn(embedded)
            whenever(kvkClient.getMaatschappelijkeActiviteit("123")).thenReturn(activiteit)
            whenever(kvkClient.getVestiging("456")).thenThrow(IllegalStateException("kapot"))

            kvkService.getKvkData("123", "456")

            assertSame(hoofdvestiging, embedded.vestiging)
        }
}