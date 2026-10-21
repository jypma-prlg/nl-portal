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
import { FormattedMessage } from "react-intl";
import {
  GetOpenProductenDocument,
  OpenProductProduct,
} from "@nl-portal/nl-portal-api";
import { useQuery } from "@apollo/client/react";
import PageHeader from "../components/PageHeader";
import PageGrid from "../components/PageGrid";
import ProductsList from "../components/ProductsList";

const ProductsPage = () => {
  const { data, loading, error, refetch } = useQuery(GetOpenProductenDocument, {
    variables: { pageSize: 10 },
  });
  const producten = data?.getOpenProducten?.content as
    OpenProductProduct[] | undefined;

  const onPageChange = async (index: number) => {
    try {
      await refetch({ pageNumber: index + 1 });
    } catch (error) {
      console.error("Error fetching tasks:", error);
    }
  };

  console.log("Producten:", producten);

  return (
    <PageGrid variant="medium">
      <PageHeader
        title={<FormattedMessage id="pageTitles.products" />}
      ></PageHeader>
      <ProductsList
        loading={loading}
        error={Boolean(error)}
        titleTranslationId={null}
        products={producten}
        onChange={onPageChange}
        index={(data?.getOpenProducten?.number || 1) - 1}
        indexLimit={(data?.getOpenProducten?.totalPages || 1) - 1}
      />
    </PageGrid>
  );
};

export default ProductsPage;
