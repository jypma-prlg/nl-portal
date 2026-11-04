import globals from "globals";
import importPlugin from "eslint-plugin-import";
import reactPlugin from "eslint-plugin-react";
import hooksPlugin from "eslint-plugin-react-hooks";
import jsxA11yPlugin from "eslint-plugin-jsx-a11y";
import typescriptParser from "@typescript-eslint/parser";
import typescriptPlugin from "@typescript-eslint/eslint-plugin";
import notice from "eslint-plugin-notice";
import prettierConfig from "eslint-config-prettier";

const reactRule = {
  "react/react-in-jsx-scope": "off",
  "react-hooks/exhaustive-deps": "off",
};

const licenseHeader = `/*
 * Copyright 2015-${new Date().getFullYear()} Den Haag, Ritense, the Netherlands.
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
`;

export default [
  {
    ignores: [
      "**/node_modules/",
      "**/build/",
      "**/dist/",
      "**/coverage/",
      "**/generated/",
    ],
  },
  {
    languageOptions: {
      ecmaVersion: 2020,
      sourceType: "module",
      globals: {
        ...globals.browser,
        ...globals.node,
      },
    },
  },
  {
    files: ["**/*.js", "**/*.jsx", "**/*.mjs"],
    plugins: {
      import: importPlugin,
      react: reactPlugin,
      "react-hooks": hooksPlugin,
      "jsx-a11y": jsxA11yPlugin,
    },
    settings: {
      react: {
        version: "detect",
      },
    },
    rules: {
      ...jsxA11yPlugin.configs.recommended.rules,
      ...reactPlugin.configs.recommended.rules,
      ...hooksPlugin.configs.recommended.rules,
      ...reactRule,
    },
  },
  {
    files: ["**/*.ts", "**/*.tsx"],
    plugins: {
      "@typescript-eslint": typescriptPlugin,
      import: importPlugin,
      react: reactPlugin,
      "react-hooks": hooksPlugin,
      "jsx-a11y": jsxA11yPlugin,
    },
    languageOptions: {
      parser: typescriptParser,
    },
    settings: {
      react: {
        version: "detect",
      },
    },
    rules: {
      ...jsxA11yPlugin.configs.recommended.rules,
      ...reactPlugin.configs.recommended.rules,
      ...hooksPlugin.configs.recommended.rules,
      ...typescriptPlugin.configs.recommended.rules,
      ...reactRule,
    },
  },
  {
    files: ["packages/user-interface/src/components/formio/**/*.{ts,tsx}"],
    rules: {
      // Disable the rule inside this folder
      "@typescript-eslint/no-explicit-any": "off",
    },
  },
  {
    // Package boundaries: a package in packages/* may only import another package's
    // public export (the bare package name), never an internal file path. Existing
    // packages only export "." (see each package.json's "exports" field), so any
    // subpath import is necessarily internal.
    files: ["packages/**/*.{ts,tsx,js,jsx}"],
    ignores: [
      "**/node_modules/**",
      "**/dist/**",
      "**/build/**",
      "**/generated/**",
    ],
    plugins: { import: importPlugin },
    rules: {
      "import/no-internal-modules": [
        "error",
        {
          forbid: ["@nl-portal/*/**"],
        },
      ],
      // Also catches reaching into a sibling package via a relative path (../../api/src/...).
      "import/no-relative-packages": "error",
    },
  },
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
    plugins: { notice },
    rules: {
      "notice/notice": [
        "error",
        {
          template: licenseHeader,
          onNonMatchingHeader: "replace",
          nonMatchingTolerance: 0.7,
          messages: {
            whenFailedToMatch:
              "Missing or malformed EUPL 1.2 copyright header. Run `pnpm lint:fix`.",
            whenOutsideTolerance:
              "Copyright header is malformed. Run `pnpm lint:fix`.",
          },
        },
      ],
    },
  },
  prettierConfig,
];
