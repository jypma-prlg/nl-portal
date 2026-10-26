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
import { Alert } from "@gemeente-denhaag/alert";
import { FormattedMessage, useIntl } from "react-intl";
import CasesList from "../components/CasesList";
import PageHeader from "../components/PageHeader";
import {
  TaakV2,
  Zaak,
  GetTakenV2Document,
  GetZakenDocument,
  GetOpenProductenDocument,
  OpenProductProduct,
} from "@nl-portal/nl-portal-api";
import { useQuery } from "@apollo/client/react";
import TasksList from "../components/TasksList";
import PageGrid from "../components/PageGrid";
import { Paragraph } from "@utrecht/component-library-react";
import { ReactNode, useContext } from "react";
import UserContext from "../contexts/UserContext";
import { RouterOutletContext } from "../interfaces/router-outlet-context";
import { useNavigate, useOutletContext } from "react-router";
import AppContext from "../contexts/AppContext";
import ProductsList from "../components/ProductsList";
import { DEFAULT_LOCALES } from "@nl-portal/nl-portal-localization";
import QuickLinks from "../components/QuickLinks";

interface OverviewPageProps {
  showNoEmailAlert?: boolean;
  fetchTasksLength?: number;
  children?: ReactNode;
}

const OverviewPage = ({
  showNoEmailAlert = false,
  fetchTasksLength = 5,
  children,
}: OverviewPageProps) => {
  const intl = useIntl();
  const { features } = useContext(AppContext);
  const fetchCasesLength =
    features?.properties.overviewCurrentCasesPreviewLength ?? 4;
  const fetchProductsLength = features?.toggles.openProductEnabled ? 5 : 0;
  const { username, usernameVolmacht, isVolmacht, contact } =
    useContext(UserContext);
  const {
    data: tasksData,
    loading: tasksLoading,
    error: tasksError,
  } = useQuery(GetTakenV2Document, {
    variables: { pageSize: fetchTasksLength },
    skip: !fetchTasksLength,
  });
  const {
    data: casesData,
    loading: casesLoading,
    error: casesError,
  } = useQuery(GetZakenDocument, {
    variables: { pageSize: fetchCasesLength },
    skip: !fetchCasesLength,
  });
  const {
    data: productsData,
    loading: productsLoading,
    error: productsError,
  } = useQuery(GetOpenProductenDocument, {
    variables: { pageSize: fetchProductsLength },
    skip: !fetchProductsLength,
  });
  const { paths } = useOutletContext<RouterOutletContext>();
  const navigate = useNavigate();

  const loading = tasksLoading || casesLoading || productsLoading;
  const tasks = tasksData?.getTakenV2.content as TaakV2[] | undefined;
  const cases = casesData?.getZaken.content as Zaak[] | undefined;
  const products = productsData?.getOpenProducten?.content as
    OpenProductProduct[] | undefined;
  const emailadres = contact?.getUserDigitaleAdressen?.find(
    (a) => a.type === "EMAIL",
  );

  return (
    <PageGrid>
      {features?.toggles.overviewMaintenanceAlertEnabled && (
        <Alert
          variant="warning"
          title={
            intl.locale === DEFAULT_LOCALES.ENGLISH
              ? features.properties.overviewMaintenanceAlertTitleEn
              : features.properties.overviewMaintenanceAlertTitleNl
          }
          text={
            <Paragraph>
              {intl.locale === DEFAULT_LOCALES.ENGLISH
                ? features.properties.overviewMaintenanceAlertTextEn
                : features.properties.overviewMaintenanceAlertTextNl}
            </Paragraph>
          }
        />
      )}
      {showNoEmailAlert && contact && !emailadres?.waarde && (
        <Alert
          title={<FormattedMessage id="overviewpage.noEmail.title" />}
          text={
            <Paragraph>
              <FormattedMessage id="overviewpage.noEmail.text" />
            </Paragraph>
          }
          variant="warning"
          action={{
            buttonText: intl.formatMessage({
              id: "overviewpage.noEmail.text.button",
            }),
            onClick: () => navigate(paths.changeContactInfo("email")),
          }}
        />
      )}
      {contact &&
        emailadres?.waarde &&
        emailadres?.verificatieNeeded &&
        !emailadres?.verificatieDatum && (
          <Alert
            title={<FormattedMessage id="overviewpage.noEmailVerified.title" />}
            text={
              <Paragraph>
                <FormattedMessage id="overviewpage.noEmailVerified.text" />
              </Paragraph>
            }
            variant="warning"
            action={{
              buttonText: intl.formatMessage({
                id: "overviewpage.noEmailVerified.text.button",
              }),
              onClick: () => navigate(paths.changeContactInfo("email")),
            }}
          />
        )}
      {features?.toggles.overviewIntroEnabled && (
        <PageHeader
          title={
            <>
              <FormattedMessage id="overviewpage.title" />{" "}
              <span translate="no">{username}</span>
            </>
          }
          subTitle={
            isVolmacht && (
              <>
                <FormattedMessage id="overview.subTitle" />{" "}
                <span translate="no">{usernameVolmacht}</span>
              </>
            )
          }
        >
          <Paragraph>
            <FormattedMessage id="overviewpage.paragraph" />
          </Paragraph>
        </PageHeader>
      )}
      {children}
      <QuickLinks />
      {Boolean(fetchTasksLength) && (
        <TasksList
          loading={loading}
          error={Boolean(tasksError)}
          tasks={tasks}
          openInContext={true}
          totalAmount={
            tasksData?.getTakenV2.totalElements &&
            tasksData?.getTakenV2.totalElements > fetchTasksLength
              ? tasksData?.getTakenV2.totalElements
              : undefined
          }
        />
      )}
      {Boolean(fetchCasesLength) && (
        <CasesList
          loading={loading}
          error={Boolean(casesError)}
          titleTranslationId="overviewpage.casesTitle"
          listView={false}
          cases={cases}
          totalAmount={
            casesData?.getZaken.totalElements &&
            casesData?.getZaken.totalElements > fetchCasesLength
              ? casesData?.getZaken.totalElements
              : undefined
          }
        />
      )}
      {Boolean(fetchProductsLength) && (
        <ProductsList
          loading={loading}
          error={Boolean(productsError)}
          products={products}
          totalAmount={
            productsData?.getOpenProducten.totalElements &&
            productsData?.getOpenProducten.totalElements > fetchProductsLength
              ? productsData?.getOpenProducten.totalElements
              : undefined
          }
        />
      )}
    </PageGrid>
  );
};

export default OverviewPage;
