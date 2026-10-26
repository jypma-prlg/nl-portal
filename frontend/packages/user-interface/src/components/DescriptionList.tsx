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
import { DescriptionList as DescriptionListComponent } from "@gemeente-denhaag/descriptionlist";
import SectionHeader from "./SectionHeader";
import { useIntl } from "react-intl";
import Skeleton from "./Skeleton";
import { Paragraph } from "@utrecht/component-library-react";
import styles from "./DescriptionList.module.scss";
import { listViewHeight } from "../constants/skeleton";

interface Props {
  loading?: boolean;
  error?: boolean;
  errorTranslationId?: string;
  showEmpty?: boolean;
  emptyTranslationId?: string;
  titleTranslationId?: string | null;
  items: {
    title: React.ReactNode;
    detail: React.ReactNode;
    action?: React.ReactNode;
  }[];
  children?: React.ReactNode;
}

const DescriptionList = ({
  loading,
  error,
  errorTranslationId = "descriptionList.fetchError",
  showEmpty = true,
  emptyTranslationId = "descriptionList.empty",
  titleTranslationId = "descriptionList.title",
  items,
  children,
}: Props) => {
  const intl = useIntl();
  const errorMessage = intl.formatMessage({ id: errorTranslationId });
  const emptyMessage = intl.formatMessage({ id: emptyTranslationId });
  const title = titleTranslationId
    ? intl.formatMessage({ id: titleTranslationId })
    : undefined;

  if (loading) {
    return (
      <section>
        <SectionHeader title={title} />
        <div>
          <Skeleton height={listViewHeight} />
          <Skeleton height={listViewHeight} />
          <Skeleton height={listViewHeight} />
        </div>
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

  if (!items || items.length === 0) {
    if (!showEmpty) return null;
    return (
      <section>
        <SectionHeader title={title} />
        <Paragraph>{emptyMessage}</Paragraph>
        {children && (
          <div className={styles["description-list__children"]}>{children}</div>
        )}
      </section>
    );
  }

  return (
    <section>
      <SectionHeader title={title} />
      <DescriptionListComponent items={items} />
      {children && (
        <div className={styles["description-list__children"]}>{children}</div>
      )}
    </section>
  );
};

export default DescriptionList;
