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
import { MockTasksPage } from "../mock/pages/TasksPage.mock";
import { describe, it, expect, vi, afterEach } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import TestProvider, { testPaths as paths } from "../../providers/TestProvider";
import { QUERY_GET_TAKEN_V2 } from "@nl-portal/nl-portal-api";
import TasksPage from "../../pages/TasksPage";
import { getTaken } from "../mock/data/taken.mock";

describe("TasksPage", () => {
  const takenAlgemeneInformatie = () => screen.getByText("OPEN TAAK 1");
  const taakAanvullendeInformatie = () => screen.getByText("OPEN TAAK 4");

  it("Shows an overview of all tasks", async () => {
    render(MockTasksPage());

    await waitFor(async () => {
      expect(takenAlgemeneInformatie()).toBeVisible();
    });

    expect(takenAlgemeneInformatie()).toBeVisible();
    expect(taakAanvullendeInformatie()).toBeVisible();
  });

  it("Allows me to continue to a task", async () => {
    render(MockTasksPage());

    await waitFor(async () => {
      expect(taakAanvullendeInformatie()).toBeVisible();
    });

    expect(screen.getByRole("link", { name: "OPEN TAAK 4" })).toHaveAttribute(
      "href",
      paths.case("66fecaa3-24b4-4739-a7c8-eb58f39e9aae"),
    );
  });

  describe("when a search refetch succeeds", () => {
    it("resets the page index back to the first page", async () => {
      const searchMock = {
        request: {
          query: QUERY_GET_TAKEN_V2,
          variables: { title: "zoekterm" },
        },
        result: {
          data: {
            getTakenV2: {
              content: [
                {
                  ...getTaken({}).result.data.getTakenV2.content[0],
                  id: "search-result-task",
                  titel: "GEVONDEN TAAK",
                },
              ],
              totalElements: 1,
              totalPages: 1,
              __typename: "TaakPageV2",
            },
          },
        },
      };

      render(
        <TestProvider
          mocks={[getTaken({}), searchMock]}
          routes={[
            {
              path: paths.tasks,
              children: [{ index: true, element: <TasksPage /> }],
            },
          ]}
          initialIndex={0}
          initialEntries={[paths.tasks]}
        />,
      );

      await waitFor(() => {
        expect(screen.getByText("OPEN TAAK 1")).toBeVisible();
      });

      fireEvent.change(screen.getByRole("textbox"), {
        target: { value: "zoekterm" },
      });
      fireEvent.click(screen.getByRole("button", { name: "Zoeken" }));

      await waitFor(() => {
        expect(screen.getByText("GEVONDEN TAAK")).toBeVisible();
      });
    });
  });

  describe("when a search refetch fails", () => {
    afterEach(() => {
      vi.restoreAllMocks();
    });

    it("should report the error through console.error instead of an unhandled rejection (kotlin:S9383/typescript:S9383 pattern)", async () => {
      // given: handleFormSubmit calls refetch(...) inside a non-async try/catch - before this
      // fix, the catch block can never see a refetch rejection (the promise it returns is never
      // awaited), so a failed search silently became an unhandled promise rejection instead of
      // the intended console.error.
      const consoleErrorSpy = vi
        .spyOn(console, "error")
        .mockImplementation(() => {});
      const searchErrorMock = {
        request: {
          query: QUERY_GET_TAKEN_V2,
          variables: { title: "faalzoekterm" },
        },
        error: new Error("Network error"),
      };

      render(
        <TestProvider
          mocks={[getTaken({}), searchErrorMock]}
          routes={[
            {
              path: paths.tasks,
              children: [{ index: true, element: <TasksPage /> }],
            },
          ]}
          initialIndex={0}
          initialEntries={[paths.tasks]}
        />,
      );

      await waitFor(() => {
        expect(screen.getByText("OPEN TAAK 1")).toBeVisible();
      });

      fireEvent.change(screen.getByRole("textbox"), {
        target: { value: "faalzoekterm" },
      });
      fireEvent.click(screen.getByRole("button", { name: "Zoeken" }));

      // then
      await waitFor(() => {
        expect(consoleErrorSpy).toHaveBeenCalledWith(
          "Error fetching tasks:",
          expect.anything(),
        );
      });
    });
  });

  describe("pagination", () => {
    it("fetches the next page and renders its content when a page button is clicked", async () => {
      const firstPage = getTaken({});
      const secondPage = {
        request: {
          query: QUERY_GET_TAKEN_V2,
          variables: { pageNumber: 2 },
        },
        result: {
          data: {
            getTakenV2: {
              content: [
                {
                  ...firstPage.result.data.getTakenV2.content[0],
                  id: "second-page-task",
                  titel: "OPEN TAAK OP PAGINA 2",
                },
              ],
              totalElements: 7,
              totalPages: 2,
              __typename: "TaakPageV2",
            },
          },
        },
      };
      const firstPageWithTotalPages = {
        ...firstPage,
        result: {
          data: {
            getTakenV2: {
              ...firstPage.result.data.getTakenV2,
              totalElements: 7,
              totalPages: 2,
            },
          },
        },
      };

      render(
        <TestProvider
          mocks={[firstPageWithTotalPages, secondPage]}
          routes={[
            {
              path: paths.tasks,
              children: [{ index: true, element: <TasksPage /> }],
            },
          ]}
          initialIndex={0}
          initialEntries={[paths.tasks]}
        />,
      );

      await waitFor(() => {
        expect(screen.getByText("OPEN TAAK 1")).toBeVisible();
      });

      fireEvent.click(screen.getByRole("button", { name: "Page 2" }));

      await waitFor(() => {
        expect(screen.getByText("OPEN TAAK OP PAGINA 2")).toBeVisible();
      });
    });
  });
});
