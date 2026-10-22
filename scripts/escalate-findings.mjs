#!/usr/bin/env node
// Normalizes findings from dependency-check, grype, SonarCloud and gitleaks into one shape,
// and decides which ones still need a GitHub issue opened for them (deduped against already-open
// issues). See docs/workflows/periodic-checks.md for the process this backs.

import { execFileSync } from "node:child_process";
import { readFileSync } from "node:fs";

const SEVERITY_MAP = {
  CRITICAL: "critical",
  HIGH: "high",
  Critical: "critical",
  High: "high",
  BLOCKER: "critical",
};

export function parseDependencyCheckFindings(report) {
  const findings = [];
  for (const dependency of report.dependencies ?? []) {
    for (const vulnerability of dependency.vulnerabilities ?? []) {
      const severity = SEVERITY_MAP[vulnerability.severity];
      if (severity !== "critical" && severity !== "high") continue;
      findings.push({
        id: vulnerability.name,
        summary: `${dependency.fileName}: ${vulnerability.name}`,
        severity,
        source: "dependency-check",
      });
    }
  }
  return findings;
}

export function parseGrypeFindings(grypeOutput) {
  const findings = [];
  for (const match of grypeOutput.matches ?? []) {
    const severity = SEVERITY_MAP[match.vulnerability.severity];
    if (severity !== "critical" && severity !== "high") continue;
    findings.push({
      id: match.vulnerability.id,
      summary: `${match.artifact.name}@${match.artifact.version}: ${match.vulnerability.id}`,
      severity,
      source: "grype",
    });
  }
  return findings;
}

export function parseSonarVulnerabilities(issues) {
  const findings = [];
  for (const issue of issues) {
    const severity = SEVERITY_MAP[issue.severity];
    if (severity !== "critical") continue;
    const path = issue.component.split(":").slice(1).join(":");
    findings.push({
      id: issue.key,
      summary: `${issue.rule}: ${issue.message} (${path}:${issue.line})`,
      severity,
      source: "sonarcloud",
    });
  }
  return findings;
}

// Gitleaks' own JSON report carries the raw secret in `Secret`/`Match` - deliberately never
// read here, so a leaked credential can't end up quoted inside a GitHub issue body.
export function parseGitleaksFindings(gitleaksReport) {
  return gitleaksReport.map((finding) => ({
    id: finding.Fingerprint,
    summary: `${finding.RuleID}: ${finding.Description} (${finding.File}:${finding.StartLine})`,
    severity: "critical",
    source: "secret-scan",
  }));
}

export function buildIssueTitle(finding) {
  return `[security] ${finding.id}: ${finding.summary}`;
}

export function buildIssueBody(finding) {
  return [
    `**Bron**: ${finding.source}`,
    `**Severity**: ${finding.severity}`,
    "",
    finding.summary,
    "",
    "Automatisch aangemaakt door de security-escalatiestap, zie " +
      "[docs/workflows/periodic-checks.md](../blob/main/docs/workflows/periodic-checks.md).",
  ].join("\n");
}

export function findingsNeedingIssues(findings, openIssueTitles) {
  return findings.filter(
    (finding) => !openIssueTitles.some((title) => title.includes(finding.id)),
  );
}

// --- CLI glue below: reads a scan report (or queries SonarCloud), creates a GitHub issue per
// new Critical/High finding via the `gh` CLI. Not unit-tested, same convention as
// scripts/sonar-issues.mjs - this is I/O glue, the decision logic above is what's tested.

const SONAR_ORG = "pinkroccade";
const SECURITY_LABEL = "security";
const SEVERITY_LABELS = { critical: "severity:critical", high: "severity:high" };

async function fetchSonarVulnerabilities(componentKey) {
  const url = new URL("https://sonarcloud.io/api/issues/search");
  url.searchParams.set("componentKeys", componentKey);
  url.searchParams.set("organization", SONAR_ORG);
  url.searchParams.set("resolved", "false");
  url.searchParams.set("types", "VULNERABILITY");
  url.searchParams.set("ps", "500");

  const res = await fetch(url);
  if (!res.ok) {
    throw new Error(`SonarCloud API ${res.status} for ${componentKey}: ${await res.text()}`);
  }
  return (await res.json()).issues;
}

function ensureLabelsExist() {
  execFileSync("gh", ["label", "create", SECURITY_LABEL, "--color", "d73a4a", "--force"], { stdio: "ignore" });
  execFileSync("gh", ["label", "create", "severity:critical", "--color", "b60205", "--force"], { stdio: "ignore" });
  execFileSync("gh", ["label", "create", "severity:high", "--color", "e99695", "--force"], { stdio: "ignore" });
}

function fetchOpenIssueTitles() {
  const output = execFileSync(
    "gh",
    ["issue", "list", "--label", SECURITY_LABEL, "--state", "open", "--json", "title", "--limit", "200"],
    { encoding: "utf8" },
  );
  return JSON.parse(output).map((issue) => issue.title);
}

function createIssue(finding) {
  execFileSync(
    "gh",
    [
      "issue", "create",
      "--title", buildIssueTitle(finding),
      "--body", buildIssueBody(finding),
      "--label", SECURITY_LABEL,
      "--label", SEVERITY_LABELS[finding.severity],
    ],
    { stdio: "inherit" },
  );
}

function parseArgs(argv) {
  return Object.fromEntries(
    argv.map((arg) => {
      const [key, ...rest] = arg.replace(/^--/, "").split("=");
      return [key, rest.join("=")];
    }),
  );
}

async function main() {
  const args = parseArgs(process.argv.slice(2));

  let findings;
  switch (args.source) {
    case "dependency-check":
      findings = parseDependencyCheckFindings(JSON.parse(readFileSync(args.report, "utf8")));
      break;
    case "grype":
      findings = parseGrypeFindings(JSON.parse(readFileSync(args.report, "utf8")));
      break;
    case "sonarcloud":
      findings = parseSonarVulnerabilities(await fetchSonarVulnerabilities(args.component));
      break;
    case "secret-scan":
      findings = parseGitleaksFindings(JSON.parse(readFileSync(args.report, "utf8")));
      break;
    default:
      throw new Error(`Onbekende --source: ${args.source}`);
  }

  if (findings.length === 0) {
    console.log("Geen Critical/High-bevindingen om te escaleren.");
    return;
  }

  ensureLabelsExist();
  const needed = findingsNeedingIssues(findings, fetchOpenIssueTitles());

  if (needed.length === 0) {
    console.log(`${findings.length} bevinding(en) gevonden, voor elk bestaat al een open issue.`);
    return;
  }

  for (const finding of needed) {
    createIssue(finding);
    console.log(`Issue aangemaakt voor ${finding.id}`);
  }
}

const isMainModule =
  process.argv[1] && import.meta.url === new URL(process.argv[1], "file://").href;
if (isMainModule) {
  main().catch((err) => {
    console.error(err);
    process.exitCode = 1;
  });
}
