import http from "k6/http";
import { check } from "k6";
import type { Options } from "k6/options";
import { fetchToken } from "../lib/auth.ts";
import { loadConfig } from "../lib/config.ts";
import { buildGraphqlRequest, graphqlErrors } from "../lib/graphql.ts";
import { smokeThresholds } from "../lib/thresholds.ts";

// Smoke: 1 gebruiker, 1 iteratie. Bewijst dat tokenflow en GraphQL-endpoint werken en dat de
// thresholds kloppen, voordat zwaardere profielen draaien.
export const options: Options = {
  vus: 1,
  iterations: 1,
  thresholds: smokeThresholds,
};

const config = loadConfig(__ENV);

export default function () {
  const token = fetchToken(config);
  const request = buildGraphqlRequest("{ __typename }", token);
  const response = http.post(config.graphqlUrl, request.body, {
    headers: request.headers,
    tags: { name: "graphql_typename" },
  });
  check(response, {
    "GraphQL antwoordt 200": (r) => r.status === 200,
    "GraphQL-antwoord zonder fouten": (r) => graphqlErrors(String(r.body)).length === 0,
  });
}
