#!/usr/bin/env node
// Genereert de lokale dev-omgeving met willekeurige secrets (mise run dev:setup).
// Rendert dezelfde waarden uit templates in de Keycloak-realms, de compose-omgeving (docker-compose/.env)
// en de lokale omgeving van :app (backend/app/.env.properties). Alles wat dit script schrijft is gitignored.
// Opnieuw uitvoeren geeft nieuwe secrets; draai daarna `docker compose down` en `up`, zodat Keycloak
// opnieuw start met de nieuwe realms en een lege database.
// Usage: node scripts/dev-setup.mjs [--root <repo-root>]

import { randomBytes } from "node:crypto";
import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

export const SECRET_NAMES = ["KEYCLOAK_M2M_CLIENT_SECRET", "KEYCLOAK_ADMIN_PASSWORD", "KEYCLOAK_DB_PASSWORD"];

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
  { template: "backend/app/.env.properties.example", output: "backend/app/.env.properties" },
];

// :app-dev leest alleen het client-secret van de m2m-client uit een eigen, gitignored bestand.
export const APP_DEV_OUTPUT = "backend/app-dev/.env.properties";
export const COMPOSE_ENV = "docker-compose/.env";
export const COMPOSE_ENV_EXAMPLE = "docker-compose/.env.example";

const BLOCK_START = "# --- dev:setup: gegenereerd, niet handmatig wijzigen ---";
const BLOCK_END = "# --- einde dev:setup ---";

export function generateSecrets(bytes = randomBytes) {
  return Object.fromEntries(SECRET_NAMES.map((name) => [name, bytes(24).toString("base64url")]));
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
  for (const { template, output } of TEMPLATES) {
    write(root, output, render(readFileSync(join(root, template), "utf8"), secrets, template));
    written.push(output);
  }

  // Eigen regels in een bestaande docker-compose/.env (RUN_MODE, NETWERK, ...) blijven staan.
  const base = existsSync(join(root, COMPOSE_ENV)) ? COMPOSE_ENV : COMPOSE_ENV_EXAMPLE;
  write(root, COMPOSE_ENV, withManagedBlock(readFileSync(join(root, base), "utf8"), secrets));
  written.push(COMPOSE_ENV);

  write(
    root,
    APP_DEV_OUTPUT,
    `# Gegenereerd door mise run dev:setup (gitignored).\nKEYCLOAK_CLIENT_SECRET=${secrets.KEYCLOAK_M2M_CLIENT_SECRET}\n`,
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
  console.log("\nHet Keycloak-adminwachtwoord staat in docker-compose/.env (KEYCLOAK_ADMIN_PASSWORD).");
  console.log("Draaide de stack al? Start hem opnieuw met een lege Keycloak-database:");
  console.log("  cd docker-compose && docker compose down && docker compose up -d");
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) main();
