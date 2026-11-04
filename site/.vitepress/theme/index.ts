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

// The default theme hides `themeConfig.footer` on any page that has a sidebar, which is every page
// of the four main sections. Compliance is footer-only (ADR 0006), so render its link on all pages.

import { h } from "vue";
import DefaultTheme from "vitepress/theme";
import type { Theme } from "vitepress";
import { footerComplianceLink } from "../sections";

export default {
  extends: DefaultTheme,
  Layout: () =>
    h(DefaultTheme.Layout, null, {
      "layout-bottom": () =>
        h("footer", { class: "compliance-footer", style: "text-align:center;padding:16px;font-size:14px" }, [
          h("a", { href: footerComplianceLink.link }, footerComplianceLink.text),
        ]),
    }),
} satisfies Theme;
