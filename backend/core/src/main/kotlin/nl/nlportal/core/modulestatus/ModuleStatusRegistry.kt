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

import java.time.Clock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Reden waarom een API niet werkt. Bewust een vaste code: geen foutmelding, URL of geheim.
 * Volgorde = ernst: de eerste waarde wint als een module meerdere oorzaken heeft.
 */
enum class Oorzaak { NOT_CONFIGURED, UNREACHABLE }

/** `UNKNOWN`: aangezet maar nog geen verkeer. `DISABLED`: uitgezet in de configuratie (geen storing). */
enum class ModuleState { UP, DEGRADED, NOT_CONFIGURED, UNKNOWN, DISABLED }

data class LaatsteFout(
    val oorzaak: Oorzaak,
    val tijdstip: Instant,
)

data class ApiStatus(
    val api: String,
    val bereikbaar: Boolean,
    val laatstGecontroleerd: Instant,
    val laatsteFout: LaatsteFout?,
)

data class ModuleStatus(
    val module: String,
    val status: ModuleState,
    val reden: Oorzaak?,
    val apis: List<ApiStatus>,
)

/**
 * Passieve status per module en API, gevuld uit de laatste echte request-uitkomst (geen eigen polling).
 * Een succes wist de actuele storing maar bewaart de laatste fout voor de beheerder.
 */
class ModuleStatusRegistry(
    private val clock: Clock = Clock.systemUTC(),
) {
    private val apis = ConcurrentHashMap<Pair<String, String>, ApiStatus>()

    fun recordSuccess(
        module: String,
        api: String,
    ) {
        apis.compute(module to api) { _, huidig ->
            ApiStatus(api, true, clock.instant(), huidig?.laatsteFout)
        }
    }

    fun recordFailure(
        module: String,
        api: String,
        oorzaak: Oorzaak,
    ) {
        val nu = clock.instant()
        apis[module to api] = ApiStatus(api, false, nu, LaatsteFout(oorzaak, nu))
    }

    /** `enabled`: modulenaam naar aan/uit uit de configuratie; de modulenaam is dezelfde als bij `recordSuccess`/`recordFailure`. */
    fun snapshot(enabled: Map<String, Boolean> = emptyMap()): List<ModuleStatus> {
        val perModule = apis.entries.groupBy({ it.key.first }, { it.value })
        return (enabled.keys + perModule.keys).toSortedSet().map { module ->
            val lijst = perModule[module].orEmpty().sortedBy { it.api }
            val reden = lijst.filter { !it.bereikbaar }.mapNotNull { it.laatsteFout?.oorzaak }.minOrNull()
            val status =
                when {
                    enabled[module] == false -> ModuleState.DISABLED
                    lijst.isEmpty() -> ModuleState.UNKNOWN
                    reden == null -> ModuleState.UP
                    reden == Oorzaak.NOT_CONFIGURED -> ModuleState.NOT_CONFIGURED
                    else -> ModuleState.DEGRADED
                }
            ModuleStatus(module, status, reden, lijst)
        }
    }
}