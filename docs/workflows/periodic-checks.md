# Workflow: periodieke checks

Eén overzicht van alle security- en kwaliteitschecks die *niet* op elke push/PR draaien, maar op
een vaste cadans - en wat er gebeurt zodra een van die checks (of een van de wél-continue checks)
iets Critical/High vindt. Dit document dupliceert [`security.md`](security.md),
[`dependency-updates.md`](dependency-updates.md) en [`sbom.md`](sbom.md) niet; het verwijst ernaar
en voegt de twee lagen toe die daar nog ontbraken: een afgedwongen maandelijkse cadans, en directe
escalatie bij een productie-relevante vondst.

## 1. Wat al continu/wekelijks draait (ongewijzigd)

| Check | Cadans | Workflow |
|---|---|---|
| SAST (backend + frontend) | Elke push naar `main` + elke PR | [`sonarcloud.yml`](../../.github/workflows/sonarcloud.yml) |
| Secret-scanning | Elke push naar `main` + elke PR | [`secret-scan.yml`](../../.github/workflows/secret-scan.yml) |
| Backend dependency-kwetsbaarheden | Wekelijks, zo 18:00 UTC | [`dependency-check.yml`](../../.github/workflows/dependency-check.yml) |
| Frontend dependency-kwetsbaarheden | Wekelijks, zo 19:00 UTC | [`frontend-vulnerability-scan.yml`](../../.github/workflows/frontend-vulnerability-scan.yml) |
| Dependency-updates (minor/patch) | Wekelijks, zo 18-23 UTC | Renovate (`.github/renovate.json`) |

Dit is niets nieuws - zie [`security.md`](security.md)'s "Repeat"-sectie. Deze tabel bestaat hier
alleen om het complete beeld te laten zien naast de nieuwe laag hieronder.

**Belangrijk, specifiek voor deze fork** (`jypma-prlg/nl-portal`): Renovate's config bestaat al
langer, maar de Renovate GitHub App moet apart op *deze* fork geïnstalleerd zijn
(github.com/apps/renovate) voordat er ook echt iets draait - een config-bestand alleen doet niets.
Geverifieerd actief sinds 2026-10-22 (zie issue #31, "Dependency Dashboard").

## 2. De nieuwe laag: maandelijks, oordeel-vereisend

Deze checks hadden een mise-task, maar geen cadans - ze gebeurden alleen als iemand er expliciet
aan dacht. `.github/workflows/periodic-maintenance.yml` draait ze nu maandelijks (1e van de maand,
06:00 UTC) en post de uitkomst als een nieuwe reactie op het doorlopende issue **"Periodieke
onderhoudscontrole"** (zelfde issue blijft open, elke maand een nieuwe reactie - vergelijkbaar met
Renovate's eigen Dependency Dashboard-issue, maar dan voor deze checks).

| Check | mise-task | Wat het oplevert |
|---|---|---|
| Major-dependency-review | `deps:check` | Lijst beschikbare major-updates (zie [dependency-updates.md §2](dependency-updates.md)) |
| SonarCloud-backlog-triage | `sonar:issues` | Open Bug-issues per project, gegroepeerd per regel |
| License-rapport | `security:license-report` | `generateLicenseReport`, nooit falend |
| License-allowlist-check | `security:check-license` | `checkLicense` - **momenteel kapot op `main`**, zie §4 |
| CVE-uitzonderingen herbeoordelen | - | `backend/cve-exclusions.xml` + de `braces`-uitzondering in `frontend/pnpm-workspace.yaml`; de `braces`-advisory (GHSA-vfj7-8cjw-p6xm) wordt automatisch tegen GitHub's advisory-API gecheckt |

Lokaal uitvoeren zonder op de 1e van de maand te wachten:

```bash
mise run deps:check
mise run sonar:issues
mise run security:license-report
mise run security:check-license
node scripts/periodic-maintenance.mjs   # vereist gh CLI + GH_TOKEN, post naar het echte issue
```

## 3. Escalatie: direct handelen bij Critical/High

Vier van de al-bestaande, continue/wekelijkse workflows krijgen een extra stap die bij een
Critical/High-bevinding automatisch een eigen GitHub-issue aanmaakt (label `security` +
`severity:critical`/`severity:high`), in plaats van te wachten op de maandelijkse batch hierboven.
Dit is de reden waarom een vondst na livegang niet onopgemerkt in een rode CI-run kan blijven
liggen.

| Workflow | Bron | Drempel |
|---|---|---|
| `dependency-check.yml` | OWASP Dependency-Check JSON-rapport | severity Critical/High |
| `frontend-vulnerability-scan.yml` | grype JSON-output | severity Critical/High |
| `sonarcloud.yml` (nieuwe `escalate`-job, alleen op push naar `main`) | SonarCloud API, `types=VULNERABILITY` | severity Blocker/Critical |
| `secret-scan.yml` | gitleaks JSON-rapport (`--redact`, nooit de rauwe secret) | elke bevinding |

Gedeelde logica in [`scripts/escalate-findings.mjs`](../../scripts/escalate-findings.mjs)
(unit-getest: zie `scripts/escalate-findings.test.mjs`):

- **Dedupliceren**: vóór het aanmaken wordt gezocht naar een al-open issue met dezelfde
  CVE-/GHSA-/Sonar-issue-key/gitleaks-fingerprint in de titel. Bestaat die al, dan gebeurt er
  niets - zo spamt een aanhoudende wekelijkse bevinding niet elke week een nieuw issue.
- **SonarCloud-drempel**: alleen issue-type Vulnerability op Blocker/Critical-niveau escaleert
  direct; Bug/Code Smell en lagere severities blijven in de maandelijkse `sonar:issues`-triage uit
  §2 - anders te veel ruis op elke push.
- **Bekende vertraging**: SonarCloud's eigen analyse-verwerking loopt soms een paar minuten achter
  op de `escalate`-job (zie ook de operationele les hierover in de architectuurreview van
  2026-10-22). Een gemiste bevinding komt dan bij de eerstvolgende push naar `main` alsnog naar
  boven - geen polling-loop toegevoegd om dit op te lossen, dat woog niet op tegen de complexiteit.

## 4. Bekende openstaande items (eerste twee regels in het tracking-issue)

- **`checkLicense` staat kapot op `main`**: "Some library licenses are not allowed." Niet
  opgelost als onderdeel van dit document - alleen zichtbaar gemaakt via
  `security:check-license` en elke maand opnieuw gerapporteerd totdat iemand het oppakt.
- **`braces` GHSA-vfj7-8cjw-p6xm**: nog geen upstream-fix (laatst gecontroleerd 2026-10-22,
  `first_patched_version: null`). De uitzondering in `frontend/pnpm-workspace.yaml` blijft staan;
  `scripts/periodic-maintenance.mjs` checkt dit automatisch elke maand bij GitHub's advisory-API en
  meldt zodra er wél een fix beschikbaar is.

## 5. Eigenaarschap

Momenteel is er één maintainer, die zelf op elk escalatie-issue en de maandelijkse checklist
handelt. Bij livegang in productie wordt dit onderdeel van het team-proces (rotatie/toewijzing) -
dit document gaat daar nu nog niet op vooruitlopen.
