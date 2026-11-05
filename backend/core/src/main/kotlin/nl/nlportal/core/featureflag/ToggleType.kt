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
package nl.nlportal.core.featureflag

/** The four kinds of feature toggles; see Fowler's "Feature Toggles" article. */
enum class ToggleType {
    /** Operational control of runtime behaviour; may live long. */
    OPS,

    /** Grants access to a capability; may live long and is fail-closed. */
    PERMISSION,

    /** Experiment or canary; short-lived. */
    EXPERIMENT,

    /** Releases unfinished or new functionality gradually; short-lived. */
    RELEASE,
}