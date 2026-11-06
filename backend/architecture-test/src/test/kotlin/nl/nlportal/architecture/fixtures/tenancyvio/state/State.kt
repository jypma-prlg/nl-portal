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
package nl.nlportal.architecture.fixtures.tenancyvio.state

import dev.openfeature.sdk.OpenFeatureAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/** Eigen klasse met dezelfde naam als die van de payment-SDK: de regel matcht op naam, zodat de SDK niet op het fixture-classpath hoeft. */
class InMemorySecretKeyStore

@Suppress("OPT_IN_USAGE")
class ViolatingState {
    /** OVERTREDING: ThreadLocal. */
    val threadLocal = ThreadLocal<String>()

    /** OVERTREDING: InMemorySecretKeyStore. */
    val keyStore = InMemorySecretKeyStore()

    /** OVERTREDING: GlobalScope. */
    fun viaGlobalScope() = GlobalScope.launch { }

    /** OVERTREDING: runBlocking. */
    fun viaRunBlocking() = runBlocking { }

    /** OVERTREDING: nieuwe CoroutineScope. */
    fun viaNewScope() = CoroutineScope(Dispatchers.Default).launch { }

    /** OVERTREDING: MainScope. */
    fun viaMainScope() = MainScope().launch { }

    /** OVERTREDING: OpenFeatureAPI.getInstance() buiten core.featureflag. */
    fun viaOpenFeature() = OpenFeatureAPI.getInstance()
}