#!/usr/bin/env node
// Normalizes findings from dependency-check, grype, SonarCloud and gitleaks into one shape,
// and decides per finding whether to create, reopen or skip a GitHub issue (deduped against open
// and recently closed issues). See site/development/workflows/periodic-checks.md for the process this backs.

import { execFileSync } from "node:child_process";
import { readFileSync } from "node:fs";

const SEVERITY_MAP = {
  CRITICAL: "critical",
  HIGH: "high",
  Critical: "critical",
  High: "high",
  BLOCKER: "critical",
};

// "app.jar: grpc-netty-shaded-1.85.1.jar" -> "app.jar: grpc-netty-shaded.jar": a version bump
// must not turn a known finding into a new one (#324).
function withoutJarVersions(fileName) {
  // A version starts at "-<digit>" and may carry letter-led qualifiers ("-jre", "-SNAPSHOT"),
  // so a number inside the name itself ("log4j-1.2-api-2.20.0.jar") survives.
  return fileName.replace(/-\d[\w.+]*(?:-[A-Za-z][\w.+]*)*\.(jar|war|ear)\b/g, ".$1");
}

export function parseDependencyCheckFindings(report) {
  const findings = [];
  for (const dependency of report.dependencies ?? []) {
    for (const vulnerability of dependency.vulnerabilities ?? []) {
      const severity = SEVERITY_MAP[vulnerability.severity];
      if (severity !== "critical" && severity !== "high") continue;
      findings.push({
        id: vulnerability.name,
        summary: `${dependency.fileName}: ${vulnerability.name}`,
        artifact: withoutJarVersions(dependency.fileName),
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
      artifact: match.artifact.name,
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
      "[site/development/workflows/periodic-checks.md](../blob/main/site/development/workflows/periodic-checks.md).",
  ].join("\n");
}

// Reads the versionless artifact back out of a title built by buildIssueTitle for a
// dependency-check ("[security] CVE-x: app.jar: lib-1.2.jar: CVE-x") or grype
// ("[security] GHSA-x: lib@1.2: GHSA-x") finding. Null for any other title shape.
function artifactFromTitle(title, id) {
  const prefix = `[security] ${id}: `;
  const suffix = `: ${id}`;
  if (!title.startsWith(prefix) || !title.endsWith(suffix)) return null;
  const subject = title.slice(prefix.length, title.length - suffix.length);
  return withoutJarVersions(subject).replace(/(.)@[^@\s]+$/, "$1");
}

// Dependency findings match on CVE id + artifact without version (#324); findings without an
// artifact (SonarCloud, gitleaks) match on their id appearing in the title.
function reportsFinding(issue, finding) {
  if (finding.artifact) return artifactFromTitle(issue.title, finding.id) === finding.artifact;
  return issue.title.includes(finding.id);
}

export const REOPEN_WINDOW_DAYS = 90;
const DAY_MS = 24 * 60 * 60 * 1000;

// Per finding: skip when an open issue already reports it, reopen the best recently closed match
// (completed before not-planned, then most recently closed), otherwise create. A scanner that
// still reports a closed finding means it isn't resolved; accepting a risk is done with a scanner
// suppression, not by closing the issue (see periodic-checks.md).
function reopenWindowStart(now) {
  return new Date(now.getTime() - REOPEN_WINDOW_DAYS * DAY_MS);
}

function dedupKey(finding) {
  return finding.artifact ? `${finding.id} ${finding.artifact}` : finding.id;
}

export function planEscalation(findings, issues, now) {
  const since = reopenWindowStart(now).getTime();
  const seenKeys = new Set();
  const firstPerKey = findings.filter((finding) => {
    const key = dedupKey(finding);
    if (seenKeys.has(key)) return false;
    seenKeys.add(key);
    return true;
  });
  return firstPerKey.map((finding) => {
    const matches = issues.filter((issue) => reportsFinding(issue, finding));
    const open = matches.find((issue) => issue.state === "OPEN");
    if (open) return { action: "skip", finding, issue: open };

    const [closed] = matches
      .filter((issue) => issue.state === "CLOSED" && Date.parse(issue.closedAt) >= since)
      .sort(
        (a, b) =>
          (b.stateReason === "COMPLETED") - (a.stateReason === "COMPLETED") ||
          Date.parse(b.closedAt) - Date.parse(a.closedAt),
      );
    if (closed) return { action: "reopen", finding, issue: closed };
    return { action: "create", finding };
  });
}

// --- CLI glue below: reads a scan report (or queries SonarCloud), then creates or reopens a
// GitHub issue per Critical/High finding via the `gh` CLI, as planEscalation decides. Not unit-tested, same convention as
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

function listSecurityIssues(extraArgs) {
  const output = execFileSync(
    "gh",
    [
      "issue", "list",
      "--label", SECURITY_LABEL,
      "--json", "number,title,state,stateReason,closedAt",
      "--limit", "500",
      ...extraArgs,
    ],
    { encoding: "utf8" },
  );
  return JSON.parse(output);
}

// Open issues plus the ones closed within the reopen window; planEscalation decides which counts.
function fetchSecurityIssues(now) {
  const since = reopenWindowStart(now).toISOString().slice(0, 10);
  return [
    ...listSecurityIssues(["--state", "open"]),
    ...listSecurityIssues(["--state", "closed", "--search", `closed:>=${since}`]),
  ];
}

function reopenIssue(issue, finding) {
  const comment = [
    `Opnieuw gemeld door ${finding.source}: ${finding.summary}`,
    "",
    "Heropend door de security-escalatiestap omdat de scanner deze bevinding nog steeds meldt. " +
      "Een geaccepteerd risico leg je vast als suppressie in de scanner, niet door dit issue te sluiten; zie " +
      "[site/development/workflows/periodic-checks.md](../blob/main/site/development/workflows/periodic-checks.md).",
  ].join("\n");
  execFileSync("gh", ["issue", "reopen", String(issue.number), "--comment", comment], { stdio: "inherit" });
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
  const now = new Date();
  for (const { action, finding, issue } of planEscalation(findings, fetchSecurityIssues(now), now)) {
    switch (action) {
      case "create":
        createIssue(finding);
        console.log(`Issue aangemaakt voor ${finding.id}`);
        break;
      case "reopen":
        reopenIssue(issue, finding);
        console.log(`Issue #${issue.number} heropend voor ${finding.id}`);
        break;
      case "skip":
        console.log(`${finding.id}: al gemeld in open issue #${issue.number}`);
        break;
    }
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
