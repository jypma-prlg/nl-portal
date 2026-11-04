import { test } from "node:test";
import assert from "node:assert/strict";
import { execFileSync, spawnSync } from "node:child_process";
import { mkdtempSync, mkdirSync, readFileSync, writeFileSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { fileURLToPath } from "node:url";

const SCRIPT = fileURLToPath(new URL("./generate-capabilities-docs.mjs", import.meta.url));

const YAML = `# commentaar
version: 1
generated: 2026-11-04
capabilities:
  - id: zaken-inzien
    naam: Zaken inzien
    beschrijving: Burger ziet zijn zaken.
    status: actief
    bewijs: getZaken gebruikt.
    modules: [backend/zgw/zaken, backend/zgw/zaken-api]
    signalen: {in_assembly: true, tests: true, laatste_commit: 2026-11-04, commits_12m: 17, deprecated_markers: false}
    ingangen:
      - {type: graphql-query, naam: getZaken, frontend_gebruikt: true}
      - {type: rest, naam: GET /api/zakenapi/zaakdocument/{zaakDocumentId}/content, frontend_gebruikt: true}

  - id: eigen-zaken-case
    naam: Eigen zaken | case
    beschrijving: Portaal-interne zaakafhandeling.
    status: deprecated
    bewijs: Niet gebruikt.
    besluit: site/development/adr/0010-verwijderen-case-module.md
    modules: [backend/case]
    signalen: {in_assembly: true, tests: true, laatste_commit: 2026-10-20, commits_12m: 5, deprecated_markers: false}
    ingangen:
      - {type: graphql-query, naam: allCaseDefinitions, frontend_gebruikt: false}
      - {type: event-listener, naam: CaseListener (ApplicationReadyEvent), frontend_gebruikt: false, status: kandidaat-verwijderen, voorwaarde: eerst checken}

modules_zonder_ingang:
  - {module: backend/graphql, rol: library (GraphQL-config, scalars, exceptionhandler)}

technische_schuld:
  - {id: clamav-virusscan-api, module: backend/zgw/documenten-api, omschrijving: "deprecated: ClamAV/VirusScan-API"}
`;

const ARC42_5 = `# 5. Bouwblokken

handgeschreven voor

<!-- BEGIN GENERATED: capabilities -->
oud
<!-- END GENERATED: capabilities -->

handgeschreven na
`;
const ARC42_11 = `# 11. Risico's

<!-- BEGIN GENERATED: technische-schuld -->
<!-- END GENERATED: technische-schuld -->
`;

function withRoot(yaml, run) {
  const root = mkdtempSync(join(tmpdir(), "capabilities-docs-"));
  try {
    mkdirSync(join(root, "docs"), { recursive: true });
    mkdirSync(join(root, "site/architectuur/arc42"), { recursive: true });
    mkdirSync(join(root, "site/likec4"), { recursive: true });
    writeFileSync(join(root, "docs/capabilities.yaml"), yaml);
    writeFileSync(join(root, "site/architectuur/arc42/05-bouwblokkenzicht.md"), ARC42_5);
    writeFileSync(join(root, "site/architectuur/arc42/11-risicos-en-technische-schulden.md"), ARC42_11);
    run(root);
  } finally {
    rmSync(root, { recursive: true, force: true });
  }
}

const generate = (root) => execFileSync("node", [SCRIPT, "--root", root], { encoding: "utf8" });
const read = (root, rel) => readFileSync(join(root, rel), "utf8");

test("running the generator twice yields identical output", () => {
  withRoot(YAML, (root) => {
    generate(root);
    const files = [
      "site/architectuur/arc42/05-bouwblokkenzicht.md",
      "site/architectuur/arc42/11-risicos-en-technische-schulden.md",
      "site/likec4/capabilities.generated.c4",
    ];
    const first = files.map((f) => read(root, f));
    generate(root);
    assert.deepEqual(files.map((f) => read(root, f)), first);
  });
});

test("chapter 5 lists each capability with status, modules and entry points, keeping hand-written text", () => {
  withRoot(YAML, (root) => {
    generate(root);
    const md = read(root, "site/architectuur/arc42/05-bouwblokkenzicht.md");
    assert.match(md, /handgeschreven voor/);
    assert.match(md, /handgeschreven na/);
    assert.doesNotMatch(md, /\noud\n/);
    assert.match(md, /Zaken inzien/);
    assert.match(md, /`backend\/zgw\/zaken-api`/);
    assert.match(md, /`getZaken`/);
    assert.match(md, /`GET \/api\/zakenapi\/zaakdocument\/\{zaakDocumentId\}\/content`/);
    assert.match(md, /Eigen zaken \\\| case/, "pipes in cells are escaped");
    assert.match(md, /backend\/graphql/, "modules without entry point are listed");
  });
});

test("chapter 11 lists technical debt and at-risk capabilities and entry points, not healthy ones", () => {
  withRoot(YAML, (root) => {
    generate(root);
    const md = read(root, "site/architectuur/arc42/11-risicos-en-technische-schulden.md");
    assert.match(md, /clamav-virusscan-api/);
    assert.match(md, /eigen-zaken-case/);
    assert.match(md, /site\/development\/adr\/0010-verwijderen-case-module\.md/);
    assert.match(md, /CaseListener/, "entry-point level kandidaat-verwijderen is listed");
    assert.match(md, /eerst checken/);
    assert.doesNotMatch(md, /zaken-inzien/);
  });
});

test("LikeC4 file has capability elements tagged by status and related to components", () => {
  withRoot(YAML, (root) => {
    generate(root);
    const c4 = read(root, "site/likec4/capabilities.generated.c4");
    assert.match(c4, /capability zakenInzien 'Zaken inzien'/);
    assert.match(c4, /#actief/);
    assert.match(c4, /#deprecated/);
    assert.match(c4, /zakenInzien -> nlPortal\.backend\.zgwIntegrations\.zaken /);
    assert.match(c4, /zakenInzien -> nlPortal\.backend\.zgwIntegrations\.zakenApi /);
    assert.match(c4, /eigenZakenCase -> nlPortal\.backend\.caseModule /);
  });
});

test("changing a status in the YAML changes the generated output", () => {
  withRoot(YAML, (root) => {
    generate(root);
    const before = read(root, "site/likec4/capabilities.generated.c4");
    writeFileSync(join(root, "docs/capabilities.yaml"), YAML.replace("status: actief", "status: twijfel"));
    generate(root);
    const after = read(root, "site/likec4/capabilities.generated.c4");
    assert.notEqual(after, before);
    assert.match(read(root, "site/architectuur/arc42/11-risicos-en-technische-schulden.md"), /zaken-inzien/);
  });
});

test("an unknown status is rejected", () => {
  withRoot(YAML.replace("status: actief", "status: blijkbaar"), (root) => {
    const r = spawnSync("node", [SCRIPT, "--root", root], { encoding: "utf8" });
    assert.notEqual(r.status, 0);
    assert.match(r.stderr, /blijkbaar/);
  });
});

test("a missing generated-region marker is an error, not silent", () => {
  withRoot(YAML, (root) => {
    writeFileSync(join(root, "site/architectuur/arc42/05-bouwblokkenzicht.md"), "# geen markers\n");
    const r = spawnSync("node", [SCRIPT, "--root", root], { encoding: "utf8" });
    assert.notEqual(r.status, 0);
    assert.match(r.stderr, /BEGIN GENERATED: capabilities/);
  });
});
