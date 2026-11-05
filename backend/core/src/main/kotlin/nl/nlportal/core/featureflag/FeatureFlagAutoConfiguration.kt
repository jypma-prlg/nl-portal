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
package nl.nlportal.core.featureflag

import dev.openfeature.contrib.providers.flagd.Config
import dev.openfeature.contrib.providers.flagd.FlagdOptions
import dev.openfeature.contrib.providers.flagd.FlagdProvider
import dev.openfeature.sdk.FeatureProvider
import dev.openfeature.sdk.OpenFeatureAPI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ClassPathResource

/**
 * Wires OpenFeature. The default provider is flagd in file mode, so NL Portal works without a flag server;
 * define a [FeatureProvider] bean to plug in another OpenFeature provider.
 */
@AutoConfiguration
@EnableConfigurationProperties(FeatureFlagProperties::class)
class FeatureFlagAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(FeatureProvider::class)
    fun featureProvider(properties: FeatureFlagProperties): FeatureProvider =
        FlagdProvider(
            FlagdOptions
                .builder()
                .resolverType(Config.Resolver.FILE)
                .offlineFlagSourcePath(properties.file ?: bundledFlagFile())
                .offlinePollIntervalMs(properties.pollIntervalMs)
                .build(),
        )

    @Bean
    @ConditionalOnMissingBean(OpenFeatureAPI::class)
    fun openFeatureApi(featureProvider: FeatureProvider): OpenFeatureAPI = OpenFeatureAPI.getInstance().also { it.setProviderAndWait(featureProvider) }

    @Bean
    @ConditionalOnMissingBean(FeatureGate::class)
    fun featureGate(openFeatureApi: OpenFeatureAPI): FeatureGate = OpenFeatureFeatureGate(openFeatureApi.client)

    // The flagd file provider reads a file system path, so the bundled flag file is copied out of the classpath.
    private fun bundledFlagFile(): String {
        val file = Files.createTempFile("nl-portal-feature-flags", ".flagd.json")
        ClassPathResource("feature-flags/default.flagd.json").inputStream.use {
            Files.copy(it, file, StandardCopyOption.REPLACE_EXISTING)
        }
        file.toFile().deleteOnExit()
        return file.toString()
    }
}