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
package nl.nlportal.core.frontend.service

import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import nl.nlportal.core.featureflag.FeatureFlag
import nl.nlportal.core.featureflag.FeatureGate
import nl.nlportal.core.featureflag.ToggleType
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties.FrontendProperties
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties.FrontendToggles
import nl.nlportal.core.frontend.configuration.FrontendModuleConfigurationProperties
import nl.nlportal.core.frontend.configuration.FrontendModuleConfigurationProperties.FrontendModuleProperties
import nl.nlportal.core.frontend.domain.FrontendData

class FrontendConfigurationService(
    private val frontendConfigurationProperties: FrontendConfigurationProperties,
    private val frontendModuleConfigurationProperties: FrontendModuleConfigurationProperties,
    private val featureGate: FeatureGate,
) {
    fun getFrontendData(): FrontendData =
        FrontendData(
            modules = getModules(),
            toggles = getToggles(),
            properties = frontendConfigurationProperties.properties,
            theme = frontendConfigurationProperties.theme,
        )

    // Only the message count toggle is flag-driven so far; the remaining toggles follow in #156.
    private fun getToggles(): FrontendToggles {
        val source = frontendConfigurationProperties.toggles
        return FrontendToggles().apply {
            messageCountEnabled = featureGate.isEnabled(MESSAGE_COUNT)
            casesPartialSearchEnabled = source.casesPartialSearchEnabled
            openProductEnabled = source.openProductEnabled
            casesResultExplanationEnabled = source.casesResultExplanationEnabled
            myInhabitantCountEnabled = source.myInhabitantCountEnabled
            casesContactMomentsEnabled = source.casesContactMomentsEnabled
            overviewIntroEnabled = source.overviewIntroEnabled
            overviewMaintenanceAlertEnabled = source.overviewMaintenanceAlertEnabled
            custom = source.custom
        }
    }

    fun getModules(): Map<String, FrontendModuleProperties> {
        val modulesWhitelist = frontendConfigurationProperties.modulesWhitelist
        return frontendModuleConfigurationProperties.config
            .filterKeys { it in modulesWhitelist }
            .mapKeys { (key, _) -> key.replace("api", "") }
    }

    private companion object {
        val MESSAGE_COUNT = FeatureFlag("ops.frontend.message-count", ToggleType.OPS)
    }
}