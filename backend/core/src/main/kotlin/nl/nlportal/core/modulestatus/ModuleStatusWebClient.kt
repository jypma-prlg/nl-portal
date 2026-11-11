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

import nl.nlportal.core.util.ModuleNotConfiguredException
import org.springframework.web.reactive.function.client.ExchangeFilterFunction

/**
 * WebClient-filter dat de uitkomst van elke echte aanroep als passieve status vastlegt. Zet hem als eerste
 * filter, zodat ook fouten uit latere filters (zoals ontbrekende geheimen) meetellen.
 * Verbindingsfout, time-out of 5xx is `UNREACHABLE`; elk ander antwoord (ook 4xx) betekent bereikbaar.
 */
fun ModuleStatusRegistry.webClientFilter(
    module: String,
    api: String,
): ExchangeFilterFunction =
    ExchangeFilterFunction { request, next ->
        next
            .exchange(request)
            .doOnNext { response ->
                if (response.statusCode().is5xxServerError) {
                    recordFailure(module, api, Oorzaak.UNREACHABLE)
                } else {
                    recordSuccess(module, api)
                }
            }.doOnError { recordFailure(module, api, oorzaakVan(it)) }
    }

/** Voert [block] uit en legt een ontbrekende configuratie vast als `NOT_CONFIGURED` voordat de fout doorgaat. */
fun <T> ModuleStatusRegistry.bewaakConfiguratie(
    module: String,
    api: String,
    block: () -> T,
): T =
    try {
        block()
    } catch (e: ModuleNotConfiguredException) {
        recordFailure(module, api, Oorzaak.NOT_CONFIGURED)
        throw e
    }

private fun oorzaakVan(fout: Throwable): Oorzaak = if (generateSequence(fout) { it.cause }.any { it is ModuleNotConfiguredException }) Oorzaak.NOT_CONFIGURED else Oorzaak.UNREACHABLE