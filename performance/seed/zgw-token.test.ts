import { test } from "node:test";
import assert from "node:assert/strict";
import { createHmac } from "node:crypto";
import { zgwToken } from "./zgw-token.ts";

const decode = (deel: string) => JSON.parse(Buffer.from(deel, "base64url").toString());

test("het token is een HS256-JWT met client_id en uitgiftetijd", () => {
  const [header, payload, handtekening] = zgwToken("nl-portal", "geheim", 1700000000).split(".");
  assert.deepEqual(decode(header!), { alg: "HS256", typ: "JWT" });
  assert.deepEqual(decode(payload!), {
    iss: "nl-portal",
    iat: 1700000000,
    client_id: "nl-portal",
    user_id: "nl-portal-perf-seed",
    user_representation: "nl-portal-perf-seed",
  });
  const verwacht = createHmac("sha256", "geheim").update(`${header}.${payload}`).digest("base64url");
  assert.equal(handtekening, verwacht);
});
