# Workflow: SBOM genereren

Een SBOM (Software Bill of Materials, CycloneDX-formaat) is een machineleesbare lijst van alle
(transitieve) afhankelijkheden van een artefact. Dit document legt vast wat er al bestaat, breidt
het uit naar de frontend, en maakt het lokaal/on-demand draaibaar in plaats van alleen
release-only.

## 1. Wat er al bestaat

De backend-release-pijplijn genereert al een CycloneDX-SBOM:
`.github/workflows/_stage-be.yml` bouwt het project en draait dan `anchore/sbom-action`
(`format: cyclonedx-json`) over `backend/`; het resultaat wordt als `sbom.cyclonedx.json`
gepubliceerd naar `s3://nl-portal-dependency-reports/sbom/<versie>/backend.cdx.json` (zie
`_publish-be-native.yml`).

**Beperkingen van de huidige opzet:**

- Alleen de **backend**, alleen bij een **release**. De frontend heeft geen SBOM.
- Niet lokaal/on-demand draaibaar zonder de volledige release-pijplijn te doorlopen.
- `anchore/sbom-action` is zelf een wrapper rond **Syft** — hetzelfde tool dat dit document
  hieronder direct gebruikt, dus de output is identiek van vorm.

## 2. Lokaal / on-demand genereren

```bash
mise run sbom:backend     # vereist een build (resolved dependencies)
mise run sbom:frontend    # leest pnpm-lock.yaml direct, geen build nodig
mise run sbom:all         # beide
```

Output: `backend/sbom.cyclonedx.json` resp. `frontend/sbom.cyclonedx.json` (CycloneDX JSON,
dezelfde vorm als de S3-upload uit de release-pijplijn).

### Waarom backend een build nodig heeft en frontend niet

De backend heeft geen Gradle dependency-locking (geen `gradle.lockfile`-bestanden in de repo), dus
Syft kan de opgeloste versies alleen aflezen uit een gebouwd project (Gradle's module-cache +
build-output) — vandaar `./gradlew assemble` als onderdeel van `sbom:backend`, exact zoals
`_stage-be.yml` dat ook doet (build, dán scannen).

De frontend heeft wél een lockfile (`pnpm-lock.yaml`), en Syft's `javascript-lock-cataloger` leest
die direct — `sbom:frontend` heeft dus geen `pnpm install` nodig en is daardoor sneller. Dit is
**geverifieerd** tijdens het schrijven van dit document: `syft dir:frontend` tegen de huidige
`pnpm-lock.yaml` leverde een CycloneDX-SBOM met 1397 componenten op, puur op basis van de lockfile.

## 3. Wat je met de SBOM doet

Een SBOM is een inventarisatie, geen scan op zich — koppel hem aan een van deze:

- **Kwetsbaarheden**: `grype sbom:backend/sbom.cyclonedx.json` (of `frontend/...`) — zie
  [`docs/workflows/security.md`](security.md) voor waarom dit de frontend-blinde-vlek dicht die
  `dependency-check.yml` vandaag laat liggen (die draait alleen op `backend`).
- **Licenties**: kruis de SBOM met `backend/build/reports/dependency-license/index.html`
  (zie [`docs/workflows/dependency-updates.md`](dependency-updates.md), sectie 3) om te
  controleren of een nieuwe afhankelijkheid EUPL-compatibel is.
- **Drift tussen releases**: bewaar de SBOM van de vorige release naast de huidige en diff de
  `components[].purl`-lijst — nieuwe of verdwenen componenten zijn een goed startpunt voor een
  release-review.

## 4. Uitbreiding naar de release-pijplijn (voorstel)

Niet in deze wijziging doorgevoerd (buiten scope van een documentatie-PR), maar de logische
vervolgstap: `_stage-fe.yml` krijgt een `Generate SBOM`-stap die `mise run sbom:frontend` draait en
uploadt naar S3, analoog aan hoe `_stage-be.yml`/`_publish-be-native.yml` dat nu al voor de backend
doen — zodat elke release zowel een backend- als een frontend-SBOM publiceert.
