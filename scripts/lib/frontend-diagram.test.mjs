import { test } from "node:test";
import assert from "node:assert/strict";
import {
  importedPackages,
  buildEdges,
  classifyEdges,
  compareBaseline,
  renderFrontendDiagram,
} from "./frontend-diagram.mjs";

test("importedPackages finds workspace packages in import, export-from and dynamic import statements", () => {
  const source = `
    import { a } from "@nl-portal/nl-portal-api";
    import "@nl-portal/nl-portal-user-interface/index.css";
    export * from "@nl-portal/nl-portal-localization";
    const lazy = () => import("@nl-portal/nl-portal-authentication");
    import React from "react";
  `;

  assert.deepEqual(
    [...importedPackages(source)].sort(),
    [
      "@nl-portal/nl-portal-api",
      "@nl-portal/nl-portal-authentication",
      "@nl-portal/nl-portal-localization",
      "@nl-portal/nl-portal-user-interface",
    ],
  );
});

test("buildEdges draws one edge per package pair with the import count, and ignores self-imports", () => {
  const packages = [
    { name: "@nl-portal/ui", files: ['import a from "@nl-portal/api"; import b from "@nl-portal/api"; import c from "@nl-portal/ui";'] },
    { name: "@nl-portal/api", files: [] },
  ];

  assert.deepEqual(buildEdges(packages), [{ from: "@nl-portal/ui", to: "@nl-portal/api", imports: 2 }]);
});

test("classifyEdges marks every edge as a gap, and an edge that is part of a cycle as a cycle", () => {
  const edges = [
    { from: "a", to: "b", imports: 1 },
    { from: "b", to: "a", imports: 1 },
    { from: "c", to: "a", imports: 1 },
  ];

  assert.deepEqual(
    classifyEdges(edges).map((edge) => [edge.from, edge.to, edge.coverage]),
    [
      ["a", "b", "cycle"],
      ["b", "a", "cycle"],
      ["c", "a", "gap"],
    ],
  );
});

test("compareBaseline fails on a package without an entry and on an entry for an unknown package", () => {
  const failures = compareBaseline(["a", "b"], { a: { omvang: "S", risico: "laag" }, oud: { omvang: "S", risico: "laag" } });

  assert.equal(failures.length, 2);
  assert.match(failures.join("\n"), /b.*ontbreekt/);
  assert.match(failures.join("\n"), /oud.*bestaat niet/);
});

test("renderFrontendDiagram draws packages with size and risk, edges by coverage, and the rules", () => {
  const puml = renderFrontendDiagram({
    packages: [
      { name: "@nl-portal/api", label: "api" },
      { name: "@nl-portal/ui", label: "user-interface" },
    ],
    edges: classifyEdges([{ from: "@nl-portal/ui", to: "@nl-portal/api", imports: 3 }]),
    baseline: { "@nl-portal/ui": { omvang: "M", risico: "middel" } },
    rules: [
      { name: "package-boundaries", description: "alleen via de publieke API importeren", active: true },
      { name: "import/no-cycle", description: "geen cyclische importen", active: false },
    ],
  });

  assert.match(puml, /\[api\] as api\b/);
  assert.match(puml, /\[user-interface\\nomvang M, risico middel\] as user_interface #FBE3A6/);
  assert.match(puml, /user_interface \.\.> api #orange : gat \(3\)/);
  assert.match(puml, /package-boundaries: alleen via de publieke API importeren \(actief\)/);
  assert.match(puml, /import\/no-cycle: geen cyclische importen \(voorgesteld, niet actief\)/);
});
