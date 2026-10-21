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
import { describe, it, expect, vi, afterEach } from "vitest";
import { renderHook, waitFor, act } from "@testing-library/react";
import { MockedProvider } from "@apollo/client/testing/react";
import { LocaleContext } from "@nl-portal/nl-portal-localization";
import { DoDirectPaymentDocument } from "@nl-portal/nl-portal-api";
import usePayment from "../../hooks/usePayment";

describe("usePayment", () => {
  const originalLocation = window.location;

  afterEach(() => {
    vi.restoreAllMocks();
    Object.defineProperty(window, "location", {
      value: originalLocation,
      writable: true,
      configurable: true,
    });
  });

  it("starts the direct payment mutation and redirects on completion", async () => {
    const mockLocation = { href: "https://example.com/oorspronkelijk" };
    Object.defineProperty(window, "location", {
      value: mockLocation,
      writable: true,
      configurable: true,
    });
    const paymentMock = {
      request: {
        query: DoDirectPaymentDocument,
        variables: {
          amount: 10,
          identifier: "ideal",
          orderId: "order-1",
          reference: "reference-1",
          langId: "nl_NL",
          returnUrl: "https://example.com/terugkeer",
        },
      },
      result: {
        data: {
          doDirectPayment: {
            redirectUrl: "https://payment-provider.example/redirect",
            __typename: "DoDirectPaymentResponse",
          },
        },
      },
    };

    const { result } = renderHook(() => usePayment(), {
      wrapper: ({ children }) => (
        <MockedProvider mocks={[paymentMock]}>
          <LocaleContext.Provider
            value={{
              currentLocale: "nl-NL",
              supportedLocales: ["nl-NL"],
              hrefLang: "nl",
              setCurrentLocale: () => {},
            }}
          >
            {children}
          </LocaleContext.Provider>
        </MockedProvider>
      ),
    });

    act(() => {
      result.current.startPayment({
        amount: 10,
        identifier: "ideal",
        orderId: "order-1",
        reference: "reference-1",
        returnUrl: "https://example.com/terugkeer",
      });
    });

    expect(result.current.loading).toBe(true);

    await waitFor(() => {
      expect(mockLocation.href).toBe(
        "https://payment-provider.example/redirect",
      );
    });
  });
});
