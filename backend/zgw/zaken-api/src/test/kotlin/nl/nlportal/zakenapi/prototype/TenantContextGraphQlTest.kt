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

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.graphql.test.tester.HttpGraphQlTester
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.core.Ordered
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.server.WebFilter
import java.time.Instant
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.Base64
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * PROTOTYPE (wayfinder #204). Onbewezen punt (a): bereikt de tenantcontext Spring for GraphQL data fetchers en
 * suspend-resolvers, en blijft hij behouden over threadwissels? Plus: gaat een zakenapi-call echt naar de
 * tenant-eigen Zaken API met de tenant-eigen credentials, en lekt er niets bij parallelle requests?
 */
@SpringBootTest(properties = ["nl-portal.tenancy.enabled=true", "spring.graphql.schema.locations=classpath*:graphql/**/,classpath*:prototype-graphql/**/"])
@AutoConfigureWebTestClient(timeout = "30000")
@Import(TenantContextGraphQlTest.TestAuthentication::class)
class TenantContextGraphQlTest(
    @Autowired private val webTestClient: WebTestClient,
) {
    /**
     * Vervangt de echte JWT-authenticatie (die in TestApplication is uitgeschakeld): zet, tussen de tenantfilter (-200)
     * en de issuerfilter (-50), een JWT met de issuer uit `X-Test-Issuer` in de Reactor Context. Opmerking: de
     * mockJwt()-mutator van spring-security-test werkt hier niet, want die voegt zijn filter achteraan toe en draait dus
     * ná de issuerfilter.
     */
    @TestConfiguration
    class TestAuthentication {
        @Bean
        fun testAuthenticationFilter(): WebFilter =
            object : WebFilter, Ordered {
                override fun getOrder() = -90

                override fun filter(
                    exchange: org.springframework.web.server.ServerWebExchange,
                    chain: org.springframework.web.server.WebFilterChain,
                ): reactor.core.publisher.Mono<Void> {
                    val issuer = exchange.request.headers.getFirst("X-Test-Issuer") ?: return chain.filter(exchange)
                    val jwt = Jwt("t", Instant.now(), Instant.now().plusSeconds(60), mapOf("alg" to "none"), mapOf("iss" to issuer, "sub" to "u"))
                    return chain.filter(exchange).contextWrite(ReactiveSecurityContextHolder.withAuthentication(JwtAuthenticationToken(jwt)))
                }
            }
    }

    companion object {
        private const val OIN_A = "00000001003214345000"
        private const val OIN_B = "00000001003214346000"
        private const val ISSUER_A = "http://keycloak.test/realms/gemeente-a"
        private const val ISSUER_B = "http://keycloak.test/realms/gemeente-b"

        private fun zakenApiServer(label: String) =
            MockWebServer().apply {
                dispatcher =
                    object : Dispatcher() {
                        override fun dispatch(request: RecordedRequest) = MockResponse().setHeader("Content-Type", "application/json").setBody("""{"bron":"$label"}""")
                    }
                start()
            }

        private val serverA = zakenApiServer("A")
        private val serverB = zakenApiServer("B")

        @JvmStatic
        @DynamicPropertySource
        fun tenants(registry: DynamicPropertyRegistry) {
            fun tenant(
                oin: String,
                domain: String,
                issuer: String,
                server: MockWebServer,
                clientId: String,
            ) {
                registry.add("nl-portal.tenants.$oin.domains[0]") { domain }
                registry.add("nl-portal.tenants.$oin.issuers[0]") { issuer }
                registry.add("nl-portal.tenants.$oin.modules.zakenapi.url") { server.url("/").toString().trimEnd('/') }
                registry.add("nl-portal.tenants.$oin.modules.zakenapi.client-id") { clientId }
                registry.add("nl-portal.tenants.$oin.modules.zakenapi.secret") { "geheim-van-$clientId-".padEnd(40, 'x') }
                registry.add("nl-portal.tenants.$oin.modules.zakenapi.permission") { "true" }
            }
            tenant(OIN_A, "gemeente-a.test", ISSUER_A, serverA, "client-a")
            tenant(OIN_B, "gemeente-b.test", ISSUER_B, serverB, "client-b")
        }

        @JvmStatic
        @AfterAll
        fun stop() {
            serverA.shutdown()
            serverB.shutdown()
        }
    }

    private fun tester(
        host: String,
        issuer: String? = null,
    ): HttpGraphQlTester {
        val base = if (issuer != null) webTestClient.mutate().defaultHeader("X-Test-Issuer", issuer).build() else webTestClient
        return HttpGraphQlTester.create(base.mutate().baseUrl("http://$host/graphql").build())
    }

    private fun field(
        host: String,
        name: String,
        issuer: String? = null,
    ): String =
        tester(host, issuer)
            .document("{ $name }")
            .execute()
            .errors()
            .verify()
            .path(name)
            .entity(String::class.java)
            .get()

    @Test
    fun `suspend resolver ziet de tenant van de host, op twee hosts`() {
        assertThat(field("gemeente-a.test", "prototypeWhoAmI")).isEqualTo(OIN_A)
        assertThat(field("gemeente-b.test", "prototypeWhoAmI")).isEqualTo(OIN_B)
    }

    @Test
    fun `tenant blijft behouden na withContext(Dispatchers IO)`() {
        val result = field("gemeente-b.test", "prototypeWhoAmIAfterHop")
        assertThat(result).startsWith("$OIN_B@")
        assertThat(result).doesNotContain("reactor-http", "Test worker") // echt een andere thread
    }

    @Test
    fun `reactieve resolver leest de tenant met currentMono`() {
        assertThat(field("gemeente-a.test", "prototypeWhoAmIMono")).isEqualTo(OIN_A)
    }

    @Test
    fun `geneste field-resolver (SchemaMapping) ziet de tenant na een dispatcherwissel`() {
        val tenant =
            tester("gemeente-b.test")
                .document("{ prototypeParent { tenant } }")
                .execute()
                .errors()
                .verify()
                .path("prototypeParent.tenant")
                .entity(String::class.java)
                .get()
        assertThat(tenant).isEqualTo(OIN_B)
    }

    @Test
    fun `een nieuwe CoroutineScope in een resolver verliest de tenant en faalt gesloten`() {
        val response = tester("gemeente-a.test").document("{ prototypeLostContext }").execute()
        response.errors().satisfy { assertThat(it).isNotEmpty }
        response.path("prototypeLostContext").valueIsNull()
    }

    @Test
    fun `onbekend domein geeft 404 voor GraphQL`() {
        webTestClient
            .mutate()
            .baseUrl("http://onbekend.test")
            .build()
            .post()
            .uri("/graphql")
            .bodyValue("""{"query":"{ prototypeWhoAmI }"}""")
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus()
            .isNotFound
    }

    @Test
    fun `het echte anonieme pad api-public-frontend valt onder de hostcontrole`() {
        fun status(host: String) =
            webTestClient
                .mutate()
                .baseUrl("http://$host")
                .build()
                .get()
                .uri("/api/public/frontend")
                .exchange()
                .returnResult(String::class.java)
                .status
                .value()
        assertThat(status("gemeente-a.test")).isEqualTo(200)
        assertThat(status("onbekend.test")).isEqualTo(404)
    }

    @Test
    fun `token van tenant B op host van tenant A geeft 403, passende token werkt`() {
        webTestClient
            .mutate()
            .defaultHeader("X-Test-Issuer", ISSUER_B)
            .build()
            .mutate()
            .baseUrl("http://gemeente-a.test")
            .build()
            .post()
            .uri("/graphql")
            .bodyValue("""{"query":"{ prototypeWhoAmI }"}""")
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus()
            .isForbidden
        assertThat(field("gemeente-a.test", "prototypeWhoAmI", ISSUER_A)).isEqualTo(OIN_A)
    }

    @Test
    fun `zakenapi-slice gebruikt de client en credentials van de tenant van het request`() {
        val before = serverA.requestCount to serverB.requestCount
        val id = UUID.randomUUID()
        val zaak =
            tester("gemeente-a.test")
                .document("""{ prototypeZaak(id: "$id") }""")
                .execute()
                .errors()
                .verify()
                .path("prototypeZaak")
                .entity(String::class.java)
                .get()
        assertThat(zaak).contains(""""bron":"A"""")
        assertThat(serverA.requestCount - before.first).isEqualTo(1)
        assertThat(serverB.requestCount - before.second).isZero()

        val request = serverA.takeRequest(5, TimeUnit.SECONDS)!!
        assertThat(request.path).isEqualTo("/zaken/api/v1/zaken/$id")
        val claims = String(Base64.getUrlDecoder().decode(request.getHeader("Authorization")!!.removePrefix("Bearer ").split(".")[1]))
        assertThat(claims).contains("client-a").doesNotContain("client-b")
    }

    @Test
    fun `parallelle requests voor twee tenants lekken niet naar elkaar`() {
        val pool = Executors.newFixedThreadPool(16)
        try {
            val futures =
                (1..200).map { i ->
                    val host = if (i % 2 == 0) "gemeente-a.test" else "gemeente-b.test"
                    val expected = if (i % 2 == 0) OIN_A else OIN_B
                    pool.submit<Boolean> { field(host, "prototypeWhoAmIAfterHop").startsWith("$expected@") }
                }
            assertThat(futures.map { it.get(60, TimeUnit.SECONDS) }).doesNotContain(false)
        } finally {
            pool.shutdownNow()
        }
    }
}