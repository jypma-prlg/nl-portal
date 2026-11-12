import { test } from "node:test";
import assert from "node:assert/strict";
import { laadConfig } from "./seed.ts";

test("zonder opgave geldt de lokale compose-stack met een vast volume", () => {
  const config = laadConfig({});
  assert.deepEqual(config.plan, { gebruikers: 10, zakenPerGebruiker: 5, takenPerGebruiker: 2, documentenPerZaak: 2 });
  assert.equal(config.keycloak.url, "http://localhost:8082/auth");
  assert.equal(config.openzaak.catalogi, "http://localhost:8001/catalogi/api/v1");
});

test("het volume en de URL's zijn via de omgeving te overschrijven", () => {
  const config = laadConfig({ PERF_SEED_GEBRUIKERS: "50", PERF_OPENZAAK_URL: "http://zaak:8001/", PERF_SEED_PASSWORD: "x" });
  assert.equal(config.plan.gebruikers, 50);
  assert.equal(config.openzaak.url, "http://zaak:8001");
  assert.equal(config.wachtwoord, "x");
});

test("het seed-wachtwoord valt terug op PERF_PASSWORD, zodat perf:smoke en seed op elkaar aansluiten", () => {
  assert.equal(laadConfig({ PERF_PASSWORD: "gedeeld" }).wachtwoord, "gedeeld");
});
