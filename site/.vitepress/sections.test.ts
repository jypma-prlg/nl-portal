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

import { describe, expect, it } from "vitest";
import { MAIN_NAV_SECTIONS, COMPLIANCE_SECTION, nav, footerComplianceLink } from "./sections";

describe("site navigation structure (ADR 0006)", () => {
  it("has exactly the five ADR 0006 sections, Compliance last", () => {
    expect([...MAIN_NAV_SECTIONS, COMPLIANCE_SECTION]).toEqual([
      "Architectuur",
      "Gebruik & inrichting",
      "Hosting & configuratie",
      "Development",
      "Compliance",
    ]);
  });

  it("puts the four non-Compliance sections in the main nav", () => {
    const navTitles = nav.map((item) => item.text);
    expect(navTitles).toEqual([...MAIN_NAV_SECTIONS]);
  });

  it("never puts Compliance in the main nav", () => {
    const navTitles = nav.map((item) => item.text);
    expect(navTitles).not.toContain(COMPLIANCE_SECTION);
  });

  it("exposes Compliance only as a footer link", () => {
    expect(footerComplianceLink.text).toBe(COMPLIANCE_SECTION);
    expect(footerComplianceLink.link).toMatch(/^\/compliance\//);
  });
});
