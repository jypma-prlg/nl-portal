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
package nl.nlportal.core.modulestatus

import nl.nlportal.core.featureflag.FeatureContextFactory
import nl.nlportal.core.frontend.configuration.FrontendModuleConfigurationProperties
import nl.nlportal.core.modulegate.ModuleGate
import org.springframework.boot.actuate.endpoint.annotation.Endpoint
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation

/** Beheerendpoint `/actuator/modulestatus`: gedetailleerde status per module en API. Alleen op de beheerpoort. */
@Endpoint(id = "modulestatus")
class ModuleStatusEndpoint(
    private val registry: ModuleStatusRegistry,
    private val modules: FrontendModuleConfigurationProperties,
    private val moduleGate: ModuleGate,
    private val featureContextFactory: FeatureContextFactory,
) {
    /** Modules with a module flag are on or off by that flag; the others by their old `enabled` property. */
    @ReadOperation
    fun modules(): List<ModuleStatus> = registry.snapshot(modules.config.mapValues { it.value.enabled } + moduleGate.status(featureContextFactory.create(null)))
}