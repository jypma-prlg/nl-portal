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

import io.github.oshai.kotlinlogging.KLogger
import org.springframework.web.reactive.function.client.WebClientResponseException

/**
 * Shared failure policy for OpenProduct's service layer (see ADR 0003): a real failure propagates
 * to the already-existing GraphQLExceptionHandler, instead of being logged and swallowed into an
 * empty/null value. Only an upstream 404 on a get-by-id call is a legitimate "not found", which
 * this translates to `null`; every other failure (5xx, network, malformed data) propagates.
 */
internal suspend fun <T> notFoundAsNull(block: suspend () -> T): T? =
    try {
        block()
    } catch (e: WebClientResponseException.NotFound) {
        null
    }

/**
 * Deliberate, named exception to the propagate-by-default policy above: when enriching a list of
 * related objects (e.g. the zaken/taken/documents/decisions linked to one product), one failing
 * item is logged and skipped so the rest of the list still renders - showing 9 of 10 related items
 * beats a hard error for a citizen-facing portal. Used only where that resilience is a deliberate
 * choice, not as a catch-all.
 */
internal suspend fun <I, O> mapSkippingFailures(
    items: List<I>,
    logger: KLogger,
    describe: (I) -> String,
    action: suspend (I) -> O,
): List<O> {
    val results = mutableListOf<O>()
    items.forEach { item ->
        try {
            results.add(action(item))
        } catch (e: Exception) {
            logger.error(e) { "Skipping ${describe(item)}: ${e.message}" }
        }
    }
    return results
}