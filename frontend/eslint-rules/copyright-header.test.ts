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
import { Linter } from "eslint";
import { describe, expect, it } from "vitest";
// @ts-expect-error plain .mjs module without type declarations
import { copyrightHeaderPlugin } from "./copyright-header.mjs";

const EUPL_BODY = ` *
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
`;

const pinkHeader = (years: string) =>
  `/*\n * Copyright ${years} PinkRoccade Local Government B.V.\n${EUPL_BODY}`;
const ritenseHeader = (endYear: string) =>
  `/*\n * Copyright 2015-${endYear} Den Haag, Ritense, the Netherlands.\n${EUPL_BODY}`;

const CODE = `export const answer = 42;\n`;

function config(options: Record<string, unknown> = {}): Linter.Config[] {
  return [
    {
      files: ["**/*.ts"],
      plugins: { copyright: copyrightHeaderPlugin },
      rules: {
        "copyright/header": ["error", { currentYear: 2027, ...options }],
      },
    },
  ];
}

const linter = new Linter({ configType: "flat" });

function fix(source: string, options?: Record<string, unknown>) {
  return linter.verifyAndFix(source, config(options), { filename: "a.ts" })
    .output;
}

function problems(source: string, options?: Record<string, unknown>) {
  return linter
    .verify(source, config(options), { filename: "a.ts" })
    .map((message) => `${message.ruleId}: ${message.message}`);
}

describe("copyright header (ADR 0015)", () => {
  it("gives a file without header the PinkRoccade header with the current year", () => {
    expect(problems(CODE)).toEqual([
      "copyright/header: Missing EUPL 1.2 copyright header (ADR 0015). Run `pnpm lint:fix`.",
    ]);
    expect(fix(CODE)).toBe(pinkHeader("2027") + CODE);
  });

  it.each(["2023", "2026"])(
    "keeps an existing Ritense header with frozen end year %s",
    (endYear) => {
      const source = ritenseHeader(endYear) + CODE;
      expect(problems(source)).toEqual([]);
      expect(problems(source, { bumpYear: true })).toEqual([]);
    },
  );

  it("repairs a malformed Ritense header as a Ritense header, keeping its year", () => {
    const malformed =
      ritenseHeader("2023").replace(" * You may obtain", " * you may obtain") +
      CODE;
    expect(problems(malformed)).toEqual([
      "copyright/header: Malformed EUPL 1.2 copyright header (ADR 0015). Run `pnpm lint:fix`.",
    ]);
    expect(fix(malformed)).toBe(ritenseHeader("2023") + CODE);
  });

  it.each(["2025", "2025-2026", "2027"])(
    "accepts a PinkRoccade header with year %s when not bumping",
    (years) => {
      expect(problems(pinkHeader(years) + CODE)).toEqual([]);
    },
  );

  it.each([
    ["2025", "2025-2027"],
    ["2025-2026", "2025-2027"],
  ])(
    "bumps the PinkRoccade end year from %s to %s when bumping",
    (years, bumped) => {
      const source = pinkHeader(years) + CODE;
      expect(problems(source, { bumpYear: true })).toEqual([
        "copyright/header: Copyright year is not up to date (ADR 0015). Run `pnpm lint:fix`.",
      ]);
      expect(fix(source, { bumpYear: true })).toBe(pinkHeader(bumped) + CODE);
    },
  );

  it("puts the header above an existing leading comment instead of replacing it", () => {
    const source = `/* Copyright 2020 Someone Else, MIT */\n${CODE}`;
    expect(fix(source)).toBe(pinkHeader("2027") + source);
  });

  it.each(["2027", "2025-2027"])(
    "leaves an up-to-date PinkRoccade year %s alone when bumping",
    (years) => {
      expect(problems(pinkHeader(years) + CODE, { bumpYear: true })).toEqual(
        [],
      );
    },
  );

  it("repairs a PinkRoccade header with a mangled copyright line instead of adding a second one", () => {
    const mangled = pinkHeader("2025").replace("B.V.", "BV") + CODE;
    expect(fix(mangled)).toBe(pinkHeader("2025") + CODE);
  });

  it("keeps a Ritense header that is preceded by a blank line", () => {
    expect(fix(`\n${ritenseHeader("2023")}${CODE}`)).toBe(
      ritenseHeader("2023") + CODE,
    );
  });

  it("keeps a CRLF file CRLF when it repairs the header", () => {
    const crlf = (text: string) => text.replace(/\n/g, "\r\n");
    expect(fix(crlf(CODE))).toBe(crlf(pinkHeader("2027") + CODE));
    expect(problems(crlf(ritenseHeader("2023") + CODE))).toEqual([]);
  });

  it("puts the header below a shebang line", () => {
    const shebang = "#!/usr/bin/env node\n";
    expect(fix(shebang + CODE)).toBe(shebang + pinkHeader("2027") + CODE);
    expect(problems(shebang + ritenseHeader("2023") + CODE)).toEqual([]);
  });
});
