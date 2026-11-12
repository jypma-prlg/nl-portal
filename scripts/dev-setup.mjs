#!/usr/bin/env node
// Genereert de lokale dev-omgeving met willekeurige secrets (mise run dev:setup).
// Rendert dezelfde waarden uit templates in de Keycloak-realms, de compose-omgeving (docker-compose/.env)
// en de lokale omgeving van :app (backend/app/.env.properties). Alles wat dit script schrijft is gitignored.
// Opnieuw uitvoeren geeft nieuwe secrets; draai daarna `docker compose down -v` en `up`, zodat de
// services opnieuw starten met de nieuwe realms en fixtures en met lege databases.
// Usage: node scripts/dev-setup.mjs [--root <repo-root>]

import { pbkdf2Sync, randomBytes } from "node:crypto";
import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

export const SECRET_NAMES = [
  "KEYCLOAK_M2M_CLIENT_SECRET",
  "KEYCLOAK_ADMIN_PASSWORD",
  "KEYCLOAK_DB_PASSWORD",
  // Open Zaak: JWT-secrets van de ZGW-clients en de koppeling met Open Notificaties.
  "OPENZAAK_NLPORTAL_SECRET",
  "OPENZAAK_VALTIMO_CLIENT_SECRET",
  "OPENZAAK_OPENNOTIFICATIES_SECRET",
  "OPENZAAK_OPENFORMULIEREN_SECRET",
  "NOTIFICATIES_OBJECTS_SECRET",
  "NOTIFICATIES_OPENZAAK_SECRET",
  // API-tokens.
  "OBJECTS_API_TOKEN",
  "OPENKLANT_API_TOKEN",
  "OPENPRODUCT_API_TOKEN",
  // Databasewachtwoorden, Django SECRET_KEYs, beheerderswachtwoorden en de RabbitMQ-broker.
  "OPENZAAK_DB_PASSWORD",
  "OPENKLANT_DB_PASSWORD",
  "OBJECTS_DB_PASSWORD",
  "NOTIFICATIES_DB_PASSWORD",
  "OPENPRODUCT_DB_PASSWORD",
  "OPENZAAK_SECRET_KEY",
  "OPENKLANT_SECRET_KEY",
  "OBJECTS_SECRET_KEY",
  "NOTIFICATIES_SECRET_KEY",
  "OPENPRODUCT_SECRET_KEY",
  "OPENZAAK_ADMIN_PASSWORD",
  "OPENKLANT_ADMIN_PASSWORD",
  "OBJECTS_ADMIN_PASSWORD",
  "NOTIFICATIES_ADMIN_PASSWORD",
  "OPENPRODUCT_ADMIN_PASSWORD",
  "NOTIFICATIES_RABBITMQ_PASSWORD",
  "BACKEND_BEHEER_PASSWORD",
];

// Template -> uitvoer. Alle uitvoerpaden moeten gitignored zijn; de rooktest (dev-setup.test.mjs) bewaakt dat.
export const TEMPLATES = [
  {
    template: "docker-compose/templates/keycloak/nlportal-realm.json",
    output: "docker-compose/.generated/keycloak/nlportal-realm.json",
  },
  {
    template: "docker-compose/templates/keycloak/master-realm.json",
    output: "docker-compose/.generated/keycloak/master-realm.json",
  },
  { template: "docker-compose/templates/openzaak/configuration.json", output: "docker-compose/.generated/openzaak/configuration.json" },
  { template: "docker-compose/templates/open-object/configuration.json", output: "docker-compose/.generated/open-object/configuration.json" },
  { template: "docker-compose/templates/open-notificaties/configuration.json", output: "docker-compose/.generated/open-notificaties/configuration.json" },
  { template: "docker-compose/templates/openklant-2/configuration.json", output: "docker-compose/.generated/openklant-2/configuration.json" },
  { template: "docker-compose/templates/openklant-2/admin_user.json", output: "docker-compose/.generated/openklant-2/admin_user.json" },
  { template: "docker-compose/templates/openproduct/configuration.json", output: "docker-compose/.generated/openproduct/configuration.json" },
  { template: "backend/app/.env.properties.example", output: "backend/app/.env.properties" },
];

// :app-dev leest zijn secrets uit een eigen, gitignored bestand: het client-secret van de m2m-client
// (als KEYCLOAK_CLIENT_SECRET) en de secrets en tokens waarmee het de ZGW-componenten aanroept.
export const APP_DEV_SECRETS = ["OPENZAAK_NLPORTAL_SECRET", "OBJECTS_API_TOKEN", "OPENKLANT_API_TOKEN", "OPENPRODUCT_API_TOKEN"];
export const APP_DEV_OUTPUT = "backend/app-dev/.env.properties";
export const COMPOSE_ENV = "docker-compose/.env";
export const COMPOSE_ENV_EXAMPLE = "docker-compose/.env.example";

const BLOCK_START = "# --- dev:setup: gegenereerd, niet handmatig wijzigen ---";
const BLOCK_END = "# --- einde dev:setup ---";

export function generateSecrets(bytes = randomBytes) {
  return Object.fromEntries(SECRET_NAMES.map((name) => [name, bytes(24).toString("base64url")]));
}

// Django slaat wachtwoorden op als pbkdf2_sha256$<iteraties>$<salt>$<base64-hash>; Open Klant heeft geen
// superuser-hook, dus de beheerder komt als gerenderde fixture met deze hash.
export function djangoPasswordHash(password, salt = randomBytes(12).toString("hex"), iterations = 1_000_000) {
  const hash = pbkdf2Sync(password, salt, iterations, 32, "sha256").toString("base64");
  return `pbkdf2_sha256$${iterations}$${salt}$${hash}`;
}

export function render(template, values, source = "template") {
  return template.replace(/\{\{([A-Z0-9_]+)\}\}/g, (_, name) => {
    if (!(name in values)) throw new Error(`${source}: onbekende placeholder {{${name}}}`);
    return values[name];
  });
}

export function withManagedBlock(existing, values) {
  const block = [BLOCK_START, ...SECRET_NAMES.map((name) => `${name}=${values[name]}`), BLOCK_END].join("\n");
  const pattern = new RegExp(`${BLOCK_START}[\\s\\S]*?${BLOCK_END}`);
  if (pattern.test(existing)) return existing.replace(pattern, () => block);
  return `${existing.replace(/\s*$/, "")}\n\n${block}\n`;
}

function write(root, path, content) {
  const target = join(root, path);
  mkdirSync(dirname(target), { recursive: true });
  writeFileSync(target, content);
}

export function devSetup(root, secrets = generateSecrets()) {
  const written = [];
  const values = { ...secrets, OPENKLANT_ADMIN_PASSWORD_HASH: djangoPasswordHash(secrets.OPENKLANT_ADMIN_PASSWORD) };
  for (const { template, output } of TEMPLATES) {
    write(root, output, render(readFileSync(join(root, template), "utf8"), values, template));
    written.push(output);
  }

  // Eigen regels in een bestaande docker-compose/.env (RUN_MODE, NETWERK, ...) blijven staan.
  const base = existsSync(join(root, COMPOSE_ENV)) ? COMPOSE_ENV : COMPOSE_ENV_EXAMPLE;
  write(root, COMPOSE_ENV, withManagedBlock(readFileSync(join(root, base), "utf8"), secrets));
  written.push(COMPOSE_ENV);

  write(
    root,
    APP_DEV_OUTPUT,
    [
      "# Gegenereerd door mise run dev:setup (gitignored).",
      `KEYCLOAK_CLIENT_SECRET=${secrets.KEYCLOAK_M2M_CLIENT_SECRET}`,
      ...APP_DEV_SECRETS.map((name) => `${name}=${secrets[name]}`),
      "",
    ].join("\n"),
  );
  written.push(APP_DEV_OUTPUT);
  return written;
}

function main() {
  const rootArg = process.argv.indexOf("--root");
  const root = resolve(rootArg > -1 ? process.argv[rootArg + 1] : join(dirname(fileURLToPath(import.meta.url)), ".."));
  const written = devSetup(root);
  console.log("Dev-omgeving gegenereerd met nieuwe willekeurige secrets (alles gitignored):");
  for (const path of written) console.log(`  ${path}`);
  console.log("\nDe beheerderswachtwoorden (Keycloak, Open Zaak, Open Klant, ...) staan in docker-compose/.env (*_ADMIN_PASSWORD).");
  console.log("Draaide de stack al? Start hem opnieuw met lege databases:");
  console.log("  cd docker-compose && docker compose down -v && docker compose up -d");
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) main();
