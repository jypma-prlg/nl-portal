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
package nl.nlportal.commonground.authentication

import nl.nlportal.commonground.authentication.KeycloakConfig.KeycloakCredentials
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

internal class KeycloakConfigBindingTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TestConfiguration::class.java))

    @Test
    fun `an unresolved audience placeholder default binds as blank, not as a literal`() {
        bind("nl-portal.authentication.keycloak.audience=") {
            assertEquals("", it.audience)
        }
    }

    @Test
    fun `audience is blank when the property is absent`() {
        bind("nl-portal.authentication.keycloak.resource=nl-portal-m2m") {
            assertEquals("", it.audience)
        }
    }

    @Test
    fun `the whole app image configuration shape binds`() {
        bind(
            "nl-portal.authentication.keycloak.resource=nl-portal-m2m",
            "nl-portal.authentication.keycloak.audience=",
            "nl-portal.authentication.keycloak.credentials.secret=a-secret",
        ) {
            assertEquals("nl-portal-m2m", it.resource)
            assertEquals("", it.audience)
            assertEquals("a-secret", it.credentials.secret)
        }
    }

    @Test
    fun `the published constructors are preserved`() {
        val parameterTypes =
            KeycloakConfig::class.java.constructors
                .map { constructor -> constructor.parameterTypes.toList() }
                .toSet()

        assertTrue(parameterTypes.contains(emptyList()), "no-arg constructor is missing")
        assertTrue(
            parameterTypes.contains(
                listOf(String::class.java, String::class.java, KeycloakCredentials::class.java),
            ),
            "the three argument constructor is missing",
        )

        assertTrue(
            parameterTypes.contains(listOf(String::class.java)),
            "single String constructor is missing",
        )
        assertTrue(
            parameterTypes.contains(listOf(String::class.java, String::class.java)),
            "two String constructor is missing",
        )
    }

    private fun bind(
        vararg properties: String,
        assertions: (KeycloakConfig) -> Unit,
    ) {
        contextRunner.withPropertyValues(*properties).run { context ->
            assertions(context.getBean(KeycloakConfig::class.java))
        }
    }

    @Configuration
    @EnableConfigurationProperties(KeycloakConfig::class)
    internal class TestConfiguration
}