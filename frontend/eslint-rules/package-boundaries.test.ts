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
import { packageBoundariesConfig } from "./package-boundaries.mjs";

const linter = new Linter({ configType: "flat" });

function lint(source: string): string[] {
  return linter
    .verify(source, packageBoundariesConfig, {
      filename: "packages/app/src/App.js",
    })
    .map((message) => `${message.ruleId}: ${message.message}`);
}

describe("package boundaries (import/no-internal-modules)", () => {
  it("allows the public entry point of a package", () => {
    expect(lint(`import "@nl-portal/nl-portal-authentication";`)).toEqual([]);
  });

  it("allows the declared index.css subpath of user-interface", () => {
    expect(
      lint(`import "@nl-portal/nl-portal-user-interface/index.css";`),
    ).toEqual([]);
  });

  it.each([
    "@nl-portal/nl-portal-user-interface/src/components/Foo",
    "@nl-portal/nl-portal-user-interface/other.css",
    "@nl-portal/nl-portal-user-interface/dist/index.css",
    "@nl-portal/nl-portal-authentication/index.css",
    "@nl-portal/nl-portal-authentication/src/internal",
    "@nl-portal/nl-portal-api/dist/index.js",
  ])("rejects the internal import %s", (specifier) => {
    const messages = lint(`import "${specifier}";`);
    expect(messages).toHaveLength(1);
    expect(messages[0]).toContain("import/no-internal-modules");
  });
});
