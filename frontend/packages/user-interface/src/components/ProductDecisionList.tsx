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
import DecisionsList, { Decisions } from "./DecisionsList";
import { useLazyQuery } from "@apollo/client/react";
import { GetOpenProductActiePrefillDocument } from "@nl-portal/nl-portal-api";

interface Props {
  loading: boolean;
  titleTranslationId?: string | null;
  productId: string;
  decisions?: Decisions[];
}

const ProductDecisionsList = ({
  loading,
  titleTranslationId = "decisionsList.title",
  productId,
  decisions,
}: Props) => {
  const [fetchPrefill] = useLazyQuery(GetOpenProductActiePrefillDocument);

  const handlePrefillClick = (key: string) => {
    fetchPrefill({
      variables: {
        naam: key,
        productId: productId,
      },
    })
      .then((result) => {
        const data = result?.data;
        window.location.href = `${data?.getOpenProductActiePrefill?.formulierUrl}?initial_data_reference=${data?.getOpenProductActiePrefill?.objectId}`;
      })
      .catch((error) => {
        console.error("Error fetching decision prefill:", error);
      });
  };

  return (
    <DecisionsList
      loading={loading}
      titleTranslationId={titleTranslationId}
      decisions={decisions}
      onPrefillClick={handlePrefillClick}
    />
  );
};

export default ProductDecisionsList;
