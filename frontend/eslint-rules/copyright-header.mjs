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
import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

// Copyright headers (ADR 0015). A file whose header names Den Haag, Ritense keeps that header with a
// frozen year; every other file gets the PinkRoccade header. The templates are shared with the
// backend's Spotless config.
const licensesDir = join(
  dirname(fileURLToPath(import.meta.url)),
  "..",
  "..",
  "backend",
  "licenses",
);
const template = (name) =>
  readFileSync(join(licensesDir, name), "utf8").replace(/\r\n/g, "\n");
const PINK_TEMPLATE = template("pinkroccade.template");
const RITENSE_TEMPLATE = template("ritense.template");

const pinkHeader = (years) => PINK_TEMPLATE.replace("$YEAR", years);
const ritenseHeader = (endYear) => RITENSE_TEMPLATE.replace("$YEAR", endYear);

const SHEBANG = /^#!.*\r?\n/;
// The block comment at the start of the file (after an optional shebang), with leading whitespace.
const LEADING_COMMENT = /^\s*\/\*[\s\S]*?\*\/(\r?\n)?/;
const RITENSE_COPYRIGHT = /Copyright 2015-(\d{4}) Den Haag, Ritense/;
const PINK_COPYRIGHT = /Copyright ((\d{4})(?:-(\d{4}))?) PinkRoccade/;
// Ritense years are frozen since the fork; a Ritense header without a readable year gets this one.
const RITENSE_FROZEN_YEAR = "2026";

const headerRule = {
  meta: {
    type: "layout",
    fixable: "code",
    schema: [
      {
        type: "object",
        properties: {
          currentYear: { type: "integer" },
          bumpYear: { type: "boolean" },
        },
        additionalProperties: false,
      },
    ],
    messages: {
      missing:
        "Missing EUPL 1.2 copyright header (ADR 0015). Run `pnpm lint:fix`.",
      malformed:
        "Malformed EUPL 1.2 copyright header (ADR 0015). Run `pnpm lint:fix`.",
      outdatedYear:
        "Copyright year is not up to date (ADR 0015). Run `pnpm lint:fix`.",
    },
  },
  create(context) {
    const options = context.options[0] ?? {};
    const currentYear = String(options.currentYear ?? new Date().getFullYear());
    return {
      Program(node) {
        const text = context.sourceCode.text;
        const withEol = (lf) =>
          text.includes("\r\n") ? lf.replace(/\n/g, "\r\n") : lf;
        const offset = SHEBANG.exec(text)?.[0].length ?? 0;
        const comment = LEADING_COMMENT.exec(text.slice(offset))?.[0] ?? "";
        const header = comment.replace(/\r\n/g, "\n").trimStart();

        const requireHeader = (expected, messageId) => {
          if (comment === withEol(expected)) return;
          context.report({
            node,
            messageId,
            fix: (fixer) =>
              fixer.replaceTextRange(
                [offset, offset + comment.length],
                withEol(expected),
              ),
          });
        };

        if (header.includes("Den Haag, Ritense")) {
          const endYear =
            RITENSE_COPYRIGHT.exec(header)?.[1] ?? RITENSE_FROZEN_YEAR;
          requireHeader(ritenseHeader(endYear), "malformed");
          return;
        }
        if (header.includes("PinkRoccade")) {
          const [
            ,
            years = currentYear,
            startYear = currentYear,
            endYear = startYear,
          ] = PINK_COPYRIGHT.exec(header) ?? [];
          const upToDate =
            options.bumpYear && endYear !== currentYear
              ? `${startYear}-${currentYear}`
              : years;
          requireHeader(
            pinkHeader(upToDate),
            header === pinkHeader(years) ? "outdatedYear" : "malformed",
          );
          return;
        }
        context.report({
          node,
          messageId: "missing",
          fix: (fixer) =>
            fixer.insertTextBeforeRange(
              [offset, offset],
              withEol(pinkHeader(currentYear)),
            ),
        });
      },
    };
  },
};

export const copyrightHeaderPlugin = { rules: { header: headerRule } };

// bumpYear is off for `pnpm lint` / CI, so an old PinkRoccade year never fails the build. Lint-staged
// runs eslint.copyright-year.config.mjs with bumpYear on, so only committed files get the current year.
export const copyrightHeaderConfig = ({ bumpYear = false } = {}) => [
  {
    files: ["packages/**/*.{ts,tsx,js,jsx}"],
    ignores: [
      "**/node_modules/**",
      "**/dist/**",
      "**/build/**",
      "**/.turbo/**",
      "**/coverage/**",
      "**/*.d.ts",
      "**/generated/**",
      "**/*.generated.ts",
    ],
    plugins: { copyright: copyrightHeaderPlugin },
    rules: { "copyright/header": ["error", { bumpYear }] },
  },
];
