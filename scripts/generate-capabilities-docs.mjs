#!/usr/bin/env node
// Renders docs/capabilities.yaml (single source of truth) into:
//   - arc42 chapter 5  (generated region "capabilities")
//   - arc42 chapter 11 (generated region "technische-schuld")
//   - site/likec4/capabilities.generated.c4
// Run via `mise run docs:capabilities`. CI re-runs it and fails on any resulting diff.
// Usage: node scripts/generate-capabilities-docs.mjs [--root <repo-root>]

import { readFileSync, writeFileSync } from "node:fs";
import { join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { parseCapabilitiesYaml } from "./lib/capabilities-yaml.mjs";

const STATUSES = ["in-ontwikkeling", "actief", "twijfel", "kandidaat-verwijderen", "deprecated", "verwijderd"];
const AT_RISK = ["twijfel", "kandidaat-verwijderen", "deprecated"];

// Gradle module -> element in site/likec4/components.c4. Modules not listed relate to the backend container.
const BACKEND = "nlPortal.backend";
const ZGW = `${BACKEND}.zgwIntegrations`;
const MODULE_ELEMENTS = {
  "backend/core": `${BACKEND}.core`,
  "backend/graphql": `${BACKEND}.graphqlApi`,
  "backend/form": `${BACKEND}.form`,
  "backend/payment-direct": `${BACKEND}.payment`,
  "backend/product": `${BACKEND}.product`,
  "backend/portal-authentication": `${BACKEND}.portalAuthentication`,
  "backend/haalcentraal2": `${BACKEND}.haalCentraalIntegrations.haalcentraal2`,
  "backend/haalcentraal-hr": `${BACKEND}.haalCentraalIntegrations.haalcentraalHr`,
  "backend/zgw/idtoken-authentication": `${ZGW}.idtokenAuthentication`,
  "backend/zgw/common-ground-authentication": `${ZGW}.commonGroundAuthentication`,
  "backend/zgw/objectenapi": `${ZGW}.objectenapi`,
  "backend/zgw/catalogi-api": `${ZGW}.catalogiApi`,
  "backend/zgw/openklant": `${ZGW}.openklant`,
  "backend/zgw/taak": `${ZGW}.taak`,
  "backend/zgw/documenten-api": `${ZGW}.documentenApi`,
  "backend/zgw/berichten": `${ZGW}.berichten`,
  "backend/zgw/besluiten": `${ZGW}.besluiten`,
  "backend/zgw/zaken-api": `${ZGW}.zakenApi`,
  "backend/zgw/openproduct": `${ZGW}.openproduct`,
  "backend/zgw/verificatie": `${ZGW}.verificatie`,
  "backend/zgw/zaken": `${ZGW}.zaken`,
};

const GENERATED_NOTE =
  "_Gegenereerd uit [`docs/capabilities.yaml`](https://github.com/pinkroccade/nl-portal/blob/main/docs/capabilities.yaml) via `mise run docs:capabilities`. " +
  "Wijzig de YAML, niet dit blok; CI faalt als dit blok niet actueel is._";

const cell = (s) => String(s ?? "").replace(/\|/g, "\\|").replace(/\r?\n/g, " ");
const code = (s) => `\`${cell(s)}\``;
const yesNo = (b) => (b ? "ja" : "nee");

function validate(inv) {
  for (const key of ["capabilities", "modules_zonder_ingang", "technische_schuld"]) {
    if (!Array.isArray(inv[key])) throw new Error(`docs/capabilities.yaml: '${key}' ontbreekt of is geen lijst`);
  }
  for (const c of inv.capabilities) {
    if (!STATUSES.includes(c.status)) {
      throw new Error(`Capability '${c.id}': onbekende status '${c.status}' (verwacht: ${STATUSES.join(" | ")})`);
    }
    for (const i of c.ingangen ?? []) {
      if (i.status !== undefined && !STATUSES.includes(i.status)) {
        throw new Error(`Ingang '${i.naam}' van '${c.id}': onbekende status '${i.status}'`);
      }
    }
  }
}

function renderBuildingBlocks(inv) {
  const out = [GENERATED_NOTE, "", `## 5.5 Capabilities per module (inventaris ${inv.generated})`, ""];
  out.push(
    "Functioneel overzicht van de backend: wat het portaal doet, in welke Gradle-modules dat leeft en via welke",
    "ingangen (GraphQL-operaties, REST-endpoints, event-listeners) het bereikbaar is. Statussen:",
    "`in-ontwikkeling`, `actief`, `twijfel`, `kandidaat-verwijderen`, `deprecated` (met ADR), `verwijderd`.",
    "",
    "| Capability | Status | Modules | Ingangen |",
    "|---|---|---|---|",
  );
  for (const c of inv.capabilities) {
    out.push(
      `| [${cell(c.naam)}](#capability-${c.id}) | ${code(c.status)} | ${c.modules.map(code).join(", ")} | ${(c.ingangen ?? []).length} |`,
    );
  }
  for (const c of inv.capabilities) {
    out.push("", `<a id="capability-${c.id}"></a>`, `### ${cell(c.naam)} (\`${c.id}\`)`, "");
    out.push(c.beschrijving, "");
    out.push(`- **Status:** ${code(c.status)}`);
    out.push(`- **Bewijs:** ${c.bewijs}`);
    if (c.besluit) out.push(`- **Besluit:** ${c.besluit}`);
    if (c.actie) out.push(`- **Actie:** ${c.actie}`);
    out.push(`- **Modules:** ${c.modules.map(code).join(", ")}`);
    const ingangen = c.ingangen ?? [];
    if (ingangen.length > 0) {
      out.push("", "| Type | Ingang | Frontend gebruikt | Status | Voorwaarde |", "|---|---|---|---|---|");
      for (const i of ingangen) {
        out.push(
          `| ${cell(i.type)} | ${code(i.naam)} | ${yesNo(i.frontend_gebruikt)} | ${code(i.status ?? c.status)} | ${cell(i.voorwaarde ?? "")} |`,
        );
      }
    }
  }
  out.push("", "### Modules zonder eigen ingang", "", "| Module | Rol |", "|---|---|");
  for (const m of inv.modules_zonder_ingang) out.push(`| ${code(m.module)} | ${cell(m.rol)} |`);
  return out.join("\n");
}

function renderDebt(inv) {
  const out = [GENERATED_NOTE, "", `## 11.4 Technische schuld en capabilities onder twijfel (inventaris ${inv.generated})`, ""];
  out.push("### Technische schuld", "", "| Id | Module | Omschrijving |", "|---|---|---|");
  for (const d of inv.technische_schuld) out.push(`| ${code(d.id)} | ${code(d.module)} | ${cell(d.omschrijving)} |`);
  if (inv.technische_schuld.length === 0) out.push("| geen | | |");

  out.push(
    "",
    "### Capabilities met status twijfel, kandidaat-verwijderen of deprecated",
    "",
    "| Capability | Status | Toelichting | Besluit / actie |",
    "|---|---|---|---|",
  );
  for (const c of inv.capabilities.filter((c) => AT_RISK.includes(c.status))) {
    out.push(
      `| ${code(c.id)} | ${code(c.status)} | ${cell(c.bewijs)} | ${cell([c.besluit, c.actie].filter(Boolean).join(" — "))} |`,
    );
  }

  out.push("", "### Ingangen met een eigen risicostatus", "", "| Capability | Ingang | Status | Voorwaarde |", "|---|---|---|---|");
  for (const c of inv.capabilities) {
    for (const i of c.ingangen ?? []) {
      if (i.status && AT_RISK.includes(i.status)) {
        out.push(`| ${code(c.id)} | ${code(i.naam)} | ${code(i.status)} | ${cell(i.voorwaarde ?? "")} |`);
      }
    }
  }
  return out.join("\n");
}

const c4Id = (id) => id.replace(/-([a-z0-9])/g, (_, ch) => ch.toUpperCase());
const c4Str = (s) => `'${String(s).replace(/\\/g, "\\\\").replace(/'/g, "\\'")}'`;
const c4Tag = (status) => status.replace(/-/g, "_");

function renderLikeC4(inv) {
  const out = [
    "// GEGENEREERD uit docs/capabilities.yaml via `mise run docs:capabilities`. Niet handmatig bewerken.",
    "specification {",
    "  element capability",
    ...STATUSES.map((s) => `  tag ${c4Tag(s)}`),
    "}",
    "",
    "model {",
  ];
  for (const c of inv.capabilities) {
    out.push(
      `  capability ${c4Id(c.id)} ${c4Str(c.naam)} {`,
      `    #${c4Tag(c.status)}`,
      `    description ${c4Str(c.beschrijving)}`,
      "    metadata {",
      `      status ${c4Str(c.status)}`,
      `      modules ${c4Str(c.modules.join(", "))}`,
      `      ingangen ${c4Str(String((c.ingangen ?? []).length))}`,
      "    }",
      "  }",
    );
  }
  out.push("");
  for (const c of inv.capabilities) {
    for (const m of c.modules) {
      if (!MODULE_ELEMENTS[m] && c.status !== "verwijderd") console.warn(`Waarschuwing: module '${m}' (${c.id}) staat niet in MODULE_ELEMENTS; gerelateerd aan ${BACKEND}.`);
    }
    const targets = [...new Set(c.modules.map((m) => MODULE_ELEMENTS[m] ?? BACKEND))];
    for (const t of targets) out.push(`  ${c4Id(c.id)} -> ${t} 'Geïmplementeerd in'`);
  }
  out.push("}", "");
  return out.join("\n");
}

function replaceRegion(file, name, body) {
  const begin = `<!-- BEGIN GENERATED: ${name} -->`;
  const end = `<!-- END GENERATED: ${name} -->`;
  const text = readFileSync(file, "utf8");
  const b = text.indexOf(begin);
  const e = text.indexOf(end);
  if (b === -1 || e === -1 || e < b) throw new Error(`${file}: markers '${begin}' / '${end}' ontbreken`);
  writeFileSync(file, `${text.slice(0, b + begin.length)}\n${body}\n${text.slice(e)}`);
}

export function generate(root) {
  const inv = parseCapabilitiesYaml(readFileSync(join(root, "docs/capabilities.yaml"), "utf8"));
  validate(inv);
  replaceRegion(join(root, "site/architectuur/arc42/05-bouwblokkenzicht.md"), "capabilities", renderBuildingBlocks(inv));
  replaceRegion(join(root, "site/architectuur/arc42/11-risicos-en-technische-schulden.md"), "technische-schuld", renderDebt(inv));
  writeFileSync(join(root, "site/likec4/capabilities.generated.c4"), renderLikeC4(inv));
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const i = process.argv.indexOf("--root");
  const root = i === -1 ? resolve(fileURLToPath(import.meta.url), "../..") : resolve(process.argv[i + 1]);
  try {
    generate(root);
  } catch (err) {
    console.error(err.message);
    process.exit(1);
  }
}
