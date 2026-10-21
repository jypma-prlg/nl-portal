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
import { describe, it, vi, afterEach } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import TestProvider, { testPaths as paths } from "../../providers/TestProvider";
import { GetOpenProductActiePrefillDocument } from "@nl-portal/nl-portal-api";
import ProductDecisionsList from "../../components/ProductDecisionList";

const productId = "3f8b4c07-91c5-4f14-9cc4-2d6a6b8a3bda";

describe("ProductDecisionsList", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("should report a prefill fetch failure through console.error instead of an unhandled rejection (typescript:S9383 pattern)", async () => {
    const consoleErrorSpy = vi
      .spyOn(console, "error")
      .mockImplementation(() => {});

    const prefillErrorMock = {
      request: {
        query: GetOpenProductActiePrefillDocument,
        variables: { productId, naam: "aanvraag" },
      },
      error: new Error("Network error"),
    };

    render(
      <TestProvider
        mocks={[prefillErrorMock]}
        routes={[
          {
            path: paths.products,
            children: [
              {
                index: true,
                element: (
                  <ProductDecisionsList
                    loading={false}
                    productId={productId}
                    decisions={[
                      {
                        type: { value: "PREFILL" },
                        action: { value: "aanvraag" },
                        text: { value: "Start aanvraag" },
                      },
                    ]}
                  />
                ),
              },
            ],
          },
        ]}
        initialIndex={0}
        initialEntries={[paths.products]}
      />,
    );

    await waitFor(() => {
      expect(screen.getByText("Start aanvraag")).toBeVisible();
    });

    fireEvent.click(screen.getByText("Start aanvraag"));

    await waitFor(() => {
      expect(consoleErrorSpy).toHaveBeenCalledWith(
        "Error fetching decision prefill:",
        expect.anything(),
      );
    });
  });
});
