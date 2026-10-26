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
import { Document as PortalDocument } from "@nl-portal/nl-portal-api";
import { Paragraph } from "@utrecht/component-library-react";
import { useIntl } from "react-intl";
import styles from "./DocumentsList.module.scss";
import Document from "./Document";
import SectionHeader from "./SectionHeader";
import Skeleton from "./Skeleton";
import classnames from "classnames";
import { listViewHeight } from "../constants/skeleton";

interface Props {
  loading?: boolean;
  error?: boolean;
  errorTranslationId?: string;
  emptyTranslationId?: string;
  titleTranslationId?: string | null;
  documents?: Array<PortalDocument>;
  getDownloadLink: (document: PortalDocument) => string;
}

const DocumentsList = ({
  loading,
  error,
  errorTranslationId = "documentsList.fetchError",
  emptyTranslationId = "documentsList.empty",
  titleTranslationId = "documentsList.title",
  documents,
  getDownloadLink,
}: Props) => {
  const intl = useIntl();
  const title = titleTranslationId
    ? intl.formatMessage({ id: titleTranslationId })
    : undefined;
  const errorMessage = intl.formatMessage({ id: errorTranslationId });
  const emptyMessage = intl.formatMessage({ id: emptyTranslationId });

  if (loading) {
    return (
      <section className={styles["documents-list"]}>
        <SectionHeader title={title} />
        <Skeleton height={listViewHeight} />
        <Skeleton height={listViewHeight} />
        <Skeleton height={listViewHeight} />
      </section>
    );
  }

  if (error)
    return (
      <section>
        <SectionHeader title={title} />
        <Paragraph>{errorMessage}</Paragraph>
      </section>
    );

  if (!documents || documents.length === 0)
    return (
      <section>
        <SectionHeader title={title} />
        <Paragraph>{emptyMessage}</Paragraph>
      </section>
    );

  return (
    <section>
      <SectionHeader title={title} />
      <div className={classnames(styles["documents-list__documents"])}>
        {documents.map((document) => (
          <Document
            key={document.uuid}
            document={document}
            downloadLink={getDownloadLink(document)}
          />
        ))}
      </div>
    </section>
  );
};

export default DocumentsList;
