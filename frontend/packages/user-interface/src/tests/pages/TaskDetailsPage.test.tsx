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
import TestProvider, { testPaths as paths } from "../../providers/TestProvider";
import {
  GetPortaalFormulierByIdV2Document,
  GetFormDefinitionByTaskIdDocument,
} from "@nl-portal/nl-portal-api";
import TaskDetailsPage from "../../pages/TaskDetailsPage.tsx";

const taskId = "021118b9-bc59-11ee-b651-366634c97df6";

describe("TaskDetailsPage", () => {
  it("is truthy", () => {
    expect(TaskDetailsPage).toBeTruthy();
  });

  describe("when fetching the form definition fails", () => {
    afterEach(() => {
      vi.restoreAllMocks();
    });

    it("should report the error through console.error instead of an unhandled rejection (typescript:S9383 pattern)", async () => {
      const consoleErrorSpy = vi
        .spyOn(console, "error")
        .mockImplementation(() => {});

      const taskMock = {
        request: {
          query: GetPortaalFormulierByIdV2Document,
          variables: { id: taskId },
        },
        result: {
          data: {
            getTaakByIdV2: {
              id: taskId,
              portaalformulier: { data: {} },
              titel: "OPEN TAAK 1",
              status: "OPEN",
              verloopdatum: null,
              __typename: "TaakV2",
            },
          },
        },
      };
      const formDefinitionErrorMock = {
        request: {
          query: GetFormDefinitionByTaskIdDocument,
          variables: { taskId },
        },
        error: new Error("Network error"),
      };

      render(
        <TestProvider
          mocks={[taskMock, formDefinitionErrorMock]}
          routes={[{ path: paths.task(), element: <TaskDetailsPage /> }]}
          initialIndex={0}
          initialEntries={[paths.task(taskId)]}
        />,
      );

      await waitFor(() => {
        expect(consoleErrorSpy).toHaveBeenCalledWith(
          "Error fetching form definition:",
          expect.anything(),
        );
      });
    });
  });
});
