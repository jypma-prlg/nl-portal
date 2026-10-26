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
import { FormattedMessage, useIntl } from "react-intl";
import styles from "./MessagesList.module.scss";
import { Paragraph } from "@utrecht/component-library-react";
import Skeleton from "./Skeleton";
import { Pagination } from "@gemeente-denhaag/pagination";
import Message from "./Message";
import Table from "./Table";
import { Bericht } from "@nl-portal/nl-portal-api";
import { listViewHeight } from "../constants/skeleton";

interface Props {
  loading?: boolean;
  error?: boolean;
  errorTranslationId?: string;
  showEmpty?: boolean;
  emptyTranslationId?: string;
  titleTranslationId?: string | null;
  readMoreLink?: string;
  readMoreTranslationId?: string | null;
  totalAmount?: number;
  messages?: Bericht[];
  index?: number;
  indexLimit?: number;
  onChange?: (index: number) => void;
}

const MessagesList = ({
  loading,
  error,
  errorTranslationId = "messagesList.fetchError",
  showEmpty = true,
  emptyTranslationId = "messagesList.empty",
  messages,
  index,
  indexLimit,
  onChange,
}: Props) => {
  const intl = useIntl();
  const errorMessage = intl.formatMessage({ id: errorTranslationId });
  const emptyMessage = intl.formatMessage({ id: emptyTranslationId });

  if (!loading) {
    if (error)
      return (
        <section className={styles["messages-list"]}>
          <Paragraph>{errorMessage}</Paragraph>
        </section>
      );

    if (!messages || messages.length === 0) {
      if (!showEmpty) return null;
      return (
        <section className={styles["messages-list"]}>
          <Paragraph>{emptyMessage}</Paragraph>
        </section>
      );
    }
  }

  return (
    <section className={styles["messages-list"]}>
      <Table
        headers={[
          {
            key: "subject",
            head: true,
            children: <FormattedMessage id="messagesList.subject" />,
          },
          {
            key: "date",
            head: true,
            className: "denhaag-table__cell--align-end",
            children: <FormattedMessage id="messagesList.date" />,
          },
        ]}
      />
      {loading ? (
        <>
          {[...Array(3)].map((_, index) => (
            <Skeleton key={index} height={listViewHeight} />
          ))}
        </>
      ) : (
        messages?.map((message) => (
          <Message key={message.id} message={message} />
        ))
      )}
      {indexLimit ? (
        <Pagination
          className={`denhaag-pagination--center ${styles["messages-list__pagination"]}`}
          index={index}
          indexLimit={indexLimit}
          onChange={onChange}
        />
      ) : null}
    </section>
  );
};

export default MessagesList;
