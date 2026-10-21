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
import { render, waitFor } from "@testing-library/react";
import { MockedProvider } from "@apollo/client/testing/react";
import { OidcContext } from "@nl-portal/nl-portal-authentication";
import {
  QUERY_GET_PERSOON_V2,
  GetBedrijfDocument,
  QUERY_GET_GEMACHTIGDE_V2,
  QUERY_GET_USER_DIGITALE_ADRESSEN,
} from "@nl-portal/nl-portal-api";
import { UserProvider } from "../../contexts/UserContext";

const contactMock = {
  request: { query: QUERY_GET_USER_DIGITALE_ADRESSEN, variables: {} },
  result: { data: { getUserDigitaleAdressen: [] } },
};

const renderUserProvider = (
  mocks: object[],
  oidc: {
    decodedToken: { middel: string };
    authenticationMethods: { company?: string[]; proxy?: string[] };
  },
) =>
  render(
    <MockedProvider mocks={mocks}>
      <OidcContext.Provider
        value={{
          oidcToken: "token",
          setOidcToken: () => {},
          decodedToken: oidc.decodedToken,
          authenticationMethods: oidc.authenticationMethods,
        }}
      >
        <UserProvider>child</UserProvider>
      </OidcContext.Provider>
    </MockedProvider>,
  );

describe("UserProvider", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("reports a persoon and gemachtigde fetch failure through console.error instead of an unhandled rejection (typescript:S9383 pattern)", async () => {
    const consoleErrorSpy = vi
      .spyOn(console, "error")
      .mockImplementation(() => {});

    renderUserProvider(
      [
        contactMock,
        {
          request: { query: QUERY_GET_PERSOON_V2, variables: {} },
          error: new Error("Network error"),
        },
        {
          request: { query: QUERY_GET_GEMACHTIGDE_V2, variables: {} },
          error: new Error("Network error"),
        },
      ],
      {
        decodedToken: { middel: "volmacht" },
        authenticationMethods: { company: ["bedrijf"], proxy: ["volmacht"] },
      },
    );

    await waitFor(() => {
      expect(consoleErrorSpy).toHaveBeenCalledWith(
        "Error fetching persoon:",
        expect.anything(),
      );
    });
    await waitFor(() => {
      expect(consoleErrorSpy).toHaveBeenCalledWith(
        "Error fetching gemachtigde:",
        expect.anything(),
      );
    });
  });

  it("reports a bedrijf fetch failure through console.error instead of an unhandled rejection (typescript:S9383 pattern)", async () => {
    const consoleErrorSpy = vi
      .spyOn(console, "error")
      .mockImplementation(() => {});

    renderUserProvider(
      [
        contactMock,
        {
          request: { query: GetBedrijfDocument, variables: {} },
          error: new Error("Network error"),
        },
      ],
      {
        decodedToken: { middel: "bedrijf" },
        authenticationMethods: { company: ["bedrijf"], proxy: ["volmacht"] },
      },
    );

    await waitFor(() => {
      expect(consoleErrorSpy).toHaveBeenCalledWith(
        "Error fetching bedrijf:",
        expect.anything(),
      );
    });
  });
});
