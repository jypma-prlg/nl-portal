# 8. Dwarsdoorsnede-concepten

Concepten die meerdere bouwblokken overstijgen en dus niet in één bouwblok thuishoren.

## 8.1 Authenticatie en autorisatie

Zie [`documentation/fundamentals/architectuur/authentication-en-authorization.md`](../../documentation/fundamentals/architectuur/authentication-en-authorization.md).
Samengevat: DigiD (burgers) en eHerkenning (ondernemers/ketenpartners), gefedereerd via een OIDC-
broker (Keycloak in de huidige stack). De backend valideert het token op elke GraphQL-aanroep; de
frontend bewaart geen eigen sessie-state buiten wat de OIDC-flow vereist
(`@nl-portal/nl-portal-authentication`).

## 8.2 Het ZGW-integratiepatroon

Elke ZGW-bron wordt vandaag ontsloten via een eigen Gradle-module met een eigen HTTP-client, eigen
auth-token-injectie en eigen statuscode-afhandeling (zie hoofdstuk 5.2 en de architectuurreview,
kandidaten 1-2). **Voor nieuwe functionaliteit die een nieuwe of uitgebreide ZGW-integratie behelst,
betekent dit vandaag: kopieer het patroon van een bestaande, vergelijkbare module.** Zodra de
review-kandidaat "één diepe HTTP-adapter" is doorgevoerd, verandert dit in: implementeer alleen de
resource-specifieke aanroepen tegen de gedeelde adapter.

## 8.3 Generieke foutafhandeling

Een generieke foutpagina is recent toegevoegd (zie `feat: generic error page` in de git-historie).
**Niet geverifieerd in deze review:** of er ook een generiek, gedeeld patroon bestaat voor
GraphQL-errors (error codes, client-vertaalbare foutmeldingen) — dit verdient een expliciete check
voordat nieuwe functionaliteit een eigen ad-hoc foutconventie introduceert.

## 8.4 Internationalisatie

`@nl-portal/nl-portal-localization` verzorgt vertalingen; componenten gebruiken translation-ID's
(bijv. `pageTitles.${slug}Mutate`, zichtbaar in de architectuurreview-kandidaat over
`ThemeMutatePage`/`ThemeHistoryPage`). Nieuwe klant-gerichte tekst hoort via een translation-ID te
lopen, niet als hardcoded string.

## 8.5 Testen

- **Backend**: `./gradlew build` draait unit- en integratietests; integratietests provisionen een
  database via de Gradle docker-compose-plugin (Docker moet lokaal draaien). Zie `CONTRIBUTING.md`.
- **Frontend**: `pnpm test` (vitest, workspace-breed via `vitest.workspace.ts`), `pnpm lint`,
  `pnpm prettier`.
- **Observatie uit de architectuurreview**: de ZGW-HTTP-clients hebben vandaag geen test-dubbel
  (geen tweede adapter), dus geen unit test die een foutscenario simuleert zonder een live ZGW-API.
  Dit is zowel een hoofdstuk-10-testbaarheidsrisico als hoofdstuk-11-technische-schuld.

## 8.6 Kwaliteits- en beveiligingspoorten in CI

- **SonarCloud** (`sonarcloud.yml`) — codekwaliteit or draait niet op fork-PR's (zie
  `fix(ci): don't run sonar scans from fork PRs`).
- **OWASP Dependency-Check** (`dependency-check.yml`, met uitzonderingen in
  `backend/cve-exclusions.xml`) — bekende kwetsbaarheden in dependencies.
- **Renovate** (`.github/renovate.json`) — geautomatiseerde dependency-updates (zie deliverable 4).
- **SBOM-generatie** bestaat al voor de backend-release (`anchore/sbom-action` in
  `_stage-be.yml`/`_publish-be-native.yml`, CycloneDX-JSON naar S3) maar is backend- en
  release-only, en nergens gedocumenteerd als ontwikkelaarsworkflow (zie deliverable 5).

## 8.7 Copyright-headers

Elk backend- (`.kt`/`.kts`) en frontend-bestand (`.ts`/`.tsx`/`.js`/`.jsx`) draagt een EUPL
1.2-header, afgedwongen door Spotless (backend) resp. een ESLint-regel + Husky (frontend). Nieuwe
bestanden moeten deze header krijgen — zie `CONTRIBUTING.md` voor de exacte tooling.
