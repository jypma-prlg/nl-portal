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
package nl.nlportal.zakenapi.prototype

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import nl.nlportal.core.tenancy.TenantContext
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller
import reactor.core.publisher.Mono
import java.util.UUID

/** PROTOTYPE (wayfinder #204). Testresolvers die laten zien of de tenant de Spring for GraphQL data fetchers bereikt. */
@Controller
@ConditionalOnProperty(prefix = "nl-portal.tenancy", name = ["enabled"], havingValue = "true")
class PrototypeTenantGraphQlController(
    private val service: PrototypeTenantZaakService,
) {
    class PrototypeParent

    @QueryMapping
    suspend fun prototypeWhoAmI(): String = TenantContext.current().value

    @QueryMapping
    suspend fun prototypeWhoAmIAfterHop(): String =
        withContext(Dispatchers.IO) {
            delay(5)
            "${TenantContext.current().value}@${Thread.currentThread().name}"
        }

    @QueryMapping
    fun prototypeWhoAmIMono(): Mono<String> = TenantContext.currentMono().map { it.value }

    /** Bewust fout: een nieuwe CoroutineScope verliest de Reactor Context. */
    @QueryMapping
    suspend fun prototypeLostContext(): String = CoroutineScope(Dispatchers.Default).async { TenantContext.current().value }.await()

    @QueryMapping
    suspend fun prototypeZaak(
        @Argument id: UUID,
    ): String = service.zaakJson(id)

    @QueryMapping
    fun prototypeParent() = PrototypeParent()

    @SchemaMapping(typeName = "PrototypeParent", field = "tenant")
    suspend fun tenant(parent: PrototypeParent): String = withContext(Dispatchers.Default) { TenantContext.current().value }
}