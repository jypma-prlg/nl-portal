#!/usr/bin/env node
// Prints one version's CHANGELOG.md section body to stdout. Used by
// .github/workflows/build-and-publish-release.yml as the GitHub Release notes (see ADR 0004) -
// exits non-zero with a clear error instead of silently publishing an empty release.
import { readFileSync } from "node:fs";
import { extractSection } from "./lib/changelog.mjs";

const version = process.argv[2];
if (!version) {
  console.error("Usage: node scripts/changelog-section.mjs <version>");
  process.exit(1);
}

const changelog = readFileSync("CHANGELOG.md", "utf8");
const section = extractSection(changelog, version);
if (section === null) {
  console.error(`No "## [${version}]" section found in CHANGELOG.md.`);
  process.exit(1);
}

process.stdout.write(section);
