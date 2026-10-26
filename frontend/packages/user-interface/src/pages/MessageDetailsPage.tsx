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
import {
  ApiContext,
  Bericht,
  GetBerichtDocument,
} from "@nl-portal/nl-portal-api";
import { skipToken, useQuery } from "@apollo/client/react";
import { useOutletContext, useParams } from "react-router";
import BackLink from "../components/BackLink";
import PageGrid from "../components/PageGrid";
import PageHeader from "../components/PageHeader";
import { Paragraph } from "@utrecht/component-library-react";
import { FormattedDate, FormattedMessage, FormattedTime } from "react-intl";
import { RouterOutletContext } from "../interfaces/router-outlet-context";
import { useContext, useEffect } from "react";
import AppContext from "../contexts/AppContext";
import MessageContent from "../components/MessageContent";
import DocumentsList from "../components/DocumentsList";

const MessageDetailsPage = () => {
  const { id } = useParams();
  const { refetchMessages } = useContext(AppContext);
  const { restUri } = useContext(ApiContext);
  const { paths } = useOutletContext<RouterOutletContext>();
  const {
    data: messageData,
    loading: messageLoading,
    error: messageError,
  } = useQuery(
    GetBerichtDocument,
    id
      ? {
          variables: { id },
        }
      : skipToken,
  );

  useEffect(() => {
    try {
      refetchMessages();
    } catch (error) {
      console.error("Error refetching messages:", error);
    }
  }, [messageData]);

  const message = messageData?.getBericht as Bericht | undefined;

  return (
    <PageGrid variant="medium">
      <div>
        <BackLink href={paths.messages} />
        <PageHeader loading={messageLoading} title={message?.onderwerp}>
          {message?.publicatiedatum && (
            <Paragraph>
              <FormattedMessage
                id="messageDetails.sent"
                values={{
                  date: (
                    <FormattedDate
                      value={message?.publicatiedatum}
                      year="numeric"
                      month="long"
                      day="numeric"
                    />
                  ),
                  time: (
                    <FormattedTime
                      value={message?.publicatiedatum}
                      hour="numeric"
                      minute="numeric"
                    />
                  ),
                }}
              />
            </Paragraph>
          )}
        </PageHeader>
      </div>
      <MessageContent
        loading={messageLoading}
        error={Boolean(messageError)}
        messageText={message?.berichtTekst}
      />
      <DocumentsList
        loading={messageLoading}
        error={Boolean(messageError)}
        documents={message?.documenten}
        getDownloadLink={(doc) =>
          `${restUri}/berichten/${id}/document/${doc.uuid}/content`
        }
      />
    </PageGrid>
  );
};

export default MessageDetailsPage;
