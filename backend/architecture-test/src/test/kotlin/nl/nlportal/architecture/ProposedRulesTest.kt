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
package nl.nlportal.architecture

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ProposedRulesTest {
    @Test
    fun `a proposed rule is not active until a rule with its method name exists`() {
        val status = ProposedRules.status(existingRuleNames = setOf("cycleFree", "coreDoesNotDependOnFeature"))

        assertThat(status).allMatch { !it.active }
        assertThat(status.map { it.rule.methodName })
            .contains("noConfigurationPropertiesOutsideAutoconfig", "noOwnWebClient", "noJvmGlobalState", "noNewCoroutineScopes")
    }

    @Test
    fun `a proposed rule switches to active when a rule with that name lands`() {
        val status = ProposedRules.status(existingRuleNames = setOf("noOwnWebClient"))

        assertThat(status.single { it.active }.rule.methodName).isEqualTo("noOwnWebClient")
    }

    @Test
    fun `the rules that exist today are read from ArchitectureRules`() {
        assertThat(ProposedRules.existingRuleNames()).contains("cycleFree", "coreDoesNotDependOnFeature", "nothingDependsOnApp")
    }
}