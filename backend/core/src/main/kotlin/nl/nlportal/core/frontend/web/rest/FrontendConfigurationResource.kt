/*
 * Copyright 2015-2025 Den Haag, Ritense, the Netherlands.
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
package nl.nlportal.core.frontend.web.rest

import nl.nlportal.core.featureflag.FeatureContextFactory
import nl.nlportal.core.frontend.domain.FrontendData
import nl.nlportal.core.frontend.service.FrontendConfigurationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping(value = ["/api/public/frontend"])
class FrontendConfigurationResource(
    private val frontendConfigurationService: FrontendConfigurationService,
    private val featureContextFactory: FeatureContextFactory,
) {
    @GetMapping(value = [""])
    fun features(): Mono<ResponseEntity<FrontendData>> =
        featureContextFactory.current().map { context ->
            ResponseEntity.ok().header("Content-Type", "application/json").body(frontendConfigurationService.getFrontendData(context))
        }
}