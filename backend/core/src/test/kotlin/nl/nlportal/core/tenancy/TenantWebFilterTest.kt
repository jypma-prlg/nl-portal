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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

/**
 * PROTOTYPE (wayfinder #204). Bewijst op WebFlux-niveau (zonder GraphQL): hostresolutie, 404 zonder standaardtenant,
 * 403 bij issuer-mismatch, anoniem pad alleen uit de host, en behoud van de tenant over coroutine- en threadwissels.
 */
class TenantWebFilterTest {
    private val oinA = "00000001003214345000"
    private val oinB = "00000001003214346000"
    private val issuerA = "https://keycloak.test/realms/gemeente-a"
    private val issuerB = "https://keycloak.test/realms/gemeente-b"

    private val directory =
        TenantDirectory.from(
            mapOf(
                oinA to
                    TenantDefinition(listOf("gemeente-a.test"), listOf(issuerA)),
                oinB to
                    TenantDefinition(listOf("gemeente-b.test"), listOf(issuerB)),
            ),
        )

    /** Vervangt Spring Security in deze test: zet een JWT met de issuer uit `X-Test-Issuer` in de Reactor Context. */
    private val testAuthentication =
        object : WebFilter {
            override fun filter(
                exchange: ServerWebExchange,
                chain: WebFilterChain,
            ): Mono<Void> {
                val issuer = exchange.request.headers.getFirst("X-Test-Issuer") ?: return chain.filter(exchange)
                val jwt = Jwt("t", Instant.now(), Instant.now().plusSeconds(60), mapOf("alg" to "none"), mapOf("iss" to issuer, "sub" to "u"))
                return chain
                    .filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(JwtAuthenticationToken(jwt)))
            }
        }

    private val client: WebTestClient =
        WebTestClient
            .bindToController(Probe())
            .webFilter<WebTestClient.ControllerSpec>(TenantWebFilter(directory), testAuthentication, TenantIssuerConsistencyFilter(directory))
            .build()

    private fun get(
        host: String,
        path: String,
        issuer: String? = null,
    ) = client
        .get()
        .uri("http://$host/$path")
        .apply { issuer?.let { header("X-Test-Issuer", it) } }
        .exchange()

    private fun body(
        host: String,
        path: String,
    ) = get(host, path)
        .expectStatus()
        .isOk
        .expectBody(String::class.java)
        .returnResult()
        .responseBody!!

    @Test
    fun `suspend functie ziet de tenant van de host`() {
        assertThat(body("gemeente-a.test", "whoami")).isEqualTo(oinA)
        assertThat(body("gemeente-b.test", "whoami")).isEqualTo(oinB)
    }

    @Test
    fun `hostnaam met poort en hoofdletters wordt herkend`() {
        assertThat(body("Gemeente-A.TEST:8443", "whoami")).isEqualTo(oinA)
    }

    @Test
    fun `onbekend domein geeft 404 en de handler draait niet`() {
        Probe.calls.set(0)
        get("onbekend.test", "whoami").expectStatus().isNotFound
        assertThat(Probe.calls.get()).isZero()
    }

    @Test
    fun `tenant blijft behouden over coroutine- en threadwissels`() {
        val result = body("gemeente-b.test", "hop")
        assertThat(result).isEqualTo("ok:$oinB")
    }

    @Test
    fun `een nieuwe CoroutineScope verliest de tenant en faalt gesloten`() {
        get("gemeente-a.test", "newscope").expectStatus().is5xxServerError
        get("gemeente-a.test", "globalscope").expectStatus().is5xxServerError
    }

    @Test
    fun `reactieve code leest de tenant met currentMono`() {
        assertThat(body("gemeente-a.test", "mono")).isEqualTo(oinA)
    }

    @Test
    fun `anoniem pad krijgt de tenant alleen uit de host`() {
        get("gemeente-a.test", "api/public/frontend")
            .expectStatus()
            .isOk
            .expectBody(String::class.java)
            .isEqualTo(oinA)
    }

    @Test
    fun `issuer van dezelfde tenant als de host is toegestaan`() {
        get("gemeente-a.test", "whoami", issuerA).expectStatus().isOk
    }

    @Test
    fun `issuer van een andere tenant dan de host geeft 403 en de handler draait niet`() {
        Probe.calls.set(0)
        get("gemeente-a.test", "whoami", issuerB).expectStatus().isForbidden
        assertThat(Probe.calls.get()).isZero()
    }

    @Test
    fun `onbekende issuer geeft 403`() {
        get("gemeente-a.test", "whoami", "https://evil.test/realms/x").expectStatus().isForbidden
    }

    @Test
    fun `lezen zonder TenantWebFilter faalt gesloten`() {
        val bare = WebTestClient.bindToController(Probe()).build()
        bare
            .get()
            .uri("/whoami")
            .exchange()
            .expectStatus()
            .is5xxServerError
    }

    @RestController
    class Probe {
        companion object {
            val calls = AtomicInteger()
        }

        @GetMapping("/whoami")
        suspend fun whoami(): String {
            calls.incrementAndGet()
            return TenantContext.current().value
        }

        @GetMapping("/api/public/frontend")
        suspend fun frontend(): String = TenantContext.current().value

        @GetMapping("/mono")
        fun mono(): Mono<String> = TenantContext.currentMono().map { it.value }

        @GetMapping("/hop")
        suspend fun hop(): String {
            val expected = TenantContext.current()
            val threads = mutableSetOf(Thread.currentThread().name)
            val seen = mutableListOf<Oin>()
            withContext(Dispatchers.IO) {
                threads += Thread.currentThread().name
                seen += TenantContext.current()
            }
            delay(5)
            seen += TenantContext.current()
            coroutineScope {
                // parallelle kindcoroutines op een andere dispatcher
                seen +=
                    async(Dispatchers.Default) {
                        threads.add(Thread.currentThread().name)
                        TenantContext.current()
                    }.let { it.await() }
            }
            seen += flow { emit(TenantContext.current()) }.flowOn(Dispatchers.IO).first()
            // een geneste Mono die zelf de contextView leest, aangeroepen vanuit de coroutine
            seen += TenantContext.currentMono().awaitSingle()
            check(seen.all { it == expected }) { "tenant gelekt of verloren: $seen" }
            check(threads.size > 1) { "geen threadwissel gebeurd: $threads" }
            return "ok:$expected"
        }

        @GetMapping("/newscope")
        suspend fun newScope(): String = CoroutineScope(Dispatchers.Default).async { TenantContext.current().value }.await()

        @Suppress("OPT_IN_USAGE")
        @GetMapping("/globalscope")
        suspend fun globalScope(): String = GlobalScope.async { TenantContext.current().value }.await()
    }
}