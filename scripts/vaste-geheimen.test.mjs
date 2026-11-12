import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import { readFileSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { test } from "node:test";
import { fileURLToPath } from "node:url";
import { vasteGeheimen } from "./vaste-geheimen.mjs";

const repoRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const git = (...args) => execFileSync("git", args, { cwd: repoRoot, encoding: "utf8" });

test("vasteGeheimen laat verwijzingen, lege waarden en getallen toe", () => {
  const toegestaan = [
    "- DB_PASSWORD=${DB_PASSWORD:?draai eerst mise run dev:setup}",
    "SECRET_KEY: ${X}",
    '"secret": "{{X}}",',
    '"header_value": "Token {{T}}"',
    "NLPORTAL_BEHEER_WACHTWOORDHASH={noop}${BEHEER}",
    "# PASSWORD=commentaar",
    '"token_auth": 1,',
    '"secret": "",',
    "- BROKER=amqp://gebruiker:${PW}@host:5672//",
  ];
  for (const regel of toegestaan) assert.deepEqual(vasteGeheimen(regel), [], regel);
});

test("vasteGeheimen herkent vaste waarden, ook als standaardwaarde, zonder de waarde te melden", () => {
  const vast = [
    "- DB_PASSWORD=iets",
    "- SECRET_KEY=${SECRET_KEY:-iets}",
    '"token": "iets",',
    "  secret: iets",
    "NLPORTAL_BEHEER_WACHTWOORDHASH={noop}iets",
    "- BROKER=amqp://gebruiker:iets@host:5672//",
  ];
  for (const regel of vast) {
    const meldingen = vasteGeheimen(regel);
    assert.equal(meldingen.length, 1, regel);
    assert.ok(!meldingen[0].includes("iets"), "de melding bevat de waarde niet");
  }
});

test("rooktest: geen vaste wachtwoorden, secrets of tokens in compose-, import- en app-dev-bestanden", () => {
  const bestanden = git("ls-files", "docker-compose", "backend/app", "backend/app-dev")
    .split(/\r?\n/)
    .filter((p) => /\.(ya?ml|env|json|example|properties)$/.test(p))
    // De Keycloak-templates bevatten de wachtwoordhash van de vaste testgebruikers (zie #151).
    .filter((p) => !p.startsWith("docker-compose/templates/keycloak/"))
    .filter((p) => !p.includes("/src/test/"));
  assert.ok(bestanden.length > 10, "te weinig bestanden gevonden om iets te bewaken");
  const fouten = bestanden.flatMap((p) => vasteGeheimen(readFileSync(join(repoRoot, p), "utf8")).map((f) => `${p} ${f}`));
  assert.deepEqual(fouten, []);
});
