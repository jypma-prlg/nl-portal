import { test } from "node:test";
import assert from "node:assert/strict";
import { buildGraphqlRequest, graphqlErrors } from "./graphql.ts";

test("bouwt een JSON-request met bearer-token", () => {
  const request = buildGraphqlRequest("{ __typename }", "tok", { a: 1 });

  assert.deepEqual(JSON.parse(request.body), { query: "{ __typename }", variables: { a: 1 } });
  assert.equal(request.headers["Authorization"], "Bearer tok");
  assert.equal(request.headers["Content-Type"], "application/json");
});

test("een geldig antwoord heeft geen fouten", () => {
  assert.deepEqual(graphqlErrors('{"data":{"__typename":"Query"}}'), []);
});

test("GraphQL-fouten bij HTTP 200 tellen als fout", () => {
  assert.deepEqual(graphqlErrors('{"errors":[{"message":"kapot"}],"data":null}'), ["kapot"]);
});

test("een antwoord zonder data of fouten telt als fout", () => {
  assert.equal(graphqlErrors("{}").length, 1);
});

test("een antwoord dat geen JSON is telt als fout", () => {
  assert.equal(graphqlErrors("<html>502</html>").length, 1);
});
