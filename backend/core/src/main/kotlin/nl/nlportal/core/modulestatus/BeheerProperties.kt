/*
 * Copyright 2026 PinkRoccade Local Government B.V.
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
package nl.nlportal.core.modulestatus

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Inloggegevens voor het beheerendpoint (HTTP Basic). Zonder deze waarden blijft het endpoint dicht (fail closed).
 * `wachtwoordHash` is een Spring Security-hash met prefix, bijvoorbeeld `{bcrypt}$2a$...`.
 */
@ConfigurationProperties(prefix = "nl-portal.beheer")
class BeheerProperties {
    var gebruikersnaam: String? = null
    var wachtwoordHash: String? = null

    /** Naam en hash als beide zijn ingesteld, anders `null` (dan is er geen account). */
    fun inloggegevens(): Pair<String, String>? {
        val naam = gebruikersnaam
        val hash = wachtwoordHash
        return if (naam.isNullOrBlank() || hash.isNullOrBlank()) null else naam to hash
    }

    fun isGeconfigureerd() = inloggegevens() != null

    /** Waarschuwingen voor configuraties die in productie niet horen; `heeftBeheerpoort` is of er een aparte poort is. */
    fun waarschuwingen(heeftBeheerpoort: Boolean): List<String> =
        buildList {
            if (wachtwoordHash?.startsWith("{noop}") == true) {
                add("Beheerwachtwoord is een {noop}-hash: alleen toegestaan voor lokaal gebruik")
            }
            if (isGeconfigureerd() && !heeftBeheerpoort) {
                add("Beheerendpoint staat op de publieke poort: zet MANAGEMENT_SERVER_PORT voor een aparte beheerpoort")
            }
        }
}