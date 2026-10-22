#!/usr/bin/env node
// Monthly maintenance ritual: the judgement-heavy checks that `mise.toml`'s report-only tasks
// surface but that have no automatic cadence of their own (unlike the weekly scans). Posts a
// comment on one persistent "Periodieke onderhoudscontrole" issue. See
// docs/workflows/periodic-checks.md.

import { execFileSync } from "node:child_process";

export function isAdvisoryStillUnpatched(advisory) {
  return advisory.vulnerabilities.every((v) => v.first_patched_version === null);
}

export function buildMaintenanceComment({
  date,
  depsCheckOutput,
  sonarIssuesOutput,
  licenseReportOk,
  checkLicenseOk,
  bracesStillUnpatched,
}) {
  const checkLicenseLine = checkLicenseOk
    ? "- [x] `checkLicense` - OK"
    : "- [ ] `checkLicense` - FAIL (bekend openstaand item, zie docs/workflows/dependency-updates.md §3)";
  const bracesLine = bracesStillUnpatched
    ? "- [ ] `braces` GHSA-vfj7-8cjw-p6xm - nog geen fix upstream"
    : "- [x] `braces` GHSA-vfj7-8cjw-p6xm - fix beschikbaar, uitzondering in frontend/pnpm-workspace.yaml kan eruit";

  return [
    `## ${date}`,
    "",
    "### Major-dependency-updates (`mise run deps:check`)",
    "```",
    depsCheckOutput || "(geen output)",
    "```",
    "",
    "### SonarCloud-backlog (`mise run sonar:issues`)",
    "```",
    sonarIssuesOutput || "(geen output)",
    "```",
    "",
    "### License-rapport",
    licenseReportOk ? "- [x] `generateLicenseReport` - OK" : "- [ ] `generateLicenseReport` - FAIL",
    checkLicenseLine,
    "",
    "### CVE-uitzonderingen herbeoordelen",
    bracesLine,
    "- [ ] `backend/cve-exclusions.xml` - loop de suppressies door, nog actueel?",
  ].join("\n");
}

// --- CLI glue below: niet unit-getest, zelfde conventie als scripts/sonar-issues.mjs.

const DASHBOARD_ISSUE_TITLE = "Periodieke onderhoudscontrole";
const BRACES_ADVISORY = "GHSA-vfj7-8cjw-p6xm";

async function fetchBracesAdvisory() {
  const res = await fetch(`https://api.github.com/advisories/${BRACES_ADVISORY}`);
  if (!res.ok) {
    throw new Error(`GitHub advisory API ${res.status}: ${await res.text()}`);
  }
  return res.json();
}

function findDashboardIssueNumber() {
  const output = execFileSync(
    "gh",
    ["issue", "list", "--search", `in:title "${DASHBOARD_ISSUE_TITLE}"`, "--state", "open", "--json", "number,title"],
    { encoding: "utf8" },
  );
  const match = JSON.parse(output).find((issue) => issue.title === DASHBOARD_ISSUE_TITLE);
  return match?.number;
}

function createDashboardIssue() {
  // GitHub's search index (used by findDashboardIssueNumber) isn't guaranteed read-your-writes
  // consistent, so parse the number straight from `gh issue create`'s own stdout (a URL ending
  // in /issues/<n>) instead of re-querying it right after creation.
  const output = execFileSync(
    "gh",
    [
      "issue", "create",
      "--title", DASHBOARD_ISSUE_TITLE,
      "--body",
      "Doorlopend issue voor de maandelijkse onderhoudscontrole - zie " +
        "[docs/workflows/periodic-checks.md](../blob/main/docs/workflows/periodic-checks.md). " +
        "Elke maand komt er een nieuwe reactie bij met de actuele status; deze issue blijft open.",
      "--label", "maintenance",
    ],
    { encoding: "utf8" },
  );
  const match = output.trim().match(/\/issues\/(\d+)\s*$/);
  if (!match) {
    throw new Error(`Kon issue-nummer niet uit 'gh issue create'-output halen: ${output}`);
  }
  return Number(match[1]);
}

function postComment(issueNumber, body) {
  execFileSync("gh", ["issue", "comment", String(issueNumber), "--body", body], { stdio: "inherit" });
}

function runReportOnly(command, args, options) {
  try {
    return { ok: true, output: execFileSync(command, args, { encoding: "utf8", ...options }) };
  } catch (err) {
    return { ok: false, output: err.stdout ?? err.message };
  }
}

async function main() {
  execFileSync("gh", ["label", "create", "maintenance", "--color", "0366d6", "--force"], { stdio: "ignore" });

  const deps = runReportOnly("mise", ["run", "deps:check"]);
  const sonar = runReportOnly("mise", ["run", "sonar:issues"]);
  const licenseReport = runReportOnly("mise", ["run", "security:license-report"]);
  const checkLicense = runReportOnly("mise", ["run", "security:check-license"]);
  const bracesAdvisory = await fetchBracesAdvisory();

  const comment = buildMaintenanceComment({
    date: new Date().toISOString().slice(0, 10),
    depsCheckOutput: deps.output,
    sonarIssuesOutput: sonar.output,
    licenseReportOk: licenseReport.ok,
    checkLicenseOk: checkLicense.ok,
    bracesStillUnpatched: isAdvisoryStillUnpatched(bracesAdvisory),
  });

  const issueNumber = findDashboardIssueNumber() ?? createDashboardIssue();
  postComment(issueNumber, comment);
  console.log(`Maandelijkse update geplaatst op issue #${issueNumber}`);
}

const isMainModule =
  process.argv[1] && import.meta.url === new URL(process.argv[1], "file://").href;
if (isMainModule) {
  main().catch((err) => {
    console.error(err);
    process.exitCode = 1;
  });
}
