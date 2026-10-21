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
import { useState } from "react";
import { FormattedMessage } from "react-intl";
import { GetTakenV2Document, TaakV2 } from "@nl-portal/nl-portal-api";
import { useQuery } from "@apollo/client/react";
import TasksList from "../components/TasksList";
import PageHeader from "../components/PageHeader";
import PageGrid from "../components/PageGrid";
import SearchForm from "../components/SearchForm";

const TasksPage = () => {
  const { data, loading, error, refetch } = useQuery(GetTakenV2Document);
  const tasks = data?.getTakenV2.content as TaakV2[] | undefined;
  // GetTakenV2's response has no "current page number" field (unlike e.g. getOpenProducten), so
  // the page index is tracked locally instead of derived from the query result.
  const [pageIndex, setPageIndex] = useState(0);

  const handleFormSubmit = async (searchValue: string) => {
    try {
      await refetch({ title: searchValue });
      setPageIndex(0);
    } catch (error) {
      console.error("Error fetching tasks:", error);
    }
  };

  const onPageChange = async (index: number) => {
    try {
      await refetch({ pageNumber: index + 1 });
      setPageIndex(index);
    } catch (error) {
      console.error("Error fetching tasks:", error);
    }
  };

  return (
    <PageGrid variant="medium">
      <PageHeader
        title={<FormattedMessage id="pageTitles.tasks" />}
      ></PageHeader>
      <SearchForm
        translationId="tasks"
        totalElements={data?.getTakenV2.totalElements ?? 0}
        onSubmit={handleFormSubmit}
      />
      <TasksList
        loading={loading}
        error={Boolean(error)}
        titleTranslationId={null}
        tasks={tasks}
        onChange={onPageChange}
        index={pageIndex}
        indexLimit={(data?.getTakenV2.totalPages || 1) - 1}
        openInContext={true}
      />
    </PageGrid>
  );
};

export default TasksPage;
