#!/usr/bin/env node
// Generates site/architectuur/img/plantuml/frontend.puml: the frontend packages, the imports between them
// and what the lint rules cover, with size/risk from docs/tenancy-baseline-frontend.json.
// Run via `mise run arch:diagram`; the test in generate-frontend-diagram.test.mjs fails when the file is stale.
// Usage: node scripts/generate-frontend-diagram.mjs [--root <repo-root>]

import { existsSync, readdirSync, readFileSync, writeFileSync } from "node:fs";
import { join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { buildEdges, classifyEdges, compareBaseline, renderFrontendDiagram } from "./lib/frontend-diagram.mjs";

const SOURCE_FILE = /\.(?:[cm]?[jt]sx?)$/;
const SKIPPED_DIRS = new Set(["node_modules", "dist", "build", "generated"]);
export const OUTPUT = "site/architectuur/img/plantuml/frontend.puml";

function sourceFiles(dir) {
  return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const path = join(dir, entry.name);
    if (entry.isDirectory()) return SKIPPED_DIRS.has(entry.name) ? [] : sourceFiles(path);
    return SOURCE_FILE.test(entry.name) ? [readFileSync(path, "utf8")] : [];
  });
}

function workspacePackages(root) {
  const packagesDir = join(root, "frontend", "packages");
  return readdirSync(packagesDir, { withFileTypes: true })
    .filter((entry) => entry.isDirectory() && existsSync(join(packagesDir, entry.name, "package.json")))
    .map((entry) => {
      const dir = join(packagesDir, entry.name);
      const name = JSON.parse(readFileSync(join(dir, "package.json"), "utf8")).name;
      const src = join(dir, "src");
      return { name, label: entry.name, files: existsSync(src) ? sourceFiles(src) : [] };
    })
    .filter((pkg) => typeof pkg.name === "string");
}

function lintRules(root) {
  const config = readFileSync(join(root, "frontend", "eslint.config.mjs"), "utf8");
  const boundaries = readFileSync(join(root, "frontend", "eslint-rules", "package-boundaries.mjs"), "utf8");
  return [
    {
      name: "package-boundaries",
      description: "een package importeert een ander alleen via zijn publieke API, nooit via een intern pad",
      active: config.includes("packageBoundariesConfig") && boundaries.includes("import/no-internal-modules"),
    },
    {
      name: "import/no-cycle",
      description: "geen cyclische imports tussen packages",
      active: [config, boundaries].some((text) => text.includes("import/no-cycle")),
    },
  ];
}

/** The diagram text and the baseline failures for the repository at [root]. */
export function generate(root) {
  const packages = workspacePackages(root);
  const baseline = JSON.parse(readFileSync(join(root, "docs", "tenancy-baseline-frontend.json"), "utf8")).packages;
  const puml = renderFrontendDiagram({
    packages: packages.map(({ name, label }) => ({ name, label })),
    edges: classifyEdges(buildEdges(packages)),
    baseline,
    rules: lintRules(root),
  });
  return { puml, failures: compareBaseline(packages.map((pkg) => pkg.name), baseline) };
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const rootArg = process.argv.indexOf("--root");
  const root = rootArg === -1 ? resolve(fileURLToPath(import.meta.url), "..", "..") : resolve(process.argv[rootArg + 1]);
  const { puml, failures } = generate(root);
  if (failures.length > 0) {
    console.error(failures.join("\n"));
    process.exit(1);
  }
  writeFileSync(join(root, OUTPUT), puml);
  console.log(`Diagram geschreven naar ${OUTPUT}`);
}
