import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import { cpSync, mkdirSync, mkdtempSync, readFileSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join, resolve } from "node:path";
import { test } from "node:test";
import { fileURLToPath } from "node:url";
import {
  APP_DEV_OUTPUT,
  COMPOSE_ENV,
  COMPOSE_ENV_EXAMPLE,
  SECRET_NAMES,
  TEMPLATES,
  devSetup,
  generateSecrets,
  render,
  withManagedBlock,
} from "./dev-setup.mjs";

const repoRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const git = (...args) => execFileSync("git", args, { cwd: repoRoot, encoding: "utf8" });

// Voorheen ingecheckte waarden voor Keycloak en :app; mogen nergens in tracked bestanden terugkomen.
const LEGACY_SECRETS = ["ookVRUAxmEWMcosfcGR5nxeoUC4Rgwbc", "lR2hhFRHraNwIbNfhlNivVIMdJkTqq6s"];

function scratchRoot() {
  const root = mkdtempSync(join(tmpdir(), "dev-setup-"));
  for (const path of [...TEMPLATES.map((t) => t.template), COMPOSE_ENV_EXAMPLE]) {
    mkdirSync(dirname(join(root, path)), { recursive: true });
    cpSync(join(repoRoot, path), join(root, path));
  }
  return root;
}

const fixed = (n) => Object.fromEntries(SECRET_NAMES.map((name) => [name, `${name}-${n}`]));

test("generateSecrets geeft per naam een sterk, uniek geheim", () => {
  const a = generateSecrets();
  const b = generateSecrets();
  for (const name of SECRET_NAMES) {
    assert.match(a[name], /^[A-Za-z0-9_-]{32}$/);
    assert.notEqual(a[name], b[name]);
  }
  assert.equal(new Set(Object.values(a)).size, SECRET_NAMES.length);
});

test("render vervangt placeholders en weigert onbekende", () => {
  assert.equal(render("a={{X}};b={{X}}", { X: "1" }), "a=1;b=1");
  assert.throws(() => render("{{ONBEKEND}}", {}, "t"), /t: onbekende placeholder \{\{ONBEKEND\}\}/);
  assert.equal(render("${client_realm-management}", {}), "${client_realm-management}");
});

test("withManagedBlock behoudt eigen regels en vervangt het blok bij herhaling", () => {
  const eerste = withManagedBlock("RUN_MODE=local\n", fixed(1));
  const tweede = withManagedBlock(eerste, fixed(2));
  assert.match(tweede, /^RUN_MODE=local\n/);
  assert.equal(tweede.match(/KEYCLOAK_DB_PASSWORD=/g).length, 1);
  assert.match(tweede, /KEYCLOAK_DB_PASSWORD=KEYCLOAK_DB_PASSWORD-2/);
});

test("devSetup rendert dezelfde waarden in realm, compose en :app", () => {
  const root = scratchRoot();
  try {
    const secrets = generateSecrets();
    devSetup(root, secrets);
    const read = (p) => readFileSync(join(root, p), "utf8");

    const realm = JSON.parse(read("docker-compose/.generated/keycloak/nlportal-realm.json"));
    const m2m = realm.clients.find((c) => c.clientId === "nl-portal-m2m");
    assert.equal(m2m.secret, secrets.KEYCLOAK_M2M_CLIENT_SECRET);
    assert.match(
      read("backend/app/.env.properties"),
      new RegExp(`^KEYCLOAK_CLIENT_SECRET=${secrets.KEYCLOAK_M2M_CLIENT_SECRET}$`, "m"),
    );
    assert.match(read(APP_DEV_OUTPUT), new RegExp(`KEYCLOAK_CLIENT_SECRET=${secrets.KEYCLOAK_M2M_CLIENT_SECRET}`));
    for (const name of SECRET_NAMES) assert.match(read(COMPOSE_ENV), new RegExp(`^${name}=${secrets[name]}$`, "m"));
    assert.ok(read("docker-compose/.generated/keycloak/master-realm.json").includes(secrets.KEYCLOAK_ADMIN_PASSWORD));
    for (const { output } of TEMPLATES) assert.doesNotMatch(read(output), /\{\{[A-Z_]+\}\}/);

    // Opnieuw uitvoeren geeft een verse omgeving.
    const again = generateSecrets();
    devSetup(root, again);
    assert.ok(!read(COMPOSE_ENV).includes(secrets.KEYCLOAK_DB_PASSWORD));
    assert.ok(read(COMPOSE_ENV).includes(again.KEYCLOAK_DB_PASSWORD));
  } finally {
    rmSync(root, { recursive: true, force: true });
  }
});

test("rooktest: gegenereerde bestanden zijn gitignored", () => {
  const outputs = [...TEMPLATES.map((t) => t.output), APP_DEV_OUTPUT, COMPOSE_ENV];
  const ignored = git("check-ignore", ...outputs).split(/\r?\n/).filter(Boolean);
  assert.deepEqual([...ignored].sort(), [...outputs].sort());
});

test("rooktest: geen vaste secrets in tracked bestanden", () => {
  const tracked = git("ls-files").split(/\r?\n/).filter(Boolean);
  for (const { output } of TEMPLATES) assert.ok(!tracked.includes(output), `${output} is ingecheckt`);
  for (const path of tracked.filter((p) => p.startsWith("docker-compose/imports/keycloak/"))) {
    assert.fail(`${path}: realms horen als template onder docker-compose/templates/keycloak/`);
  }
  for (const path of tracked) {
    let text;
    try {
      text = readFileSync(join(repoRoot, path), "utf8");
    } catch {
      continue;
    }
    for (const secret of LEGACY_SECRETS) assert.ok(!text.includes(secret), `${path} bevat een vast Keycloak-secret`);
  }
  for (const { template } of TEMPLATES.filter((t) => t.template.endsWith(".json"))) {
    assert.doesNotMatch(readFileSync(join(repoRoot, template), "utf8"), /"privateKey"/, `${template} bevat sleutelmateriaal`);
  }
});
