#!/usr/bin/env node
// `mise run release:cut-rc` - see docs/adr/0004-changelog-release-proces.md.
//
// Computes the next SemVer version from the Conventional Commits merged since the previous
// release tag, updates version.txt and CHANGELOG.md, and commits both on a new rc/<version>
// branch cut from the line you currently have checked out: "main" for a regular release, or a
// "release/<major>.x" line for a hotfix (falling back to the local branch if there's no "origin"
// remote, which is only the case in tests/scratch checkouts). Does not push - that stays a
// deliberate, separate step for the releaser.

import { execFileSync } from "node:child_process";
import { writeFileSync } from "node:fs";
import {
  parseConventionalCommit,
  nextBump,
  applyBump,
  categorize,
  renderSection,
  insertSection,
} from "./lib/changelog.mjs";

const RECORD_SEP = "\x1e";
const FIELD_SEP = "\x1f";

function git(args) {
  return execFileSync("git", args, { encoding: "utf8" }).trim();
}

function tryGit(args) {
  try {
    return git(args);
  } catch {
    return null;
  }
}

function assertCleanWorkingTree() {
  const status = git(["status", "--porcelain"]);
  if (status) {
    throw new Error(
      "Working tree is not clean - commit, stash, or discard changes before cutting a release candidate.",
    );
  }
}

const RELEASE_LINE = /^release\/\d+\.x$/;

function currentBranch() {
  return git(["rev-parse", "--abbrev-ref", "HEAD"]);
}

function resolveBaseRef() {
  const branch = currentBranch();
  if (branch !== "main" && !RELEASE_LINE.test(branch)) {
    throw new Error(
      `release:cut-rc must be run from "main" or a "release/<major>.x" line, not "${branch}".`,
    );
  }
  tryGit(["fetch", "origin", branch, "--quiet"]);
  return tryGit(["rev-parse", "--verify", `origin/${branch}`]) ? `origin/${branch}` : branch;
}

function compareSemver(a, b) {
  const pa = a.split(".").map(Number);
  const pb = b.split(".").map(Number);
  for (let i = 0; i < 3; i++) {
    if (pa[i] !== pb[i]) return pa[i] - pb[i];
  }
  return 0;
}

function resolveLatestTag(baseRef) {
  // --merged scopes this to the line being released: a release/<major>.x hotfix must bump from
  // that line's own last tag, not the globally highest tag (which may only exist on main).
  const tags = (tryGit(["tag", "--merged", baseRef]) ?? "")
    .split("\n")
    .map((t) => t.trim())
    .filter((t) => /^\d+\.\d+\.\d+$/.test(t));
  if (tags.length === 0) return null;
  return tags.sort(compareSemver).at(-1);
}

function parseGitLog(raw) {
  return raw
    .split(RECORD_SEP)
    .map((record) => record.trim())
    .filter(Boolean)
    .map((record) => {
      const [hash, subject, body] = record.split(FIELD_SEP);
      return { hash: hash.slice(0, 7), subject, body: body ?? "" };
    });
}

function commitsSince(latestTag, baseRef) {
  const range = latestTag ? `${latestTag}..${baseRef}` : baseRef;
  const raw = git(["log", range, `--pretty=tformat:%H${FIELD_SEP}%s${FIELD_SEP}%b${RECORD_SEP}`]);
  if (!raw) return [];
  return parseGitLog(raw)
    .map(({ hash, subject, body }) => parseConventionalCommit({ hash, subject, body }))
    .filter(Boolean);
}

function readFileAtRef(ref, path) {
  return tryGit(["show", `${ref}:${path}`]);
}

function main() {
  assertCleanWorkingTree();
  const baseRef = resolveBaseRef();
  const latestTag = resolveLatestTag(baseRef);
  const commits = commitsSince(latestTag, baseRef);

  const bump = nextBump(commits);
  if (bump === "none") {
    console.log(
      `No feat/fix/refactor/perf/breaking commits since ${latestTag ?? "the start of history"} on ${baseRef} - nothing to release.`,
    );
    process.exitCode = 1;
    return;
  }

  const currentVersion = readFileAtRef(baseRef, "version.txt")?.trim();
  if (!currentVersion) {
    throw new Error(`Could not read version.txt from ${baseRef}.`);
  }
  const nextVersion = applyBump(currentVersion, bump);

  const date = new Date().toISOString().slice(0, 10);
  const section = renderSection(nextVersion, date, categorize(commits));
  const existingChangelog = readFileAtRef(baseRef, "CHANGELOG.md");
  const newChangelog = insertSection(existingChangelog, section);

  const branch = `rc/${nextVersion}`;
  git(["switch", "-c", branch, baseRef]);

  writeFileSync("version.txt", `${nextVersion}\n`);
  writeFileSync("CHANGELOG.md", newChangelog);

  git(["add", "version.txt", "CHANGELOG.md"]);
  git(["commit", "-m", `chore(release): ${nextVersion}`]);

  console.log(`Cut ${branch} (${currentVersion} -> ${nextVersion}, bump: ${bump}).`);
  console.log("");
  console.log(section.trimEnd());
  console.log("");
  console.log(`Review the above, then: git push -u origin ${branch}`);
  console.log("and dispatch build-and-publish-release-candidate.yml from that branch.");
}

main();
