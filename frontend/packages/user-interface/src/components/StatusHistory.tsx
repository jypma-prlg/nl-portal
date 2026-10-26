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
import { FC } from "react";
import {
  StatusType,
  ZaakStatus,
  ZaakSubStatus,
} from "@nl-portal/nl-portal-api";
import { Paragraph } from "@utrecht/component-library-react";
import { Status } from "@gemeente-denhaag/process-steps";
import Skeleton from "react-loading-skeleton";
import { useIntl } from "react-intl";
import { stringToId } from "../utils/string-to-id";
import styles from "./StatusHistory.module.scss";
import {
  statusHistoryCirleSize,
  statusHistoryTextWidth,
} from "../constants/skeleton";
import { useDateFormatter } from "@nl-portal/nl-portal-localization";

interface StatusHistoryProps {
  caseId?: string;
  statusHistory?: Array<ZaakStatus>;
  statuses?: Array<StatusType>;
  status?: ZaakStatus | null;
  loading: boolean;
}

const StatusHistory: FC<StatusHistoryProps> = ({
  statusHistory,
  statuses = [],
  status,
  loading,
  caseId,
}) => {
  const intl = useIntl();
  const { formatDate } = useDateFormatter();

  const getSkeletonStep = (key: number) => (
    <div
      key={key}
      className={styles["skeleton-step"]}
      aria-busy
      aria-disabled
      aria-label={intl.formatMessage({ id: "element.loading" })}
    >
      <div className={styles["skeleton-step__circle"]}>
        <Skeleton
          circle
          height={statusHistoryCirleSize}
          width={statusHistoryCirleSize}
        />
      </div>
      <Paragraph>
        <Skeleton width={statusHistoryTextWidth} />
      </Paragraph>
    </div>
  );

  const getStepStatus = (omschrijving?: string | null) => {
    if (!omschrijving) return "not-checked";
    if (status?.statustype?.omschrijving === omschrijving) return "current";
    if (statusHistory?.find((h) => h.statustype.omschrijving === omschrijving))
      return "checked";
    return "not-checked";
  };

  return (
    <div className={styles["status-history-container"]}>
      {!loading && statuses ? (
        <Status
          collapsible
          expandedSteps={[
            `step-${statuses.findIndex((s) => s.omschrijving === status?.statustype?.omschrijving) + 1}`,
          ]}
          steps={statuses?.map(({ omschrijving }, index) => {
            const currentStatus = [status, statusHistory]
              .flat()
              .find((s) => s?.statustype.omschrijving === omschrijving);

            return {
              id: `step-${index + 1}`,
              title: intl.formatMessage({
                id: `case.${caseId}.status.${stringToId(`${omschrijving}`)}`,
                defaultMessage: omschrijving || "",
              }),
              status: getStepStatus(omschrijving),
              marker: index + 1,
              steps: currentStatus?.substatussen?.map(
                (sub: ZaakSubStatus, subIndex: number) => ({
                  id: `substatus-${sub.uuid}`,
                  title: sub.omschrijving,
                  date: formatDate({
                    date: sub.tijdstip,
                  }),
                  status:
                    getStepStatus(omschrijving) === "checked"
                      ? "checked"
                      : getStepStatus(omschrijving) === "current" &&
                          subIndex === currentStatus?.substatussen.length - 1
                        ? "current"
                        : "checked",
                }),
              ),
            };
          })}
        />
      ) : (
        <>
          {getSkeletonStep(0)}
          {getSkeletonStep(1)}
        </>
      )}
    </div>
  );
};
export default StatusHistory;
