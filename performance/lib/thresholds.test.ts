import { test } from "node:test";
import assert from "node:assert/strict";
import { smokeThresholds } from "./thresholds.ts";

test("de smoke-thresholds laten een falend verzoek of een falende check de run laten falen", () => {
  assert.ok(smokeThresholds.http_req_failed?.includes("rate==0"));
  assert.ok(smokeThresholds.checks?.includes("rate==1"));
});

test("de smoke-thresholds begrenzen de latency met een ruime marge", () => {
  assert.ok(smokeThresholds.http_req_duration?.some((drempel) => drempel.startsWith("p(95)<")));
});
