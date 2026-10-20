# 2. Randvoorwaarden

## 2.1 Technische randvoorwaarden

| Randvoorwaarde | Bron |
|---|---|
| Backend in Kotlin op Spring Boot, Gradle multi-module build | `backend/settings.gradle.kts`, `backend/build.gradle.kts` |
| Frontend in React/TypeScript, pnpm-workspace, Vite | `frontend/pnpm-workspace.yaml`, `frontend/package.json` |
| Moet integreren met de VNG ZGW API-standaard (Zaken, Catalogi, Documenten, Besluiten, Klantinteracties, Objecten) | `documentation/fundamentals/architectuur` |
| Authenticatie via DigiD (burgers) en eHerkenning (ondernemers), gefedereerd via een OIDC-broker (Keycloak in de huidige stack) | `documentation/fundamentals/architectuur/authentication-en-authorization.md` |
| Monorepo: backend, frontend, docker-compose en documentatie releasen op één gedeelde versie | `README.md`, `CONTRIBUTING.md` |
| Containerimages `nl-portal-backend`/`nl-portal-frontend`, Helm-charts in apart repository | `docker-compose/README.md`, `README.md` |

## 2.2 Organisatorische randvoorwaarden

- **Open source, EUPL 1.2.** Iedere overheidsorganisatie mag het vrij gebruiken en wijzigen; dit
  beperkt het gebruik van niet-EUPL-compatibele afhankelijkheden en sluit organisatiespecifieke
  hardcoded aannames uit (zie `LICENSE.MD`).
  - **Let op**: de aard van de licentie is hiermee geverifieerd, maar een uitputtende
    license-compatibiliteitscheck van alle transitieve dependencies is niet uitgevoerd als
    onderdeel van deze review. Deliverable 5 (SBOM) is de aangewezen plek om dat structureel te
    borgen (`dependency-license-report`-plugin is al aanwezig in `backend/settings.gradle.kts`).
- **Issue-first workflow.** Elke PR (behalve typo's/docs/dependency-bumps) heeft een gekoppeld issue
  in een apart issues-repository; grotere features gaan via een RFC (`CONTRIBUTING.md`).
- **Spring-achtig branchmodel** met `main`, `release/<major>.x` en `rc/*`; squash-merge met
  Conventional Commit-titel (`CONTRIBUTING.md`).
- **Twee onderhouden major-lijnen tegelijk.** Een fix moet mogelijk naar twee releaselijnen
  propageren — dit is relevant voor hoofdstuk 9 (architectuurbeslissingen die backports
  bemoeilijken, zijn een reëel risico, zie hoofdstuk 11).

## 2.3 Normatieve randvoorwaarden (te bevestigen)

NL Portal verwerkt overheids-identiteit en BRP-gegevens. Een normenkader zoals de **BIO** (Baseline
Informatiebeveiliging Overheid) is voor dit type systeem gebruikelijk, maar **is niet geverifieerd
als onderdeel van deze review** — er is geen BIO-toetsingsdocument in de repository aangetroffen.
Dit is opgenomen als open vraag in hoofdstuk 11 (risico's), niet als vastgestelde randvoorwaarde.
