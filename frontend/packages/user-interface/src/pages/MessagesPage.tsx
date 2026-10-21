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
import { useIntl } from "react-intl";
import PageGrid from "../components/PageGrid";
import PageHeader from "../components/PageHeader";
import MessagesList from "../components/MessagesList";
import { Bericht, GetBerichtenDocument } from "@nl-portal/nl-portal-api";
import { useQuery } from "@apollo/client/react";
import SearchForm from "../components/SearchForm";

const MessagesPage = () => {
  const intl = useIntl();
  const { data, loading, error, refetch } = useQuery(GetBerichtenDocument, {
    variables: { pageSize: 10 },
    fetchPolicy: "cache-and-network",
  });
  const messages = data?.getBerichten.content as Bericht[] | undefined;

  const handleFormSubmit = async (searchValue: string) => {
    try {
      await refetch({ onderwerp: searchValue });
    } catch (error) {
      console.error("Error fetching messages:", error);
    }
  };

  const onPageChange = async (index: number) => {
    try {
      await refetch({ pageNumber: index + 1 });
    } catch (error) {
      console.error("Error fetching messages:", error);
    }
  };

  return (
    <PageGrid variant="medium">
      <PageHeader
        title={intl.formatMessage({ id: "pageTitles.messages" })}
      ></PageHeader>
      <SearchForm
        translationId="messages"
        totalElements={data?.getBerichten.totalElements ?? 0}
        onSubmit={handleFormSubmit}
      />
      <MessagesList
        loading={loading}
        error={Boolean(error)}
        messages={messages}
        indexLimit={
          data?.getBerichten.totalPages && data.getBerichten.totalPages - 1
        }
        onChange={onPageChange}
      />
    </PageGrid>
  );
};

export default MessagesPage;
