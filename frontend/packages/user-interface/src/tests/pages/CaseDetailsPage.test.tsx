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
import { describe, it, expect, beforeAll, vi, afterEach } from "vitest";
import {
  render,
  screen,
  waitFor,
  waitForElementToBeRemoved,
} from "@testing-library/react";
import {
  MockCaseDetailsPage,
  MockCaseDetailsPageWithFailingContactMoments,
  MockCaseDetailsPageWithoutContactMoments,
  MockCaseDetailsPageWithoutDocuments,
} from "../mock/pages/CaseDetailsPage.mock.tsx";

describe("CaseDetailsPage", () => {
  const skeleton = () => screen.getAllByLabelText("Aan het laden");

  beforeAll(() => {
    Object.defineProperty(window, "matchMedia", {
      writable: true,
      value: vitest.fn().mockImplementation((query) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: vitest.fn(), // Deprecated
        removeListener: vitest.fn(), // Deprecated
        addEventListener: vitest.fn(),
        removeEventListener: vitest.fn(),
        dispatchEvent: vitest.fn(),
      })),
    });
  });

  it("should render with all elements present", async () => {
    render(MockCaseDetailsPage());

    await waitFor(skeleton);
    await waitForElementToBeRemoved(skeleton);

    expect(screen.getByText("Certificaat WWJB")).toBeVisible();
    expect(screen.getByText("Bezwaarschrift")).toBeVisible();
    expect(screen.getByText("ZAAK-2023-0000007947")).toBeVisible();
    expect(screen.getByText("Betaalgeschiedenis 2")).toBeVisible();
    expect(screen.getByText("Kamillestraat 22")).toBeVisible();
    expect(screen.getByText("Dit is een sms")).toBeVisible();
    expect(screen.queryByText("Eerdere contactmomenten")).toBeVisible();
  });

  it("should render without any documents present and show message that no documents are present", async () => {
    render(MockCaseDetailsPageWithoutDocuments());

    await waitFor(skeleton);
    await waitForElementToBeRemoved(skeleton);

    expect(screen.queryByText("Certificaat WWJB")).toBeNull();
    expect(
      screen.queryByText("Er zijn geen documenten beschikbaar."),
    ).toBeVisible();
    expect(screen.queryByText("Eerdere contactmomenten")).toBeVisible();
  });

  it("should render without any contactmoments present and not show header contactmoments", async () => {
    render(MockCaseDetailsPageWithoutContactMoments());

    await waitFor(skeleton);
    await waitForElementToBeRemoved(skeleton);

    expect(screen.getByText("Documenten"));
    expect(screen.getByText("Certificaat WWJB")).toBeVisible();
    expect(screen.queryByText("Eerdere contactmomenten")).toBeNull();
  });

  describe("when fetching contact moments fails", () => {
    afterEach(() => {
      vi.restoreAllMocks();
    });

    it("should report the error through console.error instead of an unhandled rejection (typescript:S9383 pattern)", async () => {
      const consoleErrorSpy = vi
        .spyOn(console, "error")
        .mockImplementation(() => {});

      render(MockCaseDetailsPageWithFailingContactMoments());

      await waitFor(skeleton);
      await waitForElementToBeRemoved(skeleton);

      await waitFor(() => {
        expect(consoleErrorSpy).toHaveBeenCalledWith(
          "Error fetching contact moments:",
          expect.anything(),
        );
      });
    });
  });
});
