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

// Navigation structure decided in ADR 0006 (docs/adr/0006-unified-docs-site.md):
// four sections in the main nav, Compliance reachable only via the footer.

export const MAIN_NAV_SECTIONS = [
  "Architectuur",
  "Gebruik & inrichting",
  "Hosting & configuratie",
  "Development",
] as const;

export const COMPLIANCE_SECTION = "Compliance";

export interface NavLink {
  text: string;
  link: string;
}

const SECTION_LINKS: Record<(typeof MAIN_NAV_SECTIONS)[number], string> = {
  Architectuur: "/architectuur/",
  "Gebruik & inrichting": "/gebruik-en-inrichting/",
  "Hosting & configuratie": "/hosting-en-configuratie/",
  Development: "/development/",
};

export const nav: NavLink[] = MAIN_NAV_SECTIONS.map((text) => ({
  text,
  link: SECTION_LINKS[text],
}));

export const footerComplianceLink: NavLink = {
  text: COMPLIANCE_SECTION,
  link: "/compliance/",
};
