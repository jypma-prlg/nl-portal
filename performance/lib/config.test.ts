import { test } from "node:test";
import assert from "node:assert/strict";
import { loadConfig } from "./config.ts";

const CREDENTIALS = { PERF_USERNAME: "gebruiker", PERF_PASSWORD: "wachtwoord" };

test("gebruikt de lokale compose-stack als er geen URL's zijn opgegeven", () => {
  const config = loadConfig(CREDENTIALS);

  assert.equal(config.graphqlUrl, "http://localhost:8080/graphql");
  assert.equal(config.tokenUrl, "http://localhost:8082/auth/realms/nlportal/protocol/openid-connect/token");
  assert.equal(config.clientId, "nl-portal");
});

test("laat de URL's en de client overschrijven via de omgeving", () => {
  const config = loadConfig({
    ...CREDENTIALS,
    PERF_BACKEND_URL: "http://backend:8080/",
    PERF_KEYCLOAK_URL: "http://keycloak:8082/auth/",
    PERF_REALM: "ander",
    PERF_CLIENT_ID: "perf",
  });

  assert.equal(config.graphqlUrl, "http://backend:8080/graphql");
  assert.equal(config.tokenUrl, "http://keycloak:8082/auth/realms/ander/protocol/openid-connect/token");
  assert.equal(config.clientId, "perf");
});

test("faalt zonder gebruikersnaam of wachtwoord, en noemt de ontbrekende variabele", () => {
  assert.throws(() => loadConfig({ PERF_USERNAME: "gebruiker" }), /PERF_PASSWORD/);
  assert.throws(() => loadConfig({ PERF_PASSWORD: "wachtwoord" }), /PERF_USERNAME/);
});
