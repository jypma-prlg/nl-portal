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
package nl.nlportal.core.koppeling

import java.net.URI

class Koppeling {
    var url: String? = null
    var auth: KoppelingAuth = KoppelingAuth()
    var leverancier: String? = null
    var versie: String? = null

    /**
     * Een `http`-url naar een publiek adres. Localhost en servicenamen zonder punt (een containernetwerk)
     * tellen niet mee: daar is `http` gebruikelijk en blijft het verkeer binnen de host of het netwerk.
     */
    fun isOnbeveiligd(): Boolean {
        val uri = url?.let { runCatching { URI(it) }.getOrNull() } ?: return false
        val host = uri.host ?: return false
        val lokaal = host == "localhost" || host.endsWith(".localhost") || host.startsWith("127.") || !host.contains('.')
        return uri.scheme.equals("http", ignoreCase = true) && !lokaal
    }

    override fun toString(): String = "Koppeling(url=$url, auth=$auth, leverancier=$leverancier, versie=$versie)"
}

enum class KoppelingAuthType {
    API_KEY,
    BEARER,
    ZGW_JWT,
    OAUTH2_CLIENT_CREDENTIALS,
    OAUTH2_PRIVATE_KEY_JWT,
}

class KoppelingAuth {
    var type: KoppelingAuthType? = null
    var headerName: String? = null
    var value: String? = null
    var token: String? = null
    var clientId: String? = null
    var secret: String? = null
    var tokenUrl: String? = null
    var privateKeyFile: String? = null

    /** Secrets (`value`, `token`, `secret`) komen nooit in de tekstweergave, dus ook niet in logs of statuspagina's. */
    override fun toString(): String =
        "KoppelingAuth(type=$type, headerName=$headerName, clientId=$clientId, tokenUrl=$tokenUrl, " +
            "privateKeyFile=$privateKeyFile, value=${masker(value)}, token=${masker(token)}, secret=${masker(secret)})"

    private fun masker(waarde: String?) = if (waarde == null) "null" else "***"
}