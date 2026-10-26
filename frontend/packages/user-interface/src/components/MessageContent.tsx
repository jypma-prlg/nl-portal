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
import styles from "./MessageContent.module.scss";
import { Paragraph } from "@utrecht/component-library-react";
import Skeleton from "./Skeleton";
import { FormattedMessage } from "react-intl";
import { parseLinks } from "../utils/parse-links";
import { messageContentHeight } from "../constants/skeleton";

interface Props {
  loading?: boolean;
  error?: boolean;
  messageText?: string;
}

const MessageContent = ({ loading, error, messageText }: Props) => {
  if (loading) {
    return (
      <div>
        <Skeleton height={messageContentHeight} />
      </div>
    );
  }

  if (error) {
    return (
      <div>
        <Paragraph>
          <FormattedMessage id="messageContent.fetchError" />
        </Paragraph>
      </div>
    );
  }

  if (!messageText) {
    return (
      <div>
        <Paragraph>
          <FormattedMessage id="messageContent.fetchError" />
        </Paragraph>
      </div>
    );
  }

  return (
    <div className={styles["message-content"]}>
      <Paragraph>{parseLinks(messageText)}</Paragraph>
    </div>
  );
};

export default MessageContent;
