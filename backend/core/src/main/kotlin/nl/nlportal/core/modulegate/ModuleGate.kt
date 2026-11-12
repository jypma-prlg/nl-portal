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
package nl.nlportal.core.modulegate

import dev.openfeature.sdk.EvaluationContext
import nl.nlportal.core.featureflag.FeatureContextFactory
import nl.nlportal.core.featureflag.FeatureGate
import org.springframework.security.core.Authentication

/**
 * Per-request gate of a module. The module's beans are always registered; its GraphQL queries and REST
 * resources call [require] first, so a disabled module answers "not available" instead of failing or
 * returning empty data. The GraphQL schema does not change: the fields stay visible in introspection.
 *
 * The context is passed explicitly, never read from thread-bound state, which does not survive thread hops in WebFlux.
 */
class ModuleGate(
    private val featureGate: FeatureGate,
    private val featureContextFactory: FeatureContextFactory,
    private val modules: List<ModuleFlag>,
) {
    fun isEnabled(
        module: ModuleFlag,
        context: EvaluationContext,
    ): Boolean = featureGate.isEnabled(module.flag, context)

    /** Throws [ModuleNotAvailableException] when [module] is off for the request of [authentication]. */
    fun require(
        module: ModuleFlag,
        authentication: Authentication?,
    ) {
        if (!isEnabled(module, featureContextFactory.create(authentication))) {
            throw ModuleNotAvailableException(module.module)
        }
    }

    /** The outcome of every registered module flag, by module name. */
    fun status(context: EvaluationContext): Map<String, Boolean> = modules.associate { it.module to isEnabled(it, context) }
}