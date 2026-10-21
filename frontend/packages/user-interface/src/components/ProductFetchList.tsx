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
import {
  GetOpenProductenDocument,
  OpenProductProduct,
} from "@nl-portal/nl-portal-api";
import { useQuery } from "@apollo/client/react";
import TableList from "./TableList";
import { useIntl } from "react-intl";
import { useOutletContext } from "react-router";
import { RouterOutletContext } from "../interfaces/router-outlet-context";
import { ProductSettings } from "../interfaces/product-types";
import { getProductValue } from "../utils/get-product-value";

type ProductFetchListProps = ProductSettings & {
  slug: string;
  productLength: number;
  pagination?: boolean;
};

export const ProductFetchList = ({
  slug,
  productTypeSlug,
  productTypeCodes,
  titleTranslationId,
  headerTranslationIds,
  dataMapping,
  productLength,
  pagination,
}: ProductFetchListProps) => {
  const intl = useIntl();
  const { paths } = useOutletContext<RouterOutletContext>();
  const { data, loading, error, refetch } = useQuery(GetOpenProductenDocument, {
    variables: {
      productTypeCodes: productTypeCodes.filter((code) => code !== undefined),
      pageSize: productLength,
    },
  });

  const onPageChange = async (index: number) => {
    try {
      await refetch({ pageNumber: index + 1 });
    } catch (error) {
      console.error("Error fetching products:", error);
    }
  };

  const producten = data?.getOpenProducten.content as
    OpenProductProduct[] | undefined;

  if (!producten) return null;

  return (
    <TableList
      loading={loading}
      error={Boolean(error)}
      titleTranslationId={titleTranslationId}
      indexLimit={
        pagination
          ? data?.getOpenProducten.totalPages &&
            data.getOpenProducten.totalPages - 1
          : undefined
      }
      totalAmount={
        !pagination &&
        data?.getOpenProducten.totalElements &&
        data?.getOpenProducten.totalElements > productLength
          ? data?.getOpenProducten.totalElements
          : undefined
      }
      readMoreLink={paths.themeList(slug, productTypeSlug)}
      headers={headerTranslationIds.map((id) => intl.formatMessage({ id }))}
      rows={producten.map((product) =>
        dataMapping.map((map) => ({
          href: paths.themeDetails(slug, productTypeSlug, product.uuid),
          children: getProductValue(product, map),
        })),
      )}
      onChange={onPageChange}
    />
  );
};
