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

import dev.openfeature.sdk.EvaluationContext
import nl.nlportal.core.featureflag.FeatureFlag
import nl.nlportal.core.featureflag.FeatureGate
import nl.nlportal.core.featureflag.ToggleType
import nl.nlportal.core.frontend.configuration.FrontendConfigurationProperties
import nl.nlportal.core.frontend.configuration.FrontendModuleConfigurationProperties
import nl.nlportal.core.frontend.configuration.FrontendModuleConfigurationProperties.FrontendModuleProperties
import nl.nlportal.core.frontend.domain.FrontendData
import nl.nlportal.core.frontend.domain.FrontendToggles
import nl.nlportal.core.util.Mapper

class FrontendConfigurationService(
    private val frontendConfigurationProperties: FrontendConfigurationProperties,
    private val frontendModuleConfigurationProperties: FrontendModuleConfigurationProperties,
    private val featureGate: FeatureGate,
) {
    fun getFrontendData(context: EvaluationContext): FrontendData =
        FrontendData(
            modules = getModules(),
            toggles = getToggles(context),
            properties = frontendConfigurationProperties.properties,
            theme = frontendConfigurationProperties.theme,
        )

    private fun getToggles(context: EvaluationContext): FrontendToggles {
        fun enabled(flag: FeatureFlag) = featureGate.isEnabled(flag, context)
        return FrontendToggles(
            messageCountEnabled = enabled(MESSAGE_COUNT),
            casesPartialSearchEnabled = enabled(CASES_PARTIAL_SEARCH),
            openProductEnabled = enabled(OPEN_PRODUCT),
            casesResultExplanationEnabled = enabled(CASES_RESULT_EXPLANATION),
            myInhabitantCountEnabled = enabled(MY_INHABITANT_COUNT),
            casesContactMomentsEnabled = enabled(CASES_CONTACT_MOMENTS),
            overviewIntroEnabled = enabled(OVERVIEW_INTRO),
            overviewMaintenanceAlertEnabled = enabled(OVERVIEW_MAINTENANCE_ALERT),
            custom = Mapper.get().writeValueAsString(featureGate.booleanMap(CUSTOM_TOGGLES_KEY, context)),
        )
    }

    fun getModules(): Map<String, FrontendModuleProperties> {
        val modulesWhitelist = frontendConfigurationProperties.modulesWhitelist
        return frontendModuleConfigurationProperties.config
            .filterKeys { it in modulesWhitelist }
            .mapKeys { (key, _) -> key.replace("api", "") }
    }

    private companion object {
        fun frontendFlag(name: String) = FeatureFlag("ops.frontend.$name", ToggleType.OPS)

        val MESSAGE_COUNT = frontendFlag("message-count")
        val CASES_PARTIAL_SEARCH = frontendFlag("cases-partial-search")
        val OPEN_PRODUCT = frontendFlag("open-product")
        val CASES_RESULT_EXPLANATION = frontendFlag("cases-result-explanation")
        val MY_INHABITANT_COUNT = frontendFlag("my-inhabitant-count")
        val CASES_CONTACT_MOMENTS = frontendFlag("cases-contact-moments")
        val OVERVIEW_INTRO = frontendFlag("overview-intro")
        val OVERVIEW_MAINTENANCE_ALERT = frontendFlag("overview-maintenance-alert")
        const val CUSTOM_TOGGLES_KEY = "ops.frontend.custom-toggles"
    }
}