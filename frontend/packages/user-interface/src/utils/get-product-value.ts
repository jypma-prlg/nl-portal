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
import {
  DataMappingFn,
  DataMappingItem,
  DataObjectPath,
  SyncReactNode,
} from "../interfaces/product-types";

const isMappingFn = (m: DataMappingItem): m is DataMappingFn =>
  typeof m === "function";

const isDataObjectPath = (m: DataMappingItem): m is DataObjectPath =>
  typeof m === "string" && m.startsWith("dataobject.");

export const getProductValue = (
  product: OpenProductProduct,
  map: DataMappingItem,
): SyncReactNode => {
  if (isMappingFn(map)) {
    return map(product);
  }

  if (isDataObjectPath(map)) {
    const value = map
      .split(".")
      .slice(1)
      .reduce((acc, key) => acc?.[key], product.dataobject);

    return value == null ? null : String(value);
  }

  const value = product[map];
  return value == null ? null : String(value);
};
