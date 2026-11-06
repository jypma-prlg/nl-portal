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

import nl.nlportal.core.tenancy.TenancyProperties.TenantProperties
import nl.nlportal.core.util.requireConfigured
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.io.ByteArrayResource
import reactor.util.context.Context

/** PROTOTYPE (wayfinder #204). Fail fast bij opstarten en registergedrag. */
class TenantRegistryTest {
    private val oinA = "00000001003214345000"
    private val oinB = "00000001003214346000"

    private fun tenant(
        domain: String,
        issuer: String,
        vararg modules: Pair<String, Map<String, String>>,
    ) = TenantDefinition(listOf(domain), listOf(issuer), mapOf(*modules))

    private val granted = ModulePermission { _, _ -> true }

    @Test
    fun `dubbel domein weigert het opstarten`() {
        assertThatThrownBy {
            TenantDirectory.from(mapOf(oinA to tenant("x.test", "i1"), oinB to tenant("x.test", "i2")))
        }.isInstanceOf(InvalidTenantConfigurationException::class.java).hasMessageContaining("x.test")
    }

    @Test
    fun `dubbele issuer, tenant zonder domein en zonder tenants weigeren het opstarten`() {
        assertThatThrownBy { TenantDirectory.from(mapOf(oinA to tenant("a.test", "i"), oinB to tenant("b.test", "i"))) }
            .isInstanceOf(InvalidTenantConfigurationException::class.java)
        assertThatThrownBy { TenantDirectory.from(mapOf(oinA to TenantDefinition(domains = emptyList(), issuers = listOf("i")))) }
            .isInstanceOf(InvalidTenantConfigurationException::class.java)
        assertThatThrownBy { TenantDirectory.from(emptyMap()) }.isInstanceOf(InvalidTenantConfigurationException::class.java)
    }

    @Test
    fun `issuer buiten de publieke broker weigert het opstarten`() {
        assertThatThrownBy {
            TenantDirectory.from(mapOf(oinA to tenant("a.test", "https://private.test/realms/x")), "https://public.test/")
        }.isInstanceOf(InvalidTenantConfigurationException::class.java).hasMessageContaining("publieke broker")
    }

    @Test
    fun `incomplete modulecongiguratie weigert het opstarten met OIN in de melding`() {
        val directory = TenantDirectory.from(mapOf(oinA to tenant("a.test", "i", "zakenapi" to mapOf("url" to "http://a"))))
        assertThatThrownBy {
            TenantRegistry.build("zakenapi", directory, granted) { it["secret"].requireConfigured("zakenapi", "secret") }
        }.isInstanceOf(InvalidTenantConfigurationException::class.java).hasMessageContaining(oinA)
    }

    @Test
    fun `register levert per tenant een eigen client, of faalt gesloten`() {
        val directory =
            TenantDirectory.from(
                mapOf(
                    oinA to tenant("a.test", "ia", "zakenapi" to mapOf("url" to "http://a", "permission" to "true")),
                    oinB to tenant("b.test", "ib"), // geen zakenapi-configuratie
                ),
            )
        val registry = TenantRegistry.build("zakenapi", directory, granted) { it["url"]!! }

        assertThat(registry.forTenant(Oin(oinA))).isEqualTo("http://a")
        assertThatThrownBy { registry.forTenant(Oin(oinB)) }.isInstanceOf(ModuleNotAvailableException::class.java)
        assertThat(registry.currentMono().contextWrite(Context.of(TenantContext.KEY, Oin(oinA))).block()).isEqualTo("http://a")
        assertThatThrownBy { registry.currentMono().block() }.isInstanceOf(TenantContextMissingException::class.java)
    }

    @Test
    fun `module is pas beschikbaar als configuratie aanwezig is EN de permission-flag aan staat`() {
        val directory = TenantDirectory.from(mapOf(oinA to tenant("a.test", "ia", "zakenapi" to mapOf("url" to "http://a"))))
        val flagOff = ModulePermission { _, _ -> false }
        val registry = TenantRegistry.build("zakenapi", directory, flagOff) { it["url"]!! }
        assertThatThrownBy { registry.forTenant(Oin(oinA)) }.isInstanceOf(ModuleNotAvailableException::class.java)
    }

    @Test
    fun `binding uit YAML met OIN als sleutel, aanhalingstekens vereist voor voorloopnullen`() {
        fun bind(yaml: String): Map<String, TenantProperties> {
            val source = YamlPropertySourceLoader().load("t", ByteArrayResource(yaml.trimIndent().toByteArray())).first()
            val env = StandardEnvironment().also { it.propertySources.addFirst(source) }
            return Binder
                .get(env)
                .bind("nl-portal.tenants", Bindable.mapOf(String::class.java, TenantProperties::class.java))
                .get()
        }

        val quoted = bind("nl-portal:\n  tenants:\n    \"$oinA\":\n      domains: [a.test]\n      issuers: [ia]")
        assertThat(quoted.keys).containsExactly(oinA)

        val unquoted = bind("nl-portal:\n  tenants:\n    $oinA:\n      domains: [a.test]\n      issuers: [ia]")
        // Waarneming: YAML leest 00000001003214345000 als octaal getal (bindt als "69158947328"). Stil fout, maar
        // TenantDirectory.from weigert het opstarten, want Oin() eist 20 cijfers.
        assertThat(unquoted.keys).doesNotContain(oinA)
        assertThatThrownBy { TenantDirectory.from(unquoted.mapValues { it.value.toDefinition() }) }.isInstanceOf(IllegalArgumentException::class.java).hasMessageContaining("20 cijfers")
    }
}