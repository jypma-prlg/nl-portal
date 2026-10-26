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
import React, { useContext, useEffect } from "react";
import {
  GetZaakDocument,
  GetTakenV2Document,
  GetUserKlantContactenDocument,
  ApiContext,
  TaakV2,
  ZaakStatus,
  OnderwerpObjectIndentificatorType,
} from "@nl-portal/nl-portal-api";
import { useQuery, useLazyQuery, skipToken } from "@apollo/client/react";
import {
  LocaleContext,
  useDateFormatter,
} from "@nl-portal/nl-portal-localization";
import { Paragraph } from "@utrecht/component-library-react";
import { FormattedMessage, useIntl } from "react-intl";
import { useParams } from "react-router";
import "@utrecht/component-library-css";
import { ContactTimeline } from "@gemeente-denhaag/contact-timeline";
import DocumentsList from "../components/DocumentsList";
import StatusHistory from "../components/StatusHistory";
import BackLink from "../components/BackLink";
import PageGrid from "../components/PageGrid";
import PageHeader from "../components/PageHeader";
import TasksList from "../components/TasksList";
import SectionHeader from "../components/SectionHeader";
import DescriptionList from "../components/DescriptionList";
import { ExtraCaseDetails, Details } from "../components/ExtraCaseDetails";
import NotificationContext from "../contexts/NotificationContext";
import { stringToSlug } from "../utils/string-to-slug";
import { caseResults } from "../constants/case-results";
import Pre from "../components/Pre";
import AppContext from "../contexts/AppContext";
import usePaymentRegistration, {
  PaymentStatus,
} from "../hooks/usePaymentRegistration";

const CaseDetailsPage = () => {
  const intl = useIntl();
  const { id } = useParams();
  const { currentLocale } = useContext(LocaleContext);
  const { features } = useContext(AppContext);
  const { restUri } = useContext(ApiContext);
  const {
    data: caseData,
    loading: caseLoading,
    error: caseError,
  } = useQuery(
    GetZaakDocument,
    id
      ? {
          variables: { id },
        }
      : skipToken,
  );
  const [getMomenten, { data: momentsData, loading: momentsLoading }] =
    useLazyQuery(GetUserKlantContactenDocument);

  const { data: tasksResult, loading: taskLoading } = useQuery(
    GetTakenV2Document,
    {
      variables: { zaakId: id },
    },
  );
  const { formatDate } = useDateFormatter();
  const { paymentStatus, orderId } = usePaymentRegistration();
  const loading = caseLoading || taskLoading || momentsLoading;

  // Remove task with the orderId to prevent race condition with the payment handling in the backend
  const tasks = (
    paymentStatus === PaymentStatus.SUCCESS && orderId
      ? tasksResult?.getTakenV2.content.filter((item) => item.id !== orderId)
      : tasksResult?.getTakenV2.content
  ) as TaakV2[] | undefined;

  const { dispatch } = useContext(NotificationContext);

  useEffect(() => {
    if (!caseData?.getZaak?.resultaat?.resultaattype?.omschrijvingGeneriek)
      return;

    const slug = stringToSlug(
      caseData?.getZaak.resultaat?.resultaattype.omschrijvingGeneriek,
    );
    const variant = caseResults[slug];

    if (!variant) return;

    dispatch({
      type: "CREATE",
      id: "caseResult",
      notification: {
        variant,
        title: <FormattedMessage id={`caseDetails.resultAlert.${slug}`} />,
        text: "",
        closable: false,
      },
    });
  }, [caseData]);

  useEffect(() => {
    if (paymentStatus === PaymentStatus.SUCCESS) {
      dispatch({
        type: "CREATE",
        id: "casePaymentSuccess",
        notification: {
          variant: "success",
          title: <FormattedMessage id="task.paymentSuccessTitle" />,
          text: <FormattedMessage id="task.paymentSuccessText" />,
        },
      });
    }
    if (paymentStatus === PaymentStatus.FAILURE) {
      dispatch({
        type: "CREATE",
        id: "casePaymentFailure",
        notification: {
          variant: "error",
          title: <FormattedMessage id="task.paymentFailureTitle" />,
          text: <FormattedMessage id="task.paymentFailureText" />,
        },
      });
    }
  }, [paymentStatus]);

  const details = React.useMemo(() => {
    if (!caseData?.getZaak) return [];

    const array = [
      {
        title: intl.formatMessage({ id: "caseDetails.creationDate" }),
        detail: formatDate({ date: caseData?.getZaak.startdatum }),
      },
      {
        title: intl.formatMessage({ id: "caseDetails.caseNumber" }),
        detail: (
          <span translate="no">{caseData?.getZaak.identificatie || ""}</span>
        ),
      },
    ];

    if (caseData?.getZaak.omschrijving)
      array.push({
        title: intl.formatMessage({ id: "caseDetails.description" }),
        detail: caseData?.getZaak.omschrijving || "",
      });

    if (caseData?.getZaak.resultaat?.resultaattype.omschrijvingGeneriek) {
      array.push({
        title: intl.formatMessage({ id: "caseDetails.resultaat" }),
        detail:
          caseData?.getZaak.resultaat?.resultaattype.omschrijvingGeneriek || "",
      });
    }

    if (
      features?.toggles.casesResultExplanationEnabled &&
      caseData?.getZaak.resultaat?.toelichting
    ) {
      array.push({
        title: intl.formatMessage({ id: "caseDetails.resultaatToelichting" }),
        detail: <Pre>{caseData?.getZaak.resultaat.toelichting}</Pre>,
      });
    }

    return array;
  }, [caseData?.getZaak, intl, formatDate, features]);

  const contactItems = React.useMemo(() => {
    if (!momentsData) return [];

    return momentsData.getUserKlantContacten.map((contact, index) => ({
      id: index,
      title: contact.onderwerp,
      description: contact.inhoud && <Pre>{contact.inhoud}</Pre>,
      channel: contact.kanaal,
      isoDate: contact.plaatsgevondenOp,
    }));
  }, [momentsData]);

  const contactLabels = {
    yesterday: intl.formatMessage({
      id: "caseDetails.contacttimeline.yesterday",
    }),
    today: intl.formatMessage({ id: "caseDetails.contacttimeline.today" }),
  };

  React.useEffect(() => {
    if (!caseData || !features?.toggles.casesContactMomentsEnabled) return;
    getMomenten({
      variables: {
        identificatorType: OnderwerpObjectIndentificatorType.Zaak,
        identificatorId: caseData.getZaak.uuid,
      },
    }).catch((error) => {
      console.error("Error fetching contact moments:", error);
    });
  }, [caseData, features]);

  if (caseError) {
    return (
      <div>
        <Paragraph>
          <FormattedMessage id="caseDetails.fetchError" />
        </Paragraph>
      </div>
    );
  }

  const zaakDetails = caseData?.getZaak.zaakdetails.data as
    Details[] | undefined;

  return (
    <PageGrid>
      <div>
        <BackLink />
        <PageHeader
          loading={loading}
          title={
            !loading &&
            intl.formatMessage({
              id: `case.${caseData?.getZaak.zaaktype.identificatie}.title`,
              defaultMessage: caseData?.getZaak.zaaktype.omschrijving || "",
            })
          }
        />
      </div>
      <TasksList
        loading={loading}
        showEmpty={false}
        titleTranslationId={null}
        tasks={tasks}
      />
      <section>
        <SectionHeader
          title={intl.formatMessage({ id: "caseDetails.statusHeader" })}
        />
        <StatusHistory
          loading={loading}
          caseId={caseData?.getZaak.zaaktype.identificatie}
          statusHistory={
            caseData?.getZaak.statusGeschiedenis as ZaakStatus[] | undefined
          }
          statuses={caseData?.getZaak.statussen}
          status={caseData?.getZaak.status as ZaakStatus | undefined}
        />
      </section>
      {details.length > 0 && (
        <DescriptionList
          titleTranslationId="caseDetails.detailsHeader"
          items={details}
        />
      )}
      {zaakDetails && <ExtraCaseDetails data={zaakDetails} />}
      <DocumentsList
        loading={loading}
        error={Boolean(caseError)}
        documents={caseData?.getZaak.documenten}
        getDownloadLink={(doc) =>
          `${restUri}/zakenapi/zaakdocument/${doc.identificatie}/content`
        }
      />
      {features?.toggles.casesContactMomentsEnabled &&
        contactItems.length > 0 && (
          <section>
            <SectionHeader
              title={intl.formatMessage({ id: "caseDetails.contactHeader" })}
            />
            <ContactTimeline
              items={contactItems}
              collapsible={contactItems.some((item) =>
                Boolean(item.description),
              )}
              labels={contactLabels}
              locale={currentLocale}
            />
          </section>
        )}
      <TasksList
        loading={loading}
        showEmpty={false}
        titleTranslationId={null}
        tasks={tasks}
      />
    </PageGrid>
  );
};

export default CaseDetailsPage;
