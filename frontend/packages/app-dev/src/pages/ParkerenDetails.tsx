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
import { FormattedDate, FormattedMessage, FormattedNumber } from "react-intl";
import {
  PortalLink,
  TableList,
  ThemeDetailsPage,
} from "@nl-portal/nl-portal-user-interface";
import { useDateFormatter } from "@nl-portal/nl-portal-localization";
import { OpenProductProduct } from "@nl-portal/nl-portal-api";
import { Link } from "@gemeente-denhaag/link";
import { StatusBadge } from "@utrecht/component-library-react";
import { useParams } from "react-router";
import { paths } from "../constants/paths";
import { capitalizeFirstLetter, currencyFormat } from "../util/product-format";

const ParkerenDetails = () => {
  const { formatDate } = useDateFormatter();
  const { slug = "parkeren", productTypeSlug = "vergunningen" } = useParams<{
    slug: string;
    productTypeSlug: string;
  }>();

  return (
    <ThemeDetailsPage
      productSettings={{
        headerTranslationIds: [
          "Naam",
          "Startdatum",
          "Einddatum",
          "Status",
          "Prijs",
        ],
        dataMapping: [
          "naam",
          (product) => (
            <FormattedDate value={product?.startDatum ?? undefined} />
          ),
          (product) => (
            <FormattedDate value={product?.eindDatum ?? undefined} />
          ),
          (product) =>
            capitalizeFirstLetter(product?.status.toLowerCase() ?? ""),
          (product) => {
            return (
              <FormattedNumber
                value={product?.prijs ?? 0}
                {...currencyFormat}
              />
            );
          },
        ],
      }}
    >
      {({ loading, data }) => {
        const product = data?.getOpenProduct as OpenProductProduct | undefined;

        return (
          <>
            {Boolean(product?.verbruiksobject) && (
              <TableList
                loading={loading}
                titleTranslationId={"Voertuigen"}
                headers={[
                  <FormattedMessage key="kenteken" id={`Kenteken`} />,
                  "",
                ]}
                rows={product?.verbruiksobject?.data?.kentekens?.map(
                  (kenteken: string) => [
                    { children: kenteken },
                    {
                      children: (
                        <Link
                          href={`${paths.themeMutate(
                            slug,
                            productTypeSlug,
                            product?.uuid,
                          )}?kenteken=${kenteken}`}
                          Link={PortalLink}
                        >
                          <FormattedMessage id={`Aanmelden`} />
                        </Link>
                      ),
                    },
                  ],
                )}
              />
            )}
            {Boolean(product?.verbruiksobject) && (
              <TableList
                loading={loading}
                titleTranslationId={"Periodes"}
                totalAmount={product?.verbruiksobject?.data?.periodes?.length}
                readMoreTranslationId={"Bekijk alles"}
                readMoreLink={paths.themeHistory(
                  slug,
                  productTypeSlug,
                  product?.uuid,
                )}
                headers={[
                  <FormattedMessage key="datum" id={`Datum`} />,
                  <FormattedMessage key="kenteken" id={`Kenteken`} />,
                  <FormattedMessage key="status" id={`Status`} />,
                ]}
                rows={product?.verbruiksobject?.data?.periodes?.map(
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
            )}
          </>
        );
      }}
    </ThemeDetailsPage>
  );
};

export default ParkerenDetails;
