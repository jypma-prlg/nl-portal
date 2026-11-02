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

import { defineConfig } from "vitepress";
import { nav, footerComplianceLink } from "./sections";

export default defineConfig({
  title: "NL Portal",
  description: "NL Portal documentatie",
  srcDir: ".",
  cleanUrls: true,
  // Fail the build on any broken internal link or anchor — load-bearing while
  // ~80 files move from docs/ and documentation/ into this site.
  ignoreDeadLinks: false,
  themeConfig: {
    nav,
    sidebar: {
      "/architectuur/": [{ text: "Architectuur", items: [{ text: "Overzicht", link: "/architectuur/" }] }],
      "/gebruik-en-inrichting/": [
        { text: "Gebruik & inrichting", items: [{ text: "Overzicht", link: "/gebruik-en-inrichting/" }] },
      ],
      "/hosting-en-configuratie/": [
        { text: "Hosting & configuratie", items: [{ text: "Overzicht", link: "/hosting-en-configuratie/" }] },
      ],
      "/development/": [{ text: "Development", items: [{ text: "Overzicht", link: "/development/" }] }],
      "/compliance/": [{ text: "Compliance", items: [{ text: "Overzicht", link: "/compliance/" }] }],
    },
    footer: {
      message: `<a href="${footerComplianceLink.link}">${footerComplianceLink.text}</a>`,
      copyright: "PinkRoccade",
    },
  },
});
