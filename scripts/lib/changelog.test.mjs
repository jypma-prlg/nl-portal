import { test } from "node:test";
import assert from "node:assert/strict";
import {
  parseConventionalCommit,
  bumpFor,
  nextBump,
  applyBump,
  categorize,
  renderSection,
  insertSection,
  extractSection,
} from "./changelog.mjs";

test("parseConventionalCommit: with scope", () => {
  const result = parseConventionalCommit({
    hash: "abc1234",
    subject: "feat(auth): add passkey login",
    body: "",
  });
  assert.deepEqual(result, {
    hash: "abc1234",
    type: "feat",
    scope: "auth",
    breaking: false,
    description: "add passkey login",
  });
});

test("parseConventionalCommit: without scope", () => {
  const result = parseConventionalCommit({ hash: "a1", subject: "docs: fix typo", body: "" });
  assert.deepEqual(result, {
    hash: "a1",
    type: "docs",
    scope: null,
    breaking: false,
    description: "fix typo",
  });
});

test("parseConventionalCommit: breaking via ! suffix", () => {
  const result = parseConventionalCommit({
    hash: "a1",
    subject: "feat(api)!: drop legacy field",
    body: "",
  });
  assert.equal(result.breaking, true);
  assert.equal(result.type, "feat");
  assert.equal(result.scope, "api");
});

test("parseConventionalCommit: breaking via BREAKING CHANGE footer", () => {
  const result = parseConventionalCommit({
    hash: "a1",
    subject: "fix(auth): remove old session cookie",
    body: "Some explanation.\n\nBREAKING CHANGE: the legacy cookie is no longer accepted.",
  });
  assert.equal(result.breaking, true);
});

test("parseConventionalCommit: non-matching subject returns null", () => {
  assert.equal(
    parseConventionalCommit({ hash: "a1", subject: "Merge pull request #27 from x/y", body: "" }),
    null,
  );
});

test("bumpFor: breaking always major regardless of type", () => {
  assert.equal(bumpFor({ type: "fix", breaking: true }), "major");
});

test("bumpFor: feat is minor", () => {
  assert.equal(bumpFor({ type: "feat", breaking: false }), "minor");
});

test("bumpFor: fix/refactor/perf are patch", () => {
  assert.equal(bumpFor({ type: "fix", breaking: false }), "patch");
  assert.equal(bumpFor({ type: "refactor", breaking: false }), "patch");
  assert.equal(bumpFor({ type: "perf", breaking: false }), "patch");
});

test("bumpFor: everything else is none", () => {
  for (const type of ["docs", "test", "build", "ci", "chore", "weird"]) {
    assert.equal(bumpFor({ type, breaking: false }), "none");
  }
});

test("nextBump: picks the highest-ranked bump across commits", () => {
  const commits = [
    { type: "docs", breaking: false },
    { type: "fix", breaking: false },
    { type: "feat", breaking: false },
  ];
  assert.equal(nextBump(commits), "minor");
});

test("nextBump: empty array is none", () => {
  assert.equal(nextBump([]), "none");
});

test("nextBump: a single breaking fix outranks a feat", () => {
  const commits = [
    { type: "feat", breaking: false },
    { type: "fix", breaking: true },
  ];
  assert.equal(nextBump(commits), "major");
});

test("applyBump: major/minor/patch", () => {
  assert.equal(applyBump("4.1.0", "major"), "5.0.0");
  assert.equal(applyBump("4.1.0", "minor"), "4.2.0");
  assert.equal(applyBump("4.1.0", "patch"), "4.1.1");
});

test("applyBump: none yields null", () => {
  assert.equal(applyBump("4.1.0", "none"), null);
});

test("categorize: breaking commit goes only into Breaking, not also Fixed", () => {
  const commits = [
    parseConventionalCommit({
      hash: "a1",
      subject: "fix(auth)!: remove old session cookie",
      body: "",
    }),
  ];
  const sections = categorize(commits);
  assert.deepEqual(sections.Breaking, ["**auth**: remove old session cookie (`a1`)"]);
  assert.deepEqual(sections.Fixed, []);
});

test("categorize: feat/fix/refactor/perf map to Added/Fixed/Changed, others excluded", () => {
  const commits = [
    parseConventionalCommit({ hash: "a1", subject: "feat(x): add thing", body: "" }),
    parseConventionalCommit({ hash: "a2", subject: "fix(y): fix thing", body: "" }),
    parseConventionalCommit({ hash: "a3", subject: "refactor(z): tidy thing", body: "" }),
    parseConventionalCommit({ hash: "a4", subject: "perf(z): speed up thing", body: "" }),
    parseConventionalCommit({ hash: "a5", subject: "docs: write thing", body: "" }),
    parseConventionalCommit({ hash: "a6", subject: "chore: bump thing", body: "" }),
  ];
  const sections = categorize(commits);
  assert.deepEqual(sections.Added, ["**x**: add thing (`a1`)"]);
  assert.deepEqual(sections.Fixed, ["**y**: fix thing (`a2`)"]);
  assert.deepEqual(sections.Changed, ["**z**: tidy thing (`a3`)", "**z**: speed up thing (`a4`)"]);
  assert.deepEqual(sections.Breaking, []);
});

test("categorize: entry without scope omits the bold scope prefix", () => {
  const commits = [parseConventionalCommit({ hash: "a1", subject: "feat: add thing", body: "" })];
  const sections = categorize(commits);
  assert.deepEqual(sections.Added, ["add thing (`a1`)"]);
});

test("categorize: falsy hash omits the trailing hash segment", () => {
  const commits = [parseConventionalCommit({ hash: "", subject: "feat: add thing", body: "" })];
  const sections = categorize(commits);
  assert.deepEqual(sections.Added, ["add thing"]);
});

test("renderSection: exact Keep a Changelog block, skipping empty sections", () => {
  const sections = {
    Breaking: ["old behavior removed (`abc1234`)"],
    Added: ["**auth**: add passkey login (`def5678`)"],
    Changed: [],
    Fixed: ["handle empty result (`aaa1111`)"],
  };
  const result = renderSection("1.2.0", "2026-10-22", sections);
  assert.equal(
    result,
    [
      "## [1.2.0] - 2026-10-22",
      "",
      "### ⚠ BREAKING CHANGES",
      "",
      "- old behavior removed (`abc1234`)",
      "",
      "### Added",
      "",
      "- **auth**: add passkey login (`def5678`)",
      "",
      "### Fixed",
      "",
      "- handle empty result (`aaa1111`)",
      "",
    ].join("\n"),
  );
});

test("renderSection: all sections empty still renders just the heading", () => {
  const sections = { Breaking: [], Added: [], Changed: [], Fixed: [] };
  const result = renderSection("1.0.1", "2026-10-22", sections);
  assert.equal(result, "## [1.0.1] - 2026-10-22\n");
});

const HEADER = [
  "# Changelog",
  "",
  "All notable changes to NL Portal are documented in this file.",
  "",
  "The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project",
  "adheres to [Semantic Versioning](https://semver.org/). Sections are generated from Conventional",
  "Commits by `mise run release:cut-rc` (see [ADR 0004](docs/adr/0004-changelog-release-proces.md))",
  "at the moment a release is cut - not maintained incrementally between releases.",
].join("\n");

test("insertSection: empty file starts from the fixed header", () => {
  const section = "## [1.0.0] - 2026-10-22\n\n### Added\n\n- first release (`a1`)\n";
  for (const existing of [null, undefined, ""]) {
    const result = insertSection(existing, section);
    assert.equal(result, `${HEADER}\n\n${section}`);
  }
});

test("insertSection: prepends before the first existing version heading", () => {
  const existing = `${HEADER}\n\n## [1.0.0] - 2026-10-22\n\n### Added\n\n- first release (\`a1\`)\n`;
  const section = "## [1.1.0] - 2026-11-01\n\n### Added\n\n- second release (`b2`)\n";
  const result = insertSection(existing, section);
  assert.equal(
    result,
    `${HEADER}\n\n${section}\n## [1.0.0] - 2026-10-22\n\n### Added\n\n- first release (\`a1\`)\n`,
  );
});

test("extractSection: finds the body of a version in the middle of the file", () => {
  const content = [
    "# Changelog",
    "",
    "## [1.1.0] - 2026-11-01",
    "",
    "### Added",
    "",
    "- second release",
    "",
    "## [1.0.0] - 2026-10-22",
    "",
    "### Added",
    "",
    "- first release",
    "",
  ].join("\n");
  assert.equal(extractSection(content, "1.1.0"), "### Added\n\n- second release\n");
});

test("extractSection: version not found returns null", () => {
  const content = "# Changelog\n\n## [1.0.0] - 2026-10-22\n\n### Added\n\n- first release\n";
  assert.equal(extractSection(content, "9.9.9"), null);
});

test("extractSection: last section in the file (no trailing heading) runs to EOF", () => {
  const content = "# Changelog\n\n## [1.0.0] - 2026-10-22\n\n### Added\n\n- first release\n";
  assert.equal(extractSection(content, "1.0.0"), "### Added\n\n- first release\n");
});
