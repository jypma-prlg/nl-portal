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
import { FormattedMessage } from "react-intl";
import {
  TableList,
  ThemeHistoryPage,
} from "@nl-portal/nl-portal-user-interface";
import { useDateFormatter } from "@nl-portal/nl-portal-localization";
import { OpenProductProduct } from "@nl-portal/nl-portal-api";
import { StatusBadge } from "@utrecht/component-library-react";
import { useState } from "react";

const ParkerenHistory = () => {
  const { formatDate } = useDateFormatter();
  const [index, setIndex] = useState<number>(0);
  const pageSize = 4;

  return (
    <ThemeHistoryPage slug="parkeren">
      {({ loading, data }) => {
        const indexLimit = Math.floor(
          ((data?.getOpenProduct as OpenProductProduct | undefined)
            ?.verbruiksobject?.data?.periodes?.length ?? 0) / pageSize,
        );
        const product = data?.getOpenProduct as OpenProductProduct | undefined;

        return (
          <TableList
            loading={loading}
            titleTranslationId={null}
            index={index}
            indexLimit={indexLimit}
            onChange={setIndex}
            headers={[
              <FormattedMessage key="datum" id={`Datum`} />,
              <FormattedMessage key="kenteken" id={`Kenteken`} />,
              <FormattedMessage key="status" id={`Status`} />,
            ]}
            rows={product?.verbruiksobject?.data?.periodes
              ?.slice(index * pageSize, (index + 1) * pageSize)
              .map(
                //eslint-disable-next-line @typescript-eslint/no-explicit-any
                (periode: any) => [
                  {
                    children: formatDate({
                      date: periode?.datetimeStart,
                      namedDays: false,
                    }),
                  },
                  { children: periode?.kenteken },
                  {
                    children: <StatusBadge>{periode?.status}</StatusBadge>,
                  },
                ],
              )}
          />
        );
      }}
    </ThemeHistoryPage>
  );
};

export default ParkerenHistory;
