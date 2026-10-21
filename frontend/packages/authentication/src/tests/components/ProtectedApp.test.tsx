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
import { describe, it, expect, vi, afterEach, beforeEach } from "vitest";
import { act } from "react";
import { createRoot, Root } from "react-dom/client";
import { IntlProvider } from "react-intl";
import ProtectedApp from "../../components/ProtectedApp";

// jsdom doesn't implement the <dialog> element's imperative API used by @gemeente-denhaag/modal.
(
  globalThis as { IS_REACT_ACT_ENVIRONMENT?: boolean }
).IS_REACT_ACT_ENVIRONMENT = true;
HTMLDialogElement.prototype.showModal = function () {
  this.setAttribute("open", "");
};
HTMLDialogElement.prototype.close = function () {
  this.removeAttribute("open");
};

const signinRedirectMock = vi.fn();
const signoutRedirectMock = vi.fn();
let isAuthenticated = false;

vi.mock("react-oidc-context", () => ({
  useAuth: () => ({
    isLoading: false,
    get isAuthenticated() {
      return isAuthenticated;
    },
    signinRedirect: signinRedirectMock,
    signoutRedirect: signoutRedirectMock,
    events: { addUserLoaded: () => () => {} },
  }),
}));

describe("ProtectedApp", () => {
  let container: HTMLDivElement;
  let root: Root;

  beforeEach(() => {
    isAuthenticated = false;
    container = document.createElement("div");
    document.body.appendChild(container);
    root = createRoot(container);
  });

  afterEach(() => {
    act(() => root.unmount());
    container.remove();
  });

  it("fire-and-forgets the signin redirect when the user isn't authenticated", () => {
    act(() => {
      root.render(
        <IntlProvider locale="en" messages={{}} onError={() => {}}>
          <ProtectedApp>child</ProtectedApp>
        </IntlProvider>,
      );
    });

    expect(signinRedirectMock).toHaveBeenCalled();
  });

  it("fire-and-forgets the signout redirect when the idle logout warning is confirmed", () => {
    isAuthenticated = true;
    vi.useFakeTimers();

    try {
      act(() => {
        root.render(
          <IntlProvider locale="en" messages={{}} onError={() => {}}>
            <ProtectedApp idleTimeoutMinutes={5}>child</ProtectedApp>
          </IntlProvider>,
        );
      });

      // the "show warning" IdleTimer fires after (idleTimeoutMinutes - 5) minutes, i.e. immediately.
      act(() => {
        vi.advanceTimersByTime(1);
      });

      const logoutButton = Array.from(
        container.querySelectorAll("button"),
      ).find((button) => button.textContent === "auth.inactive.logout");
      expect(logoutButton).toBeDefined();

      act(() => {
        logoutButton?.click();
      });

      expect(signoutRedirectMock).toHaveBeenCalled();
    } finally {
      vi.useRealTimers();
    }
  });
});
