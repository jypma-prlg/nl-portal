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

import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  build: {
    chunkSizeWarningLimit: 2048,
    rolldownOptions: {
      output: {
        codeSplitting: {
          groups: [
            {
              name(id) {
                if (id.includes("@formio")) {
                  return "formio";
                }

                if (id.includes("@gemeente-denhaag")) {
                  return "gemeente-denhaag";
                }

                if (id.includes("@nl-portal")) {
                  return "nl-portal";
                }

                return null;
              },
            },
          ],
        },
      },
    },
    outDir: "build",
  },
  html: {
    cspNonce: "##NL_PORTAL_NONCE##",
  },
  plugins: [react()],
  test: {
    css: true,
    environment: "jsdom",
    globals: true,
    setupFiles: "./src/setupTests.ts",
  },
  ssr: { noExternal: true },
});
