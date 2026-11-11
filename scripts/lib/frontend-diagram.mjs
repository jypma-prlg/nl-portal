// Builds the frontend package diagram (PlantUML) from the imports between workspace packages.
// Pure functions only; scripts/generate-frontend-diagram.mjs does the file I/O.

const IMPORT_PATTERN = /(?:from\s+|import\s*\(\s*|import\s+)["'](@nl-portal\/[a-z0-9-]+)(?:\/[^"']*)?["']/g;

/** Workspace packages (`@nl-portal/*`) that a source file imports, as a set of package names. */
export function importedPackages(source) {
  const found = new Set();
  for (const match of source.matchAll(IMPORT_PATTERN)) found.add(match[1]);
  return found;
}

/** One edge per package pair, with the number of importing statements. Self-imports are dropped. */
export function buildEdges(packages) {
  const counts = new Map();
  for (const pkg of packages) {
    for (const file of pkg.files) {
      for (const match of file.matchAll(IMPORT_PATTERN)) {
        if (match[1] === pkg.name) continue;
        const key = `${pkg.name}\u0000${match[1]}`;
        counts.set(key, (counts.get(key) ?? 0) + 1);
      }
    }
  }
  return [...counts.entries()]
    .map(([key, imports]) => {
      const [from, to] = key.split("\u0000");
      return { from, to, imports };
    })
    .sort((a, b) => a.from.localeCompare(b.from) || a.to.localeCompare(b.to));
}

/**
 * No rule covers the direction of an import between packages (package-boundaries only covers how a
 * package is imported), so every edge is a gap; an edge that is part of a cycle is flagged separately.
 */
export function classifyEdges(edges) {
  const reaches = (start, target, seen = new Set()) => {
    if (start === target) return true;
    if (seen.has(start)) return false;
    seen.add(start);
    return edges.filter((edge) => edge.from === start).some((edge) => reaches(edge.to, target, seen));
  };
  return edges.map((edge) => ({ ...edge, coverage: reaches(edge.to, edge.from) ? "cycle" : "gap" }));
}

/** Failures when the baseline and the packages in the code disagree, in both directions. */
export function compareBaseline(packageNames, baseline) {
  const failures = [];
  for (const name of packageNames.filter((n) => !(n in baseline))) {
    failures.push(`Package ${name} ontbreekt in de frontend-baseline: beoordeel omvang en risico en voeg haar toe`);
  }
  for (const name of Object.keys(baseline).filter((n) => !packageNames.includes(n))) {
    failures.push(`Package ${name} staat in de frontend-baseline maar bestaat niet meer: verwijder de regel`);
  }
  return failures;
}

const alias = (label) => label.replace(/[^A-Za-z0-9]/g, "_");
const FILL = { hoog: " #F4B6B6", middel: " #FBE3A6", laag: " #CFE8CF" };

export function renderFrontendDiagram({ packages, edges, baseline, rules }) {
  const labelOf = new Map(packages.map((pkg) => [pkg.name, pkg.label]));
  const out = ["@startuml", "title Frontend: packages en regeldekking", "left to right direction"];
  for (const pkg of [...packages].sort((a, b) => a.label.localeCompare(b.label))) {
    const assessment = baseline[pkg.name];
    const text = assessment ? `${pkg.label}\\nomvang ${assessment.omvang}, risico ${assessment.risico}` : pkg.label;
    out.push(`[${text}] as ${alias(pkg.label)}${assessment ? (FILL[assessment.risico] ?? "") : ""}`);
  }
  if (rules.length > 0) {
    out.push("note as frontend_rules", "  **Frontend-regels**");
    for (const rule of rules) {
      out.push(`  ${rule.name}: ${rule.description} (${rule.active ? "actief" : "voorgesteld, niet actief"})`);
    }
    out.push("end note");
  }
  const sorted = [...edges].sort(
    (a, b) => alias(labelOf.get(a.from)).localeCompare(alias(labelOf.get(b.from))) || alias(labelOf.get(a.to)).localeCompare(alias(labelOf.get(b.to))),
  );
  for (const edge of sorted) {
    const from = alias(labelOf.get(edge.from));
    const to = alias(labelOf.get(edge.to));
    out.push(
      edge.coverage === "cycle"
        ? `${from} -[#red,dotted]-> ${to} : cyclus (${edge.imports})`
        : `${from} ..> ${to} #orange : gat (${edge.imports})`,
    );
  }
  out.push(
    "legend right",
    "  oranje gestippeld: de richting van deze import wordt door geen regel bewaakt; het getal is het aantal imports",
    "  rood gestippeld: onderdeel van een cyclus, ook niet bewaakt",
    "  vulkleur: risico van de multitenancy-migratie (rood hoog, geel middel, groen laag)",
    "endlegend",
    "@enduml",
    "",
  );
  return out.join("\n");
}
