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
package nl.nlportal.core.frontend.configuration

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "nl-portal.frontend", ignoreUnknownFields = true)
class FrontendConfigurationProperties {
    var theme: FrontendTheme = FrontendTheme()
    var properties: FrontendProperties = FrontendProperties()
    var modulesWhitelist: List<String> = emptyList()

    class FrontendTheme(
        var style: String? = null,
        var logo: String? = null,
    )

    @JsonInclude(Include.NON_NULL)
    class FrontendProperties {
        var myAddressResearchUrl: String? = null
        var myAddressResearchMoreInfoUrl: String? = null
        var myAddressChangeUrl: String? = null
        var myNameChangeUrl: String? = null
        var myGenderChangeUrl: String? = null
        var myBrpChangeUrl: String? = null
        var myBrpConfidentiallyChangeUrl: String? = null
        var themeClass: String? = null
        var messageCountPollingInterval: Int? = null
        var overviewMaintenanceAlertTitleNl: String? = null
        var overviewMaintenanceAlertTitleEn: String? = null
        var overviewMaintenanceAlertTextNl: String? = null
        var overviewMaintenanceAlertTextEn: String? = null
        var overviewCurrentCasesPreviewLength: Int? = null
        var overviewCurrentTasksPreviewLength: Int? = null
        var custom: String? = null
    }
}