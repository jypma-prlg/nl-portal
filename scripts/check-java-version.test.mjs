import { test } from "node:test";
import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { checkJavaVersion } from "./check-java-version.mjs";

const SCRIPT = fileURLToPath(new URL("./check-java-version.mjs", import.meta.url));

const CONSISTENT = {
  "backend/gradle.properties": "kotlinVersion=2.4.21\njavaVersion=25\n",
  "mise.toml": '[tools]\npnpm = "11"\njava = "temurin-25"\n',
  "backend/Dockerfile":
    "FROM eclipse-temurin:25-jdk-alpine AS build-local\nFROM eclipse-temurin:25-jre-alpine AS image\n",
  "backend/README.md":
    "![Java 25](https://img.shields.io/badge/Java-25-green)\n\n## Requirements\n\n- Java 25\n- Docker\n",
  ".github/workflows/ci.yml": "jobs:\n  a:\n    steps:\n      - uses: ./.github/actions/setup-backend-jdk\n",
};

function withRepo(files, run) {
  const root = mkdtempSync(join(tmpdir(), "check-java-version-"));
  try {
    for (const [path, content] of Object.entries(files)) {
      mkdirSync(dirname(join(root, path)), { recursive: true });
      writeFileSync(join(root, path), content);
    }
    return run(root);
  } finally {
    rmSync(root, { recursive: true, force: true });
  }
}

test("reports no mismatches when every place uses the Gradle javaVersion", () => {
  const result = withRepo(CONSISTENT, checkJavaVersion);
  assert.equal(result.version, "25");
  assert.deepEqual(result.mismatches, []);
});

test("flags mise.toml on another Java version", () => {
  const files = { ...CONSISTENT, "mise.toml": '[tools]\njava = "temurin-21"\n' };
  const { mismatches } = withRepo(files, checkJavaVersion);
  assert.equal(mismatches.length, 1);
  assert.match(mismatches[0], /mise\.toml.*21.*25/);
});

test("flags every Dockerfile image that is on another Java version", () => {
  const files = {
    ...CONSISTENT,
    "backend/Dockerfile": "FROM eclipse-temurin:21-jdk-alpine\nFROM eclipse-temurin:25-jre-alpine\nFROM eclipse-temurin:21-jre-alpine\n",
  };
  const { mismatches } = withRepo(files, checkJavaVersion);
  assert.equal(mismatches.length, 2);
  assert.ok(mismatches.every((m) => m.includes("backend/Dockerfile")));
});

test("flags the README badge and requirement line", () => {
  const files = { ...CONSISTENT, "backend/README.md": "![Java 21](https://img.shields.io/badge/Java-21-green)\n\n- Java 21\n" };
  const { mismatches } = withRepo(files, checkJavaVersion);
  assert.equal(mismatches.length, 2);
  assert.ok(mismatches.every((m) => m.includes("backend/README.md")));
});

test("flags a workflow that hardcodes a Java version", () => {
  const files = {
    ...CONSISTENT,
    ".github/workflows/sonar.yml": "with:\n  distribution: temurin\n  java-version: 21\n",
    ".github/workflows/stage.yml": 'inputs:\n  javaVersion:\n    default: "21"\n',
  };
  const { mismatches } = withRepo(files, checkJavaVersion);
  assert.equal(mismatches.length, 2);
  assert.ok(mismatches.every((m) => m.includes(".github/workflows/")));
});

test("flags even a hardcoded workflow version equal to javaVersion", () => {
  const files = { ...CONSISTENT, ".github/workflows/sonar.yml": "java-version: 25\n" };
  const { mismatches } = withRepo(files, checkJavaVersion);
  assert.equal(mismatches.length, 1);
});

test("fails loudly when javaVersion is missing from gradle.properties", () => {
  const files = { ...CONSISTENT, "backend/gradle.properties": "kotlinVersion=2.4.21\n" };
  assert.throws(() => withRepo(files, checkJavaVersion), /javaVersion/);
});

test("CLI exits 0 when consistent and 1 with the mismatches listed", () => {
  withRepo(CONSISTENT, (root) => {
    execFileSync("node", [SCRIPT, "--root", root], { stdio: "pipe" });
  });
  const files = { ...CONSISTENT, "mise.toml": 'java = "temurin-21"\n' };
  withRepo(files, (root) => {
    assert.throws(
      () => execFileSync("node", [SCRIPT, "--root", root], { stdio: "pipe" }),
      (err) => err.status === 1 && /mise\.toml/.test(String(err.stderr)),
    );
  });
});
