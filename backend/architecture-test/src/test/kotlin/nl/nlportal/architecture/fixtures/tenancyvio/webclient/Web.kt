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
package nl.nlportal.architecture.fixtures.tenancyvio.webclient

import org.springframework.web.reactive.function.client.WebClient

/** OVERTREDING: WebClient.create(). */
class ViolatingWebClientCreate {
    fun client(): WebClient = WebClient.create()
}

/** OVERTREDING: WebClient.builder(). */
class ViolatingWebClientBuilder {
    fun client(): WebClient = WebClient.builder().baseUrl("http://x").build()
}

/** TOEGESTAAN: een geïnjecteerde builder gebruiken. */
class InjectedBuilderUser(
    private val builder: WebClient.Builder,
) {
    fun client(): WebClient = builder.clone().baseUrl("http://x").build()
}