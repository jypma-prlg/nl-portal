/*
 * Copyright 2026 PinkRoccade Local Government B.V.
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
import typescriptParser from "@typescript-eslint/parser";
import { copyrightHeaderConfig } from "./eslint-rules/copyright-header.mjs";

// Lint-staged only (ADR 0015): brings the PinkRoccade end year of committed files up to date.
// The regular eslint.config.mjs accepts any year, so CI does not fail on 1 January.
export default [
  {
    files: ["**/*.{ts,tsx}"],
    languageOptions: { parser: typescriptParser },
  },
  ...copyrightHeaderConfig({ bumpYear: true }),
];
