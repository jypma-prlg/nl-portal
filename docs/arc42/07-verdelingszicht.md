# 7. Verdelingszicht (deployment view)

## 7.1 Lokale ontwikkeling

`docker-compose/` draait de ondersteunende diensten (Keycloak, Open Zaak-suite, Haal Centraal,
ClamAV, databases) via profielen (`zgw`, `haalcentraal`, `clamav`, `config`); `RUN_MODE` kiest
onafhankelijk hoe de NL Portal-app zelf draait:

| RUN_MODE | Wat draait waar |
|---|---|
| `sources` (default) | Geen app-container; backend via `./gradlew :app-dev:bootRun`, frontend via `pnpm dev` — snelste innerloop |
| `local` | Backend + frontend gebouwd uit deze monorepo-sources, als container | 
| `remote` | Gepubliceerde images van GHCR (`ghcr.io/nl-portal/nl-portal-backend`, `...-frontend`) |

Zie [`docker-compose/README.md`](../../docker-compose/README.md) voor de volledige service-/poorttabel.

## 7.2 Build en packaging

- **Backend**: Gradle multi-module build → `nl-portal-backend`-image (`backend/Dockerfile`).
  JReleaser (`backend/jreleaser.yml`) verzorgt de release-metadata.
- **Frontend**: pnpm-workspace build → `nl-portal-frontend`-image (`frontend/Dockerfile`); de
  `@nl-portal/*`-bibliotheken worden ook los gepubliceerd (npm) voor hergebruik buiten de shippable
  app.
- **CI** (`.github/workflows/`): `ci.yml` (algemene build/test), `_stage-be.yml`/`_stage-fe.yml`
  (buildartefacten + reports/SBOM), `build-and-publish-*.yml` (snapshot/release-candidate/release),
  `sonarcloud.yml` (kwaliteitspoort), `dependency-check.yml` (beveiligingspoort — zie deliverable 6).

## 7.3 Productie

Buiten deze repository: Helm-charts in
[`nl-portal/helm-charts`](https://github.com/nl-portal/helm-charts) rollen de twee images (backend,
frontend) uit op Kubernetes. Dit arc42-document beschrijft die laag niet in detail — dat hoort bij
het helm-charts-repository — maar benoemt wel de aanname die eruit volgt: **NL Portal's eigen
database is per-installatie**, niet gedeeld tussen afnemers; elke overheidsorganisatie draait haar
eigen instantie tegen haar eigen ZGW-landschap.

## 7.4 Open vraag

Er is geen architectuurdocument gevonden dat de productie-topologie (hoeveel replica's, welke
externe systemen zijn per-omgeving vs. gedeeld, hoe secrets worden aangeleverd) beschrijft binnen
déze repository. Als dat elders bestaat (bijv. in het helm-charts-repository), is een verwijzing
hiernaar een goede eerste aanvulling op dit hoofdstuk.
