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
import { FormField } from "@gemeente-denhaag/form-field";
import { Form } from "./Form";
import { FormLabel } from "@gemeente-denhaag/form-label";
import { TextInput } from "@gemeente-denhaag/text-input";
import { SearchIcon } from "@gemeente-denhaag/icons";
import { Button } from "@gemeente-denhaag/button";
import useInput, { Validation } from "../hooks/useInput";
import { FormEvent } from "react";
import styles from "./SearchForm.module.scss";
import { FormattedMessage } from "react-intl";
import { Paragraph } from "@utrecht/component-library-react";
import { FormFieldErrorMessage } from "@gemeente-denhaag/form-field-error-message";

interface SearchFormProps {
  translationId: string;
  defaultSearchValue?: string;
  totalElements: number | null;
  searchValidations?: Validation[];
  onSubmit: (searchValue: string) => void;
}

const SearchForm = ({
  translationId,
  defaultSearchValue = "",
  totalElements,
  onSubmit,
  searchValidations = [{ validationFn: () => true, errorTranslationId: "" }],
}: SearchFormProps) => {
  const {
    value: searchTitleValue,
    handleInputChange: handleSearchTitleChange,
    handleInputBlur: handleSearchTitleBlur,
    hasError,
    errorTranslationId,
  } = useInput(defaultSearchValue, searchValidations);

  const handleFormSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const formData = new FormData(event.currentTarget);
    const searchInputValue = formData.get("search-input");

    if (typeof searchInputValue === "string") {
      onSubmit(searchInputValue);
    }
  };

  return (
    <Form
      className={styles["search-form"]}
      onSubmit={handleFormSubmit}
      hideSubmit
    >
      <FormField invalid={hasError}>
        <FormLabel htmlFor="search-input">
          <FormattedMessage id={`searchForm.${translationId}.searchLabel`} />
        </FormLabel>
        <div className={styles["search-form-input-container"]}>
          <TextInput
            id="search-input"
            name="search-input"
            value={searchTitleValue}
            onChange={handleSearchTitleChange}
            onBlur={handleSearchTitleBlur}
            iconEnd={<SearchIcon />}
            invalid={hasError}
          ></TextInput>
          {hasError && (
            <FormFieldErrorMessage>
              <FormattedMessage id={errorTranslationId} />
            </FormFieldErrorMessage>
          )}
          <Button type="submit">
            <FormattedMessage id={`searchForm.${translationId}.searchButton`} />
          </Button>
        </div>
      </FormField>
      {totalElements !== null && (
        <Paragraph className={styles["search-form-total-elements"]}>
          <FormattedMessage
            id={
              totalElements === 1
                ? `searchForm.${translationId}.totalElements.singular`
                : `searchForm.${translationId}.totalElements`
            }
            values={{ total: totalElements }}
          />
        </Paragraph>
      )}
    </Form>
  );
};

export default SearchForm;
