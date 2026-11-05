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
import { existsSync, readdirSync, readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import importPlugin from "eslint-plugin-import";

// Package boundaries: a package in packages/* may only import another package's
// public export (the bare package name), never an internal file path.
//
// The one exception is the declared public subpath export
// "@nl-portal/nl-portal-user-interface/index.css" (see user-interface's package.json
// "exports"). `forbid` cannot express an exception, and minimatch's `!(...)` does not
// reliably exclude a package name in the middle of a path, so the forbidden patterns
// are generated per package: everything for every package, except that user-interface
// only has index.css as allowed subpath. Do NOT add eslint-disable comments or widen
// this: add a new public subpath to the package's "exports" and to the exception
// here, with a test in package-boundaries.test.ts.
const PUBLIC_SUBPATH_PACKAGE = "@nl-portal/nl-portal-user-interface";
const PUBLIC_SUBPATH = "index.css";

const packagesDir = join(
  dirname(fileURLToPath(import.meta.url)),
  "..",
  "packages",
);

function workspacePackageNames() {
  return readdirSync(packagesDir, { withFileTypes: true })
    .filter((entry) => entry.isDirectory())
    .map((entry) => join(packagesDir, entry.name, "package.json"))
    .filter((file) => existsSync(file))
    .map((file) => JSON.parse(readFileSync(file, "utf8")).name)
    .filter(
      (name) => typeof name === "string" && name.startsWith("@nl-portal/"),
    );
}

export const forbiddenInternalPaths = workspacePackageNames().flatMap((name) =>
  name === PUBLIC_SUBPATH_PACKAGE
    ? [`${name}/!(${PUBLIC_SUBPATH})`, `${name}/*/**`]
    : [`${name}/**`],
);

export const packageBoundariesConfig = [
  {
    files: ["packages/**/*.{ts,tsx,js,jsx}"],
    ignores: [
      "**/node_modules/**",
      "**/dist/**",
      "**/build/**",
      "**/generated/**",
    ],
    plugins: { import: importPlugin },
    languageOptions: { sourceType: "module", ecmaVersion: "latest" },
    rules: {
      "import/no-internal-modules": [
        "error",
        { forbid: forbiddenInternalPaths },
      ],
      // Also catches reaching into a sibling package via a relative path (../../api/src/...).
      "import/no-relative-packages": "error",
    },
  },
];
