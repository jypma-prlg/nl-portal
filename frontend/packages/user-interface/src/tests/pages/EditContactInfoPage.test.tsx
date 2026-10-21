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
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import TestProvider, { testPaths as paths } from "../../providers/TestProvider";
import {
  CreateUserDigitaleAdresDocument,
  DigitaleAdresType,
} from "@nl-portal/nl-portal-api";
import { getPersoon } from "../mock/data/persoon.mock";
import { getUserDigitaleAdressen } from "../mock/data/userDigitaleAdressen.mock";
import EditContactInfoPage from "../../pages/EditContactInfoPage";

const noContactMock = {
  request: getUserDigitaleAdressen.request,
  result: { data: { getUserDigitaleAdressen: [] } },
};

const routes = [
  { path: paths.changeContactInfo(), element: <EditContactInfoPage /> },
  { path: paths.account, element: <div>ACCOUNT PAGE</div> },
];

describe("EditContactInfoPage", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("navigates to the account page after a successful submit", async () => {
    const createMock = {
      request: {
        query: CreateUserDigitaleAdresDocument,
        variables: {
          digitaleAdresRequest: {
            waarde: "test@test.nl",
            type: DigitaleAdresType.Email,
            omschrijving: "email",
            verificatieCode: undefined,
          },
        },
      },
      result: {
        data: {
          createUserDigitaleAdres: {
            uuid: "a5f323d8-8c87-43c2-990b-eae03d721adf",
            waarde: "test@test.nl",
            type: "EMAIL",
            omschrijving: "email",
            referentie: "",
            verificatieDatum: null,
            verificatieNeeded: false,
            verificatieCodeVerified: null,
            __typename: "DigitaleAdresResponse",
          },
        },
      },
    };

    render(
      <TestProvider
        mocks={[getPersoon, noContactMock, createMock]}
        routes={routes}
        initialIndex={0}
        initialEntries={[paths.changeContactInfo("email")]}
      />,
    );

    const input = await screen.findByLabelText("Emailadres");
    fireEvent.change(input, { target: { value: "test@test.nl" } });
    fireEvent.click(screen.getByRole("button", { name: "Opslaan" }));

    await waitFor(() => {
      expect(screen.getByText("ACCOUNT PAGE")).toBeVisible();
    });
  });

  it("reports a submit failure through console.error instead of an unhandled rejection (typescript:S9383 pattern)", async () => {
    const consoleErrorSpy = vi
      .spyOn(console, "error")
      .mockImplementation(() => {});

    const createErrorMock = {
      request: {
        query: CreateUserDigitaleAdresDocument,
        variables: {
          digitaleAdresRequest: {
            waarde: "test@test.nl",
            type: DigitaleAdresType.Email,
            omschrijving: "email",
            verificatieCode: undefined,
          },
        },
      },
      error: new Error("Network error"),
    };

    render(
      <TestProvider
        mocks={[getPersoon, noContactMock, createErrorMock]}
        routes={routes}
        initialIndex={0}
        initialEntries={[paths.changeContactInfo("email")]}
      />,
    );

    const input = await screen.findByLabelText("Emailadres");
    fireEvent.change(input, { target: { value: "test@test.nl" } });
    fireEvent.click(screen.getByRole("button", { name: "Opslaan" }));

    await waitFor(() => {
      expect(consoleErrorSpy).toHaveBeenCalledWith(
        "Error submitting contact info:",
        expect.anything(),
      );
    });
  });
});
