#!/usr/bin/env node
// Lists open SonarCloud issues for the two nl-portal projects, grouped by rule.
// Read-only reporting: picking the next issue to fix and writing the TDD red/green pair
// stays a deliberate, per-rule decision.
//
// Both projects are public on SonarCloud, so reading issues needs no token. SONAR_TOKEN
// (see mise.local.toml.example) is picked up if set - useful for a future write-capable
// use of this script (e.g. marking issues resolved), but optional for listing.

const ORG = "pinkroccade";
const PROJECTS = ["pinkroccade_nl-portal-backend", "pinkroccade_nl-portal-frontend"];
const TOKEN = process.env.SONAR_TOKEN;

const typeFilter = process.argv[2] ?? "BUG";

async function fetchIssues(componentKey) {
  const url = new URL("https://sonarcloud.io/api/issues/search");
  url.searchParams.set("componentKeys", componentKey);
  url.searchParams.set("organization", ORG);
  url.searchParams.set("resolved", "false");
  url.searchParams.set("types", typeFilter);
  url.searchParams.set("ps", "500");

  const res = await fetch(url, {
    headers: TOKEN ? { Authorization: `Bearer ${TOKEN}` } : {},
  });
  if (!res.ok) {
    throw new Error(`SonarCloud API ${res.status} for ${componentKey}: ${await res.text()}`);
  }
  return (await res.json()).issues;
}

for (const project of PROJECTS) {
  const issues = await fetchIssues(project);
  console.log(`\n=== ${project} (${issues.length} open ${typeFilter}) ===`);

  const byRule = new Map();
  for (const issue of issues) {
    if (!byRule.has(issue.rule)) byRule.set(issue.rule, []);
    byRule.get(issue.rule).push(issue);
  }

  const sorted = [...byRule.entries()].sort((a, b) => b[1].length - a[1].length);
  for (const [rule, group] of sorted) {
    console.log(`\n${rule} (${group.length}x) - ${group[0].message}`);
    for (const issue of group) {
      const path = issue.component.replace(`${project}:`, "");
      console.log(`  ${path}${issue.line ? ":" + issue.line : ""}`);
    }
  }
}
