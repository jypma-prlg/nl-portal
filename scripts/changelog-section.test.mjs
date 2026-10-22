import { test } from "node:test";
import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import { mkdtempSync, writeFileSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { fileURLToPath } from "node:url";

const SCRIPT = fileURLToPath(new URL("./changelog-section.mjs", import.meta.url));

function withTempChangelog(content, run) {
  const dir = mkdtempSync(join(tmpdir(), "changelog-section-"));
  try {
    writeFileSync(join(dir, "CHANGELOG.md"), content);
    run(dir);
  } finally {
    rmSync(dir, { recursive: true, force: true });
  }
}

const CHANGELOG = [
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

test("changelog-section.mjs prints the requested version's body", () => {
  withTempChangelog(CHANGELOG, (dir) => {
    const output = execFileSync(process.execPath, [SCRIPT, "1.0.0"], { cwd: dir, encoding: "utf8" });
    assert.equal(output, "### Added\n\n- first release\n");
  });
});

test("changelog-section.mjs exits non-zero for a missing version", () => {
  withTempChangelog(CHANGELOG, (dir) => {
    assert.throws(
      () => execFileSync(process.execPath, [SCRIPT, "9.9.9"], { cwd: dir, encoding: "utf8" }),
      /Command failed/,
    );
  });
});

test("changelog-section.mjs exits non-zero when no version is given", () => {
  withTempChangelog(CHANGELOG, (dir) => {
    assert.throws(
      () => execFileSync(process.execPath, [SCRIPT], { cwd: dir, encoding: "utf8" }),
      /Command failed/,
    );
  });
});
