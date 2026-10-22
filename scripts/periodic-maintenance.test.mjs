import { test } from "node:test";
import assert from "node:assert/strict";
import { isAdvisoryStillUnpatched, buildMaintenanceComment } from "./periodic-maintenance.mjs";

test("isAdvisoryStillUnpatched is true when GitHub's advisory has no first_patched_version", () => {
  const advisory = {
    vulnerabilities: [{ package: { name: "braces" }, first_patched_version: null }],
  };

  assert.equal(isAdvisoryStillUnpatched(advisory), true);
});

test("isAdvisoryStillUnpatched is false once a fixed version is published", () => {
  const advisory = {
    vulnerabilities: [{ package: { name: "braces" }, first_patched_version: "3.0.4" }],
  };

  assert.equal(isAdvisoryStillUnpatched(advisory), false);
});

test("buildMaintenanceComment lists the known backlog items and this month's findings", () => {
  const comment = buildMaintenanceComment({
    date: "2026-11-01",
    depsCheckOutput: "frontend: @vitejs/plugin-react 5.0.0 -> 6.0.0 (major)",
    sonarIssuesOutput: "backend: 2 open Bug issues",
    licenseReportOk: true,
    checkLicenseOk: false,
    bracesStillUnpatched: true,
  });

  assert.match(comment, /## 2026-11-01/);
  assert.match(comment, /checkLicense.*FAIL/s);
  assert.match(comment, /braces.*nog geen fix/s);
  assert.match(comment, /@vitejs\/plugin-react/);
  assert.match(comment, /2 open Bug issues/);
});

test("buildMaintenanceComment reports the braces exception as resolved once a fix ships", () => {
  const comment = buildMaintenanceComment({
    date: "2026-12-01",
    depsCheckOutput: "",
    sonarIssuesOutput: "",
    licenseReportOk: true,
    checkLicenseOk: true,
    bracesStillUnpatched: false,
  });

  assert.match(comment, /checkLicense.*OK/s);
  assert.match(comment, /braces.*fix beschikbaar/s);
});
