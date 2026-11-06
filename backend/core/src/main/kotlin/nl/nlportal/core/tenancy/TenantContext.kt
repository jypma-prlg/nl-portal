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
package nl.nlportal.core.tenancy

import kotlinx.coroutines.reactor.ReactorContext
import reactor.core.publisher.Mono
import kotlin.coroutines.coroutineContext

class TenantContextMissingException : IllegalStateException("Geen tenant in de Reactor Context: request liep niet via TenantWebFilter of de context is verloren (nieuwe CoroutineScope?)")

/**
 * PROTOTYPE (wayfinder #204). De tenant van het huidige request, in de Reactor Context (geen ThreadLocal).
 * Lezen is fail-closed: zonder tenant volgt een exceptie, nooit een standaardtenant.
 */
object TenantContext {
    val KEY: Any = "nl.nlportal.core.tenancy.Oin"

    /** Voor reactieve code. */
    fun currentMono(): Mono<Oin> =
        Mono.deferContextual { ctx ->
            ctx.getOrEmpty<Oin>(KEY).map { Mono.just(it) }.orElseGet { Mono.error(TenantContextMissingException()) }
        }

    /** Voor `suspend fun`: leest de Reactor Context die kotlinx-coroutines-reactor in de coroutinecontext zet. */
    suspend fun current(): Oin =
        coroutineContext[ReactorContext]
            ?.context
            ?.getOrEmpty<Oin>(KEY)
            ?.orElse(null)
            ?: throw TenantContextMissingException()
}