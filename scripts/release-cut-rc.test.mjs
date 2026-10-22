// Integration test for `mise run release:cut-rc` - runs the real script against a scratch git
// repo (never the nl-portal checkout itself), per the spec's "scratch-checkout" test plan item.
import { test } from "node:test";
import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import { mkdtempSync, readFileSync, writeFileSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import { parseConventionalCommit } from "./lib/changelog.mjs";

const SCRIPT = fileURLToPath(new URL("./release-cut-rc.mjs", import.meta.url));

function git(cwd, args) {
  return execFileSync("git", args, { cwd, encoding: "utf8" }).trim();
}

function commit(cwd, file, content, message) {
  writeFileSync(join(cwd, file), content);
  git(cwd, ["add", file]);
  git(cwd, ["commit", "-m", message]);
}

function initRepo() {
  const dir = mkdtempSync(join(tmpdir(), "release-cut-rc-"));
  git(dir, ["init", "--quiet", "-b", "main"]);
  git(dir, ["config", "user.email", "test@example.com"]);
  git(dir, ["config", "user.name", "Test"]);
  // The dev machine's global git config may require GPG-signed commits/tags; this scratch repo
  // has no GPG key available, so disable signing locally (the real repo's own config is untouched).
  git(dir, ["config", "commit.gpgsign", "false"]);
  git(dir, ["config", "tag.gpgsign", "false"]);
  commit(dir, "version.txt", "1.0.0\n", "chore: init");
  git(dir, ["tag", "1.0.0"]);
  return dir;
}

function runScript(cwd) {
  return execFileSync(process.execPath, [SCRIPT], { cwd, encoding: "utf8" });
}

test("release:cut-rc cuts an rc branch with the right version and changelog", () => {
  const dir = initRepo();
  try {
    commit(dir, "a.txt", "a", "feat(x): add thing");
    commit(dir, "b.txt", "b", "fix(y): fix thing");
    commit(dir, "c.txt", "c", "docs: write doc");

    const output = runScript(dir);
    assert.match(output, /Cut rc\/1\.1\.0/);

    assert.equal(git(dir, ["rev-parse", "--abbrev-ref", "HEAD"]), "rc/1.1.0");
    assert.equal(readFileSync(join(dir, "version.txt"), "utf8"), "1.1.0\n");

    const changelog = readFileSync(join(dir, "CHANGELOG.md"), "utf8");
    assert.match(changelog, /## \[1\.1\.0\]/);
    assert.match(changelog, /add thing/);
    assert.match(changelog, /fix thing/);
    assert.doesNotMatch(changelog, /write doc/);

    assert.equal(git(dir, ["status", "--porcelain"]), "");

    // "commitlint erop loslaten" (spec, Test-en-uitrolplan #2): verify the generated commit
    // subject is itself a valid Conventional Commit, using the same parser the rest of the
    // pipeline trusts (frontend/commitlint.config.js just extends the same standard rules).
    const subject = git(dir, ["log", "-1", "--pretty=%s"]);
    const parsed = parseConventionalCommit({ hash: "x", subject, body: "" });
    assert.ok(parsed, `commit subject is not a valid Conventional Commit: "${subject}"`);
    assert.equal(parsed.type, "chore");
    assert.equal(parsed.breaking, false);
  } finally {
    rmSync(dir, { recursive: true, force: true });
  }
});

test("release:cut-rc refuses when there is nothing user-facing to release", () => {
  const dir = initRepo();
  try {
    commit(dir, "a.txt", "a", "docs: write doc");
    commit(dir, "b.txt", "b", "chore: housekeeping");

    assert.throws(() => runScript(dir), /Command failed/);
    assert.equal(git(dir, ["rev-parse", "--abbrev-ref", "HEAD"]), "main");
    assert.equal(readFileSync(join(dir, "version.txt"), "utf8"), "1.0.0\n");
  } finally {
    rmSync(dir, { recursive: true, force: true });
  }
});

test("release:cut-rc refuses on a dirty working tree", () => {
  const dir = initRepo();
  try {
    commit(dir, "a.txt", "a", "feat(x): add thing");
    writeFileSync(join(dir, "untracked.txt"), "oops");

    assert.throws(() => runScript(dir), /Command failed/);
    assert.equal(git(dir, ["rev-parse", "--abbrev-ref", "HEAD"]), "main");
  } finally {
    rmSync(dir, { recursive: true, force: true });
  }
});

test("release:cut-rc refuses on a branch that is neither main nor a release line", () => {
  const dir = initRepo();
  try {
    git(dir, ["switch", "-c", "feature/x"]);
    commit(dir, "a.txt", "a", "feat(x): add thing");

    assert.throws(() => runScript(dir), /Command failed/);
    assert.equal(git(dir, ["rev-parse", "--abbrev-ref", "HEAD"]), "feature/x");
  } finally {
    rmSync(dir, { recursive: true, force: true });
  }
});

test("release:cut-rc on a release/<major>.x line bumps from that line's own last tag, not main's", () => {
  const dir = initRepo();
  try {
    // release/1.x forks off at 1.0.0, independent from here on.
    git(dir, ["switch", "-c", "release/1.x"]);

    // main races ahead to a 2.0.0 that release/1.x never sees.
    git(dir, ["switch", "main"]);
    writeFileSync(join(dir, "version.txt"), "2.0.0\n");
    git(dir, ["add", "version.txt"]);
    git(dir, ["commit", "-m", "feat(x)!: rework thing"]);
    git(dir, ["tag", "2.0.0"]);

    // The hotfix itself, authored on the maintenance line.
    git(dir, ["switch", "release/1.x"]);
    commit(dir, "hotfix.txt", "h", "fix(auth): patch the thing");

    const output = runScript(dir);
    assert.match(output, /Cut rc\/1\.0\.1/);
    assert.match(output, /1\.0\.0 -> 1\.0\.1/);

    assert.equal(git(dir, ["rev-parse", "--abbrev-ref", "HEAD"]), "rc/1.0.1");
    assert.equal(readFileSync(join(dir, "version.txt"), "utf8"), "1.0.1\n");

    const changelog = readFileSync(join(dir, "CHANGELOG.md"), "utf8");
    assert.match(changelog, /## \[1\.0\.1\]/);
    assert.match(changelog, /patch the thing/);
    assert.doesNotMatch(changelog, /rework thing/);
  } finally {
    rmSync(dir, { recursive: true, force: true });
  }
});
