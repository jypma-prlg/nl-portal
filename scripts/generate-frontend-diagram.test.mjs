import { test } from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { generate, OUTPUT } from "./generate-frontend-diagram.mjs";

const root = resolve(fileURLToPath(import.meta.url), "..", "..");

test("the frontend baseline matches the packages in the code", () => {
  assert.deepEqual(generate(root).failures, []);
});

test("the committed frontend diagram is up to date; run `mise run arch:diagram` and commit the result", () => {
  const committed = readFileSync(join(root, OUTPUT), "utf8").replace(/\r\n/g, "\n");

  assert.equal(committed, generate(root).puml);
});
