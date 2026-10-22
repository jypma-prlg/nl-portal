import { test } from "node:test";
import assert from "node:assert/strict";
import {
  parseDependencyCheckFindings,
  parseGrypeFindings,
  parseSonarVulnerabilities,
  parseGitleaksFindings,
  findingsNeedingIssues,
  buildIssueTitle,
  buildIssueBody,
} from "./escalate-findings.mjs";

test("parseDependencyCheckFindings keeps only CRITICAL/HIGH severity vulnerabilities", () => {
  const report = {
    dependencies: [
      {
        fileName: "bcprov-jdk18on-1.81.1.jar",
        vulnerabilities: [
          { name: "CVE-2026-8763", severity: "CRITICAL", cvssv3: { baseScore: 9.8 } },
        ],
      },
      {
        fileName: "some-low-risk-lib-1.0.0.jar",
        vulnerabilities: [
          { name: "CVE-2020-0001", severity: "LOW", cvssv3: { baseScore: 3.1 } },
        ],
      },
    ],
  };

  const findings = parseDependencyCheckFindings(report);

  assert.deepEqual(findings, [
    {
      id: "CVE-2026-8763",
      summary: "bcprov-jdk18on-1.81.1.jar: CVE-2026-8763",
      severity: "critical",
      source: "dependency-check",
    },
  ]);
});

test("parseGrypeFindings keeps only Critical/High severity matches", () => {
  const grypeOutput = {
    matches: [
      {
        vulnerability: { id: "GHSA-pqg4-j6r4-53mv", severity: "Critical" },
        artifact: { name: "shell-quote", version: "1.9.0" },
      },
      {
        vulnerability: { id: "GHSA-aaaa-bbbb-cccc", severity: "Medium" },
        artifact: { name: "some-lib", version: "2.0.0" },
      },
    ],
  };

  const findings = parseGrypeFindings(grypeOutput);

  assert.deepEqual(findings, [
    {
      id: "GHSA-pqg4-j6r4-53mv",
      summary: "shell-quote@1.9.0: GHSA-pqg4-j6r4-53mv",
      severity: "critical",
      source: "grype",
    },
  ]);
});

test("parseSonarVulnerabilities keeps only BLOCKER/CRITICAL severity issues", () => {
  const issues = [
    {
      key: "AY1234",
      rule: "java:S5542",
      message: "Cipher algorithms should be robust",
      severity: "BLOCKER",
      component: "pinkroccade_nl-portal-backend:src/main/Foo.kt",
      line: 42,
    },
    {
      key: "AY5678",
      rule: "java:S1234",
      message: "Something minor",
      severity: "MAJOR",
      component: "pinkroccade_nl-portal-backend:src/main/Bar.kt",
      line: 7,
    },
  ];

  const findings = parseSonarVulnerabilities(issues);

  assert.deepEqual(findings, [
    {
      id: "AY1234",
      summary: "java:S5542: Cipher algorithms should be robust (src/main/Foo.kt:42)",
      severity: "critical",
      source: "sonarcloud",
    },
  ]);
});

test("parseGitleaksFindings normalizes every finding and never includes the raw secret", () => {
  const gitleaksReport = [
    {
      RuleID: "generic-api-key",
      Description: "Generic API Key",
      File: "backend/app/src/main/resources/application.yml",
      StartLine: 12,
      Secret: "the-raw-secret-value-must-never-leak-into-an-issue",
      Match: "api_key: the-raw-secret-value-must-never-leak-into-an-issue",
      Fingerprint: "abcdef1234567890:application.yml:generic-api-key:12",
    },
  ];

  const findings = parseGitleaksFindings(gitleaksReport);

  assert.deepEqual(findings, [
    {
      id: "abcdef1234567890:application.yml:generic-api-key:12",
      summary: "generic-api-key: Generic API Key (backend/app/src/main/resources/application.yml:12)",
      severity: "critical",
      source: "secret-scan",
    },
  ]);
  const serialized = JSON.stringify(findings);
  assert.ok(
    !serialized.includes("must-never-leak-into-an-issue"),
    "raw secret material must never appear in a finding",
  );
});

test("buildIssueTitle embeds the finding id so dedup can find it again", () => {
  const finding = { id: "CVE-2026-8763", summary: "bcprov-jdk18on: CVE-2026-8763", severity: "critical" };

  assert.equal(buildIssueTitle(finding), "[security] CVE-2026-8763: bcprov-jdk18on: CVE-2026-8763");
});

test("findingsNeedingIssues drops a finding that already has a matching open issue", () => {
  const findings = [
    { id: "CVE-2026-8763", summary: "bcprov-jdk18on: CVE-2026-8763", severity: "critical" },
    { id: "GHSA-pqg4-j6r4-53mv", summary: "shell-quote: GHSA-pqg4-j6r4-53mv", severity: "critical" },
  ];
  const openIssueTitles = ["[security] CVE-2026-8763: bcprov-jdk18on: CVE-2026-8763"];

  const needed = findingsNeedingIssues(findings, openIssueTitles);

  assert.deepEqual(needed, [findings[1]]);
});

test("buildIssueBody mentions source, severity and links back to the periodic-checks doc", () => {
  const finding = { id: "CVE-2026-8763", summary: "bcprov-jdk18on: CVE-2026-8763", severity: "critical", source: "dependency-check" };

  const body = buildIssueBody(finding);

  assert.match(body, /dependency-check/);
  assert.match(body, /critical/);
  assert.match(body, /periodic-checks\.md/);
});
