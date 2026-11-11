import http from "k6/http";
import { check, fail } from "k6";
import type { PerfConfig } from "./config.ts";

/** Haalt een access token op via de direct access grant (password flow) van Keycloak. */
export function fetchToken(config: PerfConfig): string {
  const response = http.post(
    config.tokenUrl,
    { grant_type: "password", client_id: config.clientId, username: config.username, password: config.password },
    { tags: { name: "keycloak_token" } },
  );
  const ok = check(response, { "token opgehaald (200)": (r) => r.status === 200 });
  if (!ok) {
    fail(`Tokenaanvraag mislukt: HTTP ${response.status}`);
  }
  return response.json("access_token") as string;
}
