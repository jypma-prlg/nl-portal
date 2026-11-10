import { test } from "node:test";
import assert from "node:assert/strict";
import {
  parseDependencyCheckFindings,
  parseGrypeFindings,
  parseSonarVulnerabilities,
  parseGitleaksFindings,
  planEscalation,
  buildIssueTitle,
  buildIssueBody,
} from "./escalate-findings.mjs";

test("parseDependencyCheckFindings keeps only CRITICAL/HIGH severity vulnerabilities", () => {
  const report = {
    dependencies: [
      {
        fileName: "app.jar: bcprov-jdk18on-1.81.1.jar",
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
      summary: "app.jar: bcprov-jdk18on-1.81.1.jar: CVE-2026-8763",
      artifact: "app.jar: bcprov-jdk18on.jar",
      severity: "critical",
      source: "dependency-check",
    },
  ]);
});

test("parseDependencyCheckFindings strips the version from every jar in the artifact path", () => {
  const report = {
    dependencies: [
      {
        fileName: "app.jar: netty-codec-http2-4.1.100.Final.jar",
        vulnerabilities: [{ name: "CVE-2026-1111", severity: "HIGH" }],
      },
    ],
  };

  const [finding] = parseDependencyCheckFindings(report);

  assert.equal(finding.artifact, "app.jar: netty-codec-http2.jar");
});

test("parseDependencyCheckFindings keeps a number that is part of the artifact name", () => {
  const report = {
    dependencies: [
      { fileName: "log4j-1.2-api-2.20.0.jar", vulnerabilities: [{ name: "CVE-2026-2222", severity: "HIGH" }] },
      { fileName: "guava-33.0.0-jre.jar", vulnerabilities: [{ name: "CVE-2026-3333", severity: "HIGH" }] },
    ],
  };

  const artifacts = parseDependencyCheckFindings(report).map((finding) => finding.artifact);

  assert.deepEqual(artifacts, ["log4j-1.2-api.jar", "guava.jar"]);
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
      artifact: "shell-quote",
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

const NOW = new Date("2026-11-08T12:00:00Z");

function grpcFinding(version, id = "CVE-2026-42586") {
  return parseDependencyCheckFindings({
    dependencies: [
      {
        fileName: `app.jar: grpc-netty-shaded-${version}.jar`,
        vulnerabilities: [{ name: id, severity: "HIGH" }],
      },
    ],
  })[0];
}

function issue(number, title, state = "OPEN", stateReason = "", closedAt = null) {
  return { number, title, state, stateReason, closedAt };
}

test("planEscalation creates an issue for a CVE no issue reports yet", () => {
  const finding = grpcFinding("1.85.1");

  const plan = planEscalation([finding], [], NOW);

  assert.deepEqual(plan, [{ action: "create", finding }]);
});

test("planEscalation skips a CVE whose artifact already has an open issue for another version", () => {
  const finding = grpcFinding("1.85.1");
  const open = issue(272, "[security] CVE-2026-42586: app.jar: grpc-netty-shaded-1.84.1.jar: CVE-2026-42586");

  const plan = planEscalation([finding], [open], NOW);

  assert.deepEqual(plan, [{ action: "skip", finding, issue: open }]);
});

test("planEscalation does not match the same CVE reported for a different artifact", () => {
  const finding = grpcFinding("1.85.1");
  const other = issue(250, "[security] CVE-2026-42586: app.jar: protobuf-java-4.29.0.jar: CVE-2026-42586");

  const plan = planEscalation([finding], [other], NOW);

  assert.deepEqual(plan, [{ action: "create", finding }]);
});

test("planEscalation plans one issue when a run reports the same CVE for two versions of an artifact", () => {
  const older = grpcFinding("1.84.1");
  const newer = grpcFinding("1.85.1");

  const plan = planEscalation([older, newer], [], NOW);

  assert.deepEqual(plan, [{ action: "create", finding: older }]);
});

test("planEscalation recognizes a grype issue for a scoped npm package at another version", () => {
  const [finding] = parseGrypeFindings({
    matches: [
      {
        vulnerability: { id: "GHSA-aaaa-bbbb-cccc", severity: "High" },
        artifact: { name: "@graphql-tools/utils", version: "10.2.0" },
      },
    ],
  });
  const open = issue(300, "[security] GHSA-aaaa-bbbb-cccc: @graphql-tools/utils@10.0.0: GHSA-aaaa-bbbb-cccc");

  const plan = planEscalation([finding], [open], NOW);

  assert.deepEqual(plan, [{ action: "skip", finding, issue: open }]);
});

test("planEscalation reopens a recently completed issue instead of creating a duplicate", () => {
  const finding = grpcFinding("1.85.1");
  const completed = issue(272, "[security] CVE-2026-42586: app.jar: grpc-netty-shaded-1.84.1.jar: CVE-2026-42586", "CLOSED", "COMPLETED", "2026-11-08T09:16:29Z");
  const duplicate = issue(321, "[security] CVE-2026-42586: app.jar: grpc-netty-shaded-1.85.1.jar: CVE-2026-42586", "CLOSED", "NOT_PLANNED", "2026-11-08T11:49:32Z");

  const plan = planEscalation([finding], [duplicate, completed], NOW);

  assert.deepEqual(plan, [{ action: "reopen", finding, issue: completed }]);
});

test("planEscalation prefers an open issue over a closed one with the same key", () => {
  const finding = grpcFinding("1.85.1");
  const closed = issue(272, "[security] CVE-2026-42586: app.jar: grpc-netty-shaded-1.84.1.jar: CVE-2026-42586", "CLOSED", "COMPLETED", "2026-11-08T09:16:29Z");
  const open = issue(369, "[security] CVE-2026-42586: app.jar: grpc-netty-shaded-1.85.1.jar: CVE-2026-42586");

  const plan = planEscalation([finding], [closed, open], NOW);

  assert.deepEqual(plan, [{ action: "skip", finding, issue: open }]);
});

test("planEscalation creates a fresh issue when the matching one closed more than 90 days ago", () => {
  const finding = grpcFinding("1.85.1");
  const old = issue(12, "[security] CVE-2026-42586: app.jar: grpc-netty-shaded-1.60.0.jar: CVE-2026-42586", "CLOSED", "COMPLETED", "2026-08-09T12:00:00Z");

  const plan = planEscalation([finding], [old], NOW);

  assert.deepEqual(plan, [{ action: "create", finding }]);
});

test("planEscalation matches findings without an artifact on their id in the title", () => {
  const [finding] = parseSonarVulnerabilities([
    { key: "AY1234", rule: "java:S5542", message: "Weak cipher", severity: "BLOCKER", component: "c:src/Foo.kt", line: 1 },
  ]);
  const open = issue(40, "[security] AY1234: java:S5542: Weak cipher (src/Foo.kt:1)");

  const plan = planEscalation([finding], [open], NOW);

  assert.deepEqual(plan, [{ action: "skip", finding, issue: open }]);
});

test("buildIssueBody mentions source, severity and links back to the periodic-checks doc", () => {
  const finding = { id: "CVE-2026-8763", summary: "bcprov-jdk18on: CVE-2026-8763", severity: "critical", source: "dependency-check" };

  const body = buildIssueBody(finding);

  assert.match(body, /dependency-check/);
  assert.match(body, /critical/);
  assert.match(body, /periodic-checks\.md/);
});
