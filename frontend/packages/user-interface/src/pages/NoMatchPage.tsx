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
import { Paragraph } from "@utrecht/component-library-react";
import PageGrid from "../components/PageGrid";
import PageHeader from "../components/PageHeader";
import { Link } from "@gemeente-denhaag/link";
import { useOutletContext } from "react-router";
import { RouterOutletContext } from "../interfaces/router-outlet-context";
import PortalLink from "../components/PortalLink";
import Heading from "../components/Heading";
import { FormattedMessage, useIntl } from "react-intl";
import { AnchorHTMLAttributes } from "react";

interface Props {
  contactLink?: AnchorHTMLAttributes<HTMLAnchorElement>;
}

const NoMatchPage = ({ contactLink }: Props) => {
  const intl = useIntl();
  const { paths } = useOutletContext<RouterOutletContext>();

  return (
    <PageGrid>
      <PageHeader title={<FormattedMessage id="noMatchPage.title" />}>
        <Paragraph>
          <FormattedMessage
            id="noMatchPage.text"
            values={{
              link: (chunks) => (
                <>
                  <br />
                  <br />
                  <Link Link={PortalLink} href={paths.overview}>
                    {chunks}
                  </Link>
                </>
              ),
            }}
          />
        </Paragraph>
      </PageHeader>
      {contactLink && (
        <section>
          <Heading as="h2" size="h3">
            <FormattedMessage id="noMatchPage.contact.title" />
          </Heading>
          <Paragraph>
            <FormattedMessage
              id="noMatchPage.contact.text"
              values={{
                link: (chunks) => (
                  <Link
                    href={intl.formatMessage({
                      id: "noMatchPage.contact.href",
                    })}
                    Link={PortalLink}
                    {...contactLink}
                  >
                    {chunks}
                  </Link>
                ),
              }}
            />
          </Paragraph>
        </section>
      )}
    </PageGrid>
  );
};

export default NoMatchPage;
