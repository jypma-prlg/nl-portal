#!/usr/bin/env node
// Guard: site/compliance/normatieve-bronnen.md is het register van normatieve bronnen (ASVS, RFC's, ...) met een
// traceerbaarheidsmatrix bron -> eis -> guardrail of check -> bewijs.
// Faalt op (1) een register-ID dat ergens in site/, backend/ (.kt) of .github/workflows genoemd wordt maar niet
// bestaat, (2) een matrixrij met onbekend bron-ID, onbekende status of ontbrekende velden, (3) een bron zonder
// enige check of afwijkingsbesluit, (4) een bron zonder versie, datum of wijze van raadplegen.
// Run via `mise run compliance:check`; CI draait het ook.
// Usage: node scripts/check-compliance.mjs [--root <repo-root>]

import { readFileSync, readdirSync } from "node:fs";
import { join, relative, resolve } from "node:path";
import { fileURLToPath } from "node:url";

export const REGISTER_PATH = "site/compliance/normatieve-bronnen.md";

// Een ID heeft minstens twee segmenten (N-<BRON>-<...>), zodat woorden als N-SNAPSHOT geen ID zijn; het eindigt nooit op een punt.
const ID_PATTERN = /\bN-[A-Z][A-Z0-9]*(?:-[A-Z0-9]+(?:\.[0-9]+)*)+/g;
const SKIP_DIRS = new Set(["node_modules", "build", "dist", "cache", ".git", ".gradle", ".vitepress"]);

const COVERING = new Set(["Afgedwongen", "Getoetst"]);
const STATUSES = new Set([...COVERING, "Bewust niet toegepast", "Open"]);

const isEmpty = (cell) => cell === undefined || cell.trim() === "" || cell.trim() === "-";
const stripTicks = (cell) => cell.trim().replace(/^`|`$/g, "");

function tableRows(markdown, heading) {
  const section = markdown.split(/^## /m).find((part) => part.startsWith(heading));
  if (!section) return [];
  return section
    .split(/\r?\n/)
    .filter((line) => line.trim().startsWith("|"))
    .map((line) =>
      line
        .trim()
        .replace(/^\||\|$/g, "")
        .split("|")
        .map((cell) => cell.trim()),
    )
    .filter((cells) => !cells.every((cell) => /^:?-+:?$/.test(cell)))
    .slice(1); // koprij
}

function* walk(dir, extensions) {
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    if (entry.isDirectory()) {
      if (!SKIP_DIRS.has(entry.name)) yield* walk(join(dir, entry.name), extensions);
    } else if (extensions.some((ext) => entry.name.endsWith(ext))) {
      yield join(dir, entry.name);
    }
  }
}

function scanTargets(root) {
  const targets = [];
  for (const [dir, extensions] of [
    ["site", [".md"]],
    ["backend", [".kt"]],
    [".github/workflows", [".yml", ".yaml"]],
  ]) {
    try {
      targets.push(...walk(join(root, dir), extensions));
    } catch (error) {
      if (error.code !== "ENOENT") throw error;
    }
  }
  return targets;
}

export function checkCompliance(root) {
  let markdown;
  try {
    markdown = readFileSync(join(root, REGISTER_PATH), "utf8");
  } catch (error) {
    if (error.code === "ENOENT") throw new Error(`${REGISTER_PATH} ontbreekt`);
    throw error;
  }
  const problems = [];

  const sources = new Map();
  for (const [rawId, , version, consulted, how] of tableRows(markdown, "Register")) {
    const id = stripTicks(rawId);
    if (!/^N-[A-Z][A-Z0-9]*(?:-[A-Z0-9]+(?:\.[0-9]+)*)+$/.test(id)) {
      problems.push(`${REGISTER_PATH}: "${id}" is geen geldig bron-ID`);
      continue;
    }
    if (sources.has(id)) problems.push(`${REGISTER_PATH}: bron-ID ${id} komt meer dan eens voor`);
    sources.set(id, { covered: false });
    for (const [value, label] of [
      [version, "versie"],
      [consulted, "geraadpleegd op"],
      [how, "wijze van raadplegen"],
    ]) {
      if (isEmpty(value)) problems.push(`${REGISTER_PATH}: bron ${id} heeft geen ${label}`);
    }
  }

  for (const [rawId, eis, status, guardrail, evidence, decision] of tableRows(markdown, "Traceerbaarheidsmatrix")) {
    const id = stripTicks(rawId);
    const where = `${REGISTER_PATH}: matrixrij ${id} / ${eis}`;
    const source = sources.get(id);
    if (!source) {
      problems.push(`${where} verwijst naar een onbekend bron-ID`);
      continue;
    }
    if (!STATUSES.has(status)) {
      problems.push(`${where} heeft onbekende status "${status}" (toegestaan: ${[...STATUSES].join(", ")})`);
      continue;
    }
    if (COVERING.has(status)) {
      if (isEmpty(guardrail) || isEmpty(evidence)) {
        problems.push(`${where} is ${status} maar noemt geen guardrail of check en bewijs`);
        continue;
      }
      source.covered = true;
    } else if (status === "Bewust niet toegepast") {
      if (isEmpty(decision)) {
        problems.push(`${where} is bewust niet toegepast zonder besluit`);
        continue;
      }
      source.covered = true;
    } else if (isEmpty(decision)) {
      problems.push(`${where} is Open zonder verwijzing naar een issue`);
    }
  }

  for (const [id, { covered }] of sources) {
    if (!covered) problems.push(`${REGISTER_PATH}: bron ${id} heeft geen check of afwijkingsbesluit in de matrix`);
  }

  const registerFile = join(root, REGISTER_PATH);
  for (const file of scanTargets(root)) {
    if (file === registerFile) continue;
    const found = new Set(readFileSync(file, "utf8").match(ID_PATTERN) ?? []);
    for (const id of found) {
      if (!sources.has(id)) {
        problems.push(`${relative(root, file).replaceAll("\\", "/")}: noemt ${id}, dat niet in het register staat`);
      }
    }
  }

  return { sources: [...sources.keys()], problems };
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const rootIndex = process.argv.indexOf("--root");
  const root = rootIndex >= 0 ? resolve(process.argv[rootIndex + 1]) : resolve(fileURLToPath(new URL("..", import.meta.url)));
  try {
    const { sources, problems } = checkCompliance(root);
    if (problems.length > 0) {
      console.error(`Compliance-register klopt niet:\n- ${problems.join("\n- ")}`);
      process.exit(1);
    }
    console.log(`Compliance-register consistent: ${sources.length} bron(nen)`);
  } catch (error) {
    console.error(error.message);
    process.exit(1);
  }
}
