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
export { default as ApiContext, ApiProvider } from "./contexts/ApiContext";
export * from "./constants/apollo-cache";
export * from "./constants/scalars";
export * from "./generated/types";
export * from "./generated/graphql";

export * from "./hooks/useUserContactMutation";

export * from "./queries/get-taken-v2";
export * from "./queries/get-zaken";
export * from "./queries/get-persoon-v2";
export * from "./queries/get-user-klant-contacten";
export * from "./queries/get-zaak";
export * from "./queries/get-open-product-hoofd-themas-by-producten";
export * from "./queries/berichten/get-unopened-berichten-count";
export * from "./queries/get-user-digitale-adressen";
