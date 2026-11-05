#!/usr/bin/env node
// Guard: javaVersion in backend/gradle.properties is the single source for the backend's Java version.
// Fails when mise.toml, the backend Dockerfile images or the backend README name another version, or when a
// workflow hardcodes a Java version instead of using the .github/actions/setup-backend-jdk action.
// Run via `mise run java:check-version`; CI runs it too.
// Usage: node scripts/check-java-version.mjs [--root <repo-root>]

import { readFileSync, readdirSync } from "node:fs";
import { join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const read = (root, path) => readFileSync(join(root, path), "utf8");

function lines(text) {
  return text.split(/\r?\n/).map((content, i) => ({ number: i + 1, content }));
}

export function checkJavaVersion(root) {
  const version = /^javaVersion=(\d+)\s*$/m.exec(read(root, "backend/gradle.properties"))?.[1];
  if (!version) {
    throw new Error("javaVersion ontbreekt in backend/gradle.properties");
  }
  const mismatches = [];
  const expect = (path, number, found, what) => {
    if (found !== version) {
      mismatches.push(`${path}:${number} ${what} is ${found}, javaVersion in backend/gradle.properties is ${version}`);
    }
  };

  const mise = "mise.toml";
  for (const { number, content } of lines(read(root, mise))) {
    const found = /^\s*java\s*=\s*"temurin-(\d+)/.exec(content)?.[1];
    if (found) expect(mise, number, found, "java");
  }

  const dockerfile = "backend/Dockerfile";
  for (const { number, content } of lines(read(root, dockerfile))) {
    const found = /eclipse-temurin:(\d+)[-\s]/.exec(content)?.[1];
    if (found) expect(dockerfile, number, found, "eclipse-temurin image");
  }

  const readme = "backend/README.md";
  for (const { number, content } of lines(read(root, readme))) {
    const badge = /img\.shields\.io\/badge\/Java-(\d+)-/.exec(content)?.[1];
    if (badge) expect(readme, number, badge, "Java-badge");
    const requirement = /^\s*-\s*Java\s+(\d+)\s*$/.exec(content)?.[1];
    if (requirement) expect(readme, number, requirement, "vereiste Java-versie");
  }

  const workflowDir = ".github/workflows";
  for (const file of readdirSync(join(root, workflowDir)).filter((f) => /\.ya?ml$/.test(f))) {
    for (const { number, content } of lines(read(root, `${workflowDir}/${file}`))) {
      // `javaVersion:` is flagged on its own: an input's default sits on the next line.
      if (/^\s*java-version:\s*["']?\d+/.test(content) || /^\s*javaVersion:/.test(content)) {
        mismatches.push(
          `${workflowDir}/${file}:${number} hardcodeert een Java-versie; gebruik ./.github/actions/setup-backend-jdk`,
        );
      }
    }
  }

  return { version, mismatches };
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const rootIndex = process.argv.indexOf("--root");
  const root = rootIndex >= 0 ? resolve(process.argv[rootIndex + 1]) : resolve(fileURLToPath(new URL("..", import.meta.url)));
  try {
    const { version, mismatches } = checkJavaVersion(root);
    if (mismatches.length > 0) {
      console.error(`Java-versie loopt uiteen (verwacht ${version}):\n- ${mismatches.join("\n- ")}`);
      process.exit(1);
    }
    console.log(`Java-versie consistent: ${version}`);
  } catch (error) {
    console.error(error.message);
    process.exit(1);
  }
}
