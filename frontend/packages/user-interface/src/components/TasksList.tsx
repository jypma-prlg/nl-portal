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
import styles from "./TasksList.module.scss";
import { Paragraph } from "@utrecht/component-library-react";
import Skeleton from "./Skeleton";
import { TaakV2 } from "@nl-portal/nl-portal-api";
import Task from "./Task";
import { Pagination } from "@gemeente-denhaag/pagination";
import SectionHeader from "./SectionHeader";
import { useOutletContext } from "react-router";
import { RouterOutletContext } from "../interfaces/router-outlet-context";
import { listViewHeight } from "../constants/skeleton";

interface Props {
  loading?: boolean;
  error?: boolean;
  errorTranslationId?: string;
  showEmpty?: boolean;
  emptyTranslationId?: string;
  titleTranslationId?: string | null;
  openInContext?: boolean;
  readMoreLink?: string;
  readMoreTranslationId?: string | null;
  totalAmount?: number;
  tasks?: TaakV2[];
  index?: number;
  indexLimit?: number;
  onChange?: (index: number) => void;
}

const TasksList = ({
  loading,
  error,
  errorTranslationId = "tasksList.fetchError",
  showEmpty = true,
  emptyTranslationId = "tasksList.empty",
  titleTranslationId = "tasksList.title",
  openInContext,
  readMoreLink,
  readMoreTranslationId = "tasksList.viewAll",
  /* eslint-disable @typescript-eslint/no-unused-vars */
  // @ts-expect-error: TS6133
  totalAmount,
  /* eslint-enable @typescript-eslint/no-unused-vars */
  tasks,
  index,
  indexLimit,
  onChange,
}: Props) => {
  const intl = useIntl();
  const { paths } = useOutletContext<RouterOutletContext>();
  const tasksPath = readMoreLink || paths.tasks;
  const title = titleTranslationId
    ? intl.formatMessage({ id: titleTranslationId })
    : undefined;
  const subTitle = readMoreTranslationId
    ? intl.formatMessage({ id: readMoreTranslationId })
    : undefined;
  const errorMessage = intl.formatMessage({ id: errorTranslationId });
  const emptyMessage = intl.formatMessage({ id: emptyTranslationId });

  if (!loading) {
    if (error)
      return (
        <section className={styles["tasks-list"]}>
          <SectionHeader title={title} />
          <Paragraph>{errorMessage}</Paragraph>
        </section>
      );

    if (!tasks || tasks.length === 0) {
      if (!showEmpty) return null;
      return (
        <section className={styles["tasks-list"]}>
          <SectionHeader title={title} />
          <Paragraph>{emptyMessage}</Paragraph>
        </section>
      );
    }
  }

  return (
    <section className={styles["tasks-list"]}>
      <SectionHeader title={title} subTitle={subTitle} href={tasksPath} />
      {loading ? (
        <>
          {[...Array(3)].map((_, index) => (
            <Skeleton key={index} height={listViewHeight} />
          ))}
        </>
      ) : (
        tasks?.map((task) => (
          <Task key={task.id} task={task} openInContext={openInContext} />
        ))
      )}
      {indexLimit ? (
        <Pagination
          className={`denhaag-pagination--center ${styles["tasks-list__pagination"]}`}
          index={index}
          indexLimit={indexLimit}
          onChange={onChange}
        />
      ) : null}
    </section>
  );
};

export default TasksList;
