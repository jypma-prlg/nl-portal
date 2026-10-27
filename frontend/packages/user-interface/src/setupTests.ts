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

// allows you to do things like:
// expect(element).toHaveTextContent(/react/i)
// learn more: https://github.com/testing-library/jest-dom
import "@testing-library/jest-dom/vitest";
import { vi } from "vitest";

// Stub the backend features endpoint (GET /api/public/features) that AppProvider
// fetches on mount. Contact moments are enabled and the current-cases preview
// length is set; every other feature stays off so message/product/theme queries
// remain skipped in tests.
const featuresResponse = {
  theme: {
    logo: "",
    style: "",
  },
  properties: {
    custom: "{}",
    messageCountPollingInterval: 30000,
    myAddressChangeUrl: "",
    myAddressResearchMoreInfoUrl: "",
    myAddressResearchUrl: "",
    myBrpChangeUrl: "",
    myBrpConfidentiallyChangeUrl: "",
    myGenderChangeUrl: "",
    myNameChangeUrl: "",
    overviewCurrentCasesPreviewLength: 4,
    overviewMaintenanceAlertTextEn: "",
    overviewMaintenanceAlertTextNl: "",
    overviewMaintenanceAlertTitleEn: "",
    overviewMaintenanceAlertTitleNl: "",
    themeClass: "",
  },
  toggles: {
    custom: "{}",
    casesContactMomentsEnabled: true,
    casesPartialSearchEnabled: false,
    casesResultExplanationEnabled: false,
    messageCountEnabled: false,
    myInhabitantCountEnabled: false,
    openProductEnabled: false,
    overviewIntroEnabled: false,
    overviewMaintenanceAlertEnabled: false,
  },
};

vi.stubGlobal(
  "fetch",
  vi.fn((input: RequestInfo | URL) => {
    const url =
      typeof input === "string"
        ? input
        : ((input as Request).url ?? String(input));
    if (url.includes("/public/frontend")) {
      return Promise.resolve({
        ok: true,
        status: 200,
        json: () => Promise.resolve(structuredClone(featuresResponse)),
        text: () => Promise.resolve(""),
      } as Response);
    }
    return Promise.resolve({
      ok: false,
      status: 404,
      json: () => Promise.resolve({}),
      text: () => Promise.resolve(""),
    } as Response);
  }),
);

type StorageLike = Pick<
  Storage,
  "getItem" | "setItem" | "removeItem" | "clear" | "key"
> & { length: number };

const createStorageMock = (): StorageLike => {
  const store = new Map<string, string>();

  return {
    get length() {
      return store.size;
    },
    clear: () => {
      store.clear();
    },
    getItem: (key: string) => store.get(String(key)) ?? null,
    key: (index: number) => Array.from(store.keys())[index] ?? null,
    removeItem: (key: string) => {
      store.delete(String(key));
    },
    setItem: (key: string, value: string) => {
      store.set(String(key), String(value));
    },
  };
};

const ensureStorage = (name: "localStorage" | "sessionStorage") => {
  const target = globalThis as Record<string, unknown>;
  const value = target[name] as Partial<Storage> | undefined;

  if (
    !value ||
    typeof value.getItem !== "function" ||
    typeof value.setItem !== "function" ||
    typeof value.removeItem !== "function" ||
    typeof value.clear !== "function"
  ) {
    Object.defineProperty(globalThis, name, {
      configurable: true,
      value: createStorageMock(),
      writable: true,
    });
  }
};

ensureStorage("localStorage");
ensureStorage("sessionStorage");

// jsdom ships no matchMedia. inputmask (transitive via @formio/js) calls it at
// module load, so importing a tree that reaches it throws before any test runs.
if (typeof window.matchMedia === "undefined") {
  Object.defineProperty(window, "matchMedia", {
    configurable: true,
    writable: true,
    value: (query: string) => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: () => {},
      removeListener: () => {},
      addEventListener: () => {},
      removeEventListener: () => {},
      dispatchEvent: () => false,
    }),
  });
}

if (typeof window.ResizeObserver === "undefined") {
  class ResizeObserverMock {
    observe() {}
    unobserve() {}
    disconnect() {}
  }

  Object.defineProperty(globalThis, "ResizeObserver", {
    configurable: true,
    value: ResizeObserverMock,
    writable: true,
  });
}
