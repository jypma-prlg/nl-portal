/*
 * Copyright 2015-2023 Den Haag, Ritense, the Netherlands.
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
package nl.nlportal.form.autoconfigure

import nl.nlportal.form.service.ObjectsApiFormDefinitionService
import nl.nlportal.zgw.objectenapi.service.ObjectenApiService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@AutoConfiguration
@EnableConfigurationProperties(FormConfig::class)
@ConditionalOnProperty(prefix = "nl-portal.config", name = ["objectenapi.enabled"], havingValue = "true")
class FormAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(ObjectsApiFormDefinitionService::class)
    fun objectsApiFormDefinitionService(
        objectenApiService: ObjectenApiService,
        formConfig: FormConfig,
    ): ObjectsApiFormDefinitionService = ObjectsApiFormDefinitionService(objectenApiService, formConfig)
}