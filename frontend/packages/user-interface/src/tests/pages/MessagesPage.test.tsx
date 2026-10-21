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
import { GetBerichtenDocument } from "@nl-portal/nl-portal-api";
import MessagesPage from "../../pages/MessagesPage";

const message = (onderwerp: string) => ({
  id: `${onderwerp}-id`,
  einddatumHandelingstermijn: null,
  publicatiedatum: "2024-06-15T21:59:59.999Z",
  geopend: true,
  onderwerp,
  __typename: "Bericht",
});

const firstPage = {
  request: {
    query: GetBerichtenDocument,
    variables: { pageSize: 10 },
  },
  result: {
    data: {
      getBerichten: {
        content: [message("BERICHT OP PAGINA 1")],
        totalElements: 11,
        totalPages: 2,
        __typename: "BerichtPage",
      },
    },
  },
};

const secondPage = {
  request: {
    query: GetBerichtenDocument,
    variables: { pageSize: 10, pageNumber: 2 },
  },
  result: {
    data: {
      getBerichten: {
        content: [message("BERICHT OP PAGINA 2")],
        totalElements: 11,
        totalPages: 2,
        __typename: "BerichtPage",
      },
    },
  },
};

const searchResult = {
  request: {
    query: GetBerichtenDocument,
    variables: { pageSize: 10, onderwerp: "zoekterm" },
  },
  result: {
    data: {
      getBerichten: {
        content: [message("GEVONDEN BERICHT")],
        totalElements: 1,
        totalPages: 1,
        __typename: "BerichtPage",
      },
    },
  },
};

describe("MessagesPage", () => {
  it("is truthy", () => {
    expect(MessagesPage).toBeTruthy();
  });

  it("fetches the next page and renders its content when a page button is clicked", async () => {
    render(
      <TestProvider
        mocks={[firstPage, secondPage]}
        routes={[
          {
            path: paths.messages,
            children: [{ index: true, element: <MessagesPage /> }],
          },
        ]}
        initialIndex={0}
        initialEntries={[paths.messages]}
      />,
    );

    await waitFor(() => {
      expect(screen.getByText("BERICHT OP PAGINA 1")).toBeVisible();
    });

    fireEvent.click(screen.getByRole("button", { name: "Page 2" }));

    await waitFor(() => {
      expect(screen.getByText("BERICHT OP PAGINA 2")).toBeVisible();
    });
  });

  it("fetches search results when the search form is submitted", async () => {
    render(
      <TestProvider
        mocks={[firstPage, searchResult]}
        routes={[
          {
            path: paths.messages,
            children: [{ index: true, element: <MessagesPage /> }],
          },
        ]}
        initialIndex={0}
        initialEntries={[paths.messages]}
      />,
    );

    await waitFor(() => {
      expect(screen.getByText("BERICHT OP PAGINA 1")).toBeVisible();
    });

    fireEvent.change(screen.getByRole("textbox"), {
      target: { value: "zoekterm" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Zoeken" }));

    await waitFor(() => {
      expect(screen.getByText("GEVONDEN BERICHT")).toBeVisible();
    });
  });
});
