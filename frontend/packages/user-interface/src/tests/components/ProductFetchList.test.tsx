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
import { describe, it, expect } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import TestProvider, { testPaths as paths } from "../../providers/TestProvider";
import { GetOpenProductenDocument } from "@nl-portal/nl-portal-api";
import { ProductFetchList } from "../../components/ProductFetchList";

const product = (naam: string) => ({
  uuid: `${naam}-uuid`,
  url: null,
  naam,
  startDatum: null,
  eindDatum: null,
  gepubliceerd: true,
  aanmaakDatum: "2024-06-15T21:59:59.999Z",
  updateDatum: "2024-06-15T21:59:59.999Z",
  producttype: {
    code: "CODE",
    uniformeProductNaam: "uniforme-naam",
    toegestaneStatussen: ["GEREED"],
    __typename: "OpenProductProductProductType",
  },
  prijs: null,
  status: "GEREED",
  documenten: [],
  frequentie: "EENMALIG",
  verbruiksobject: null,
  dataobject: null,
  decisions: [],
  acties: [],
  __typename: "OpenProductProduct",
});

const firstPage = {
  request: {
    query: GetOpenProductenDocument,
    variables: { productTypeCodes: ["CODE"], pageSize: 10 },
  },
  result: {
    data: {
      getOpenProducten: {
        number: 1,
        size: 10,
        totalElements: 11,
        numberOfElements: 10,
        totalPages: 2,
        content: [product("PRODUCT OP PAGINA 1")],
        __typename: "OpenProductProductPage",
      },
    },
  },
};

const secondPage = {
  request: {
    query: GetOpenProductenDocument,
    variables: { productTypeCodes: ["CODE"], pageSize: 10, pageNumber: 2 },
  },
  result: {
    data: {
      getOpenProducten: {
        number: 2,
        size: 10,
        totalElements: 11,
        numberOfElements: 1,
        totalPages: 2,
        content: [product("PRODUCT OP PAGINA 2")],
        __typename: "OpenProductProductPage",
      },
    },
  },
};

describe("ProductFetchList", () => {
  it("fetches the next page and renders its content when a page button is clicked", async () => {
    render(
      <TestProvider
        mocks={[firstPage, secondPage]}
        routes={[
          {
            path: paths.themeList(),
            children: [
              {
                index: true,
                element: (
                  <ProductFetchList
                    slug="belastingzaken"
                    productTypeSlug="aanslagen"
                    productTypeCodes={["CODE"]}
                    titleTranslationId="productsList.title"
                    headerTranslationIds={["productsList.title"]}
                    dataMapping={["naam"]}
                    productLength={10}
                    pagination={true}
                  />
                ),
              },
            ],
          },
        ]}
        initialIndex={0}
        initialEntries={[paths.themeList("belastingzaken", "aanslagen")]}
      />,
    );

    await waitFor(() => {
      expect(screen.getByText("PRODUCT OP PAGINA 1")).toBeVisible();
    });

    fireEvent.click(screen.getByRole("button", { name: "Page 2" }));

    await waitFor(() => {
      expect(screen.getByText("PRODUCT OP PAGINA 2")).toBeVisible();
    });
  });
});
