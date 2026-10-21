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
import { PropsWithChildren, useContext, useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";
import IdleTimer from "./IdleTimer";
import { Paragraph } from "@gemeente-denhaag/typography";
import { FormattedMessage, useIntl } from "react-intl";
import OidcContext from "../context/OidcContext";
import { Modal } from "@gemeente-denhaag/modal";

export type ProtectedAppProps = {
  autoIdleSessionLogout?: boolean;
  idleTimeoutMinutes?: number;
  showAutomaticLogoutWarning?: boolean;
};

export const ProtectedApp = ({
  autoIdleSessionLogout = true,
  idleTimeoutMinutes = 15,
  showAutomaticLogoutWarning = true,
  children,
}: PropsWithChildren<ProtectedAppProps>) => {
  const auth = useAuth();
  const { oidcToken, setOidcToken } = useContext(OidcContext);
  const [showLogoutWarning, setShowLogoutWarning] = useState(false);
  const intl = useIntl();

  useEffect(() => {
    // Redirect to oidc provider when not loading and user isn't authenticated or if there is no oidc token in the state (after hard refresh for example)
    if (!auth.isLoading && (!auth.isAuthenticated || !oidcToken)) {
      // fire-and-forget: this triggers a full-page redirect to the OIDC provider.
      void auth.signinRedirect();
    }
  }, [oidcToken, auth.isLoading, auth.isAuthenticated]);

  useEffect(() => {
    return auth.events.addUserLoaded((user) => setOidcToken(user.access_token));
  }, [auth.events]);

  // Placeholder for a spinner or something
  if (auth.isLoading || !auth.isAuthenticated) {
    return null;
  }

  // If authenticated, render the children
  return (
    <>
      {autoIdleSessionLogout && (
        <>
          <IdleTimer
            idleTimeoutMinutes={idleTimeoutMinutes}
            onTimeOut={auth.signoutRedirect}
          />
          {showAutomaticLogoutWarning && (
            <IdleTimer
              idleTimeoutMinutes={idleTimeoutMinutes - 5}
              onTimeOut={() => setShowLogoutWarning(true)}
            />
          )}
          {showLogoutWarning && (
            <Modal
              open={showLogoutWarning}
              title={intl.formatMessage({ id: "auth.inactive.title" })}
              closeLabel={intl.formatMessage({ id: "auth.inactive.close" })}
              onToggle={(open) => setShowLogoutWarning(open)}
              actions={(toggle) => [
                {
                  children: intl.formatMessage({
                    id: "auth.inactive.logout",
                  }),
                  variant: "secondary-action",
                  onClick: () => {
                    // fire-and-forget: this triggers a full-page redirect to the OIDC provider.
                    void auth.signoutRedirect();
                  },
                },
                {
                  children: intl.formatMessage({
                    id: "auth.inactive.stayLoggedIn",
                  }),
                  variant: "primary-action",
                  onClick: () => {
                    setShowLogoutWarning(false);
                    toggle();
                  },
                },
              ]}
            >
              <Paragraph>
                <FormattedMessage id="auth.inactive.text" />
              </Paragraph>
            </Modal>
          )}
        </>
      )}
      {children}
    </>
  );
};

export default ProtectedApp;
