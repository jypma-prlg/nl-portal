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
import { OpenProductProduct } from "@nl-portal/nl-portal-api";

export type DataObjectPath = `dataobject.${string}`;

// Excludes the thenable member React.ReactNode gained for async components,
// since DataMappingFn is always synchronous (Sonar typescript:S6544).
export type SyncReactNode = Exclude<React.ReactNode, Promise<unknown>>;

export type DataMappingFn = (product: OpenProductProduct) => SyncReactNode;

export type DataMappingItem =
  DataMappingFn | keyof OpenProductProduct | DataObjectPath;

export interface ProductSettings {
  productTypeSlug?: string;
  productTypeCodes: (string | undefined)[];
  titleTranslationId: string;
  headerTranslationIds: string[];
  dataMapping: DataMappingItem[];
}
