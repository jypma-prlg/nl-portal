# 8. Dwarsdoorsnede-concepten

Concepten die meerdere bouwblokken overstijgen en dus niet in één bouwblok thuishoren.

## 8.1 Authenticatie en autorisatie

Zie [`site/architectuur/authentication-en-authorization.md`](../authentication-en-authorization.md).
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

- **Backend**: `./gradlew build` draait unit- en integratietests; integratietests hebben geen
  database nodig; alleen de `zgw:openklant`-tests starten via de Gradle docker-compose-plugin een
  OpenKlant-container (Docker moet dan draaien). Zie `CONTRIBUTING.md`.
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
1.2-header, afgedwongen door Spotless (backend) resp. een ESLint-regel + Husky (frontend). Bestanden
uit NL Portal houden de Ritense-header; nieuwe bestanden krijgen de PinkRoccade-header
([ADR 0015](../../development/adr/0015-copyright-headers-pink-edition.md)). Zie `CONTRIBUTING.md`
voor de exacte tooling.

## 8.8 Feature flags

Gedrag dat per omgeving of tenant moet kunnen verschillen, wordt gestuurd met feature flags
([ADR 0014](../../development/adr/0014-feature-flags-via-openfeature.md)). De backend evalueert flags
via de open standaard OpenFeature; `FeatureGate` (module `core`) is het enige aanspreekpunt voor
featurecode. Zonder aparte flag-voorziening gebruikt NL Portal een flagbestand (flagd-formaat) dat
periodiek wordt herladen; een andere OpenFeature-provider sluit je aan met een eigen bean.

- **Typen**: ops (runtime-gedrag), permission (toegang; bij twijfel geweigerd), experiment en release
  (kortlevend). Flagsleutels hebben het formaat `<type>.<module>.<naam>`.
- **Evaluatiecontext**: `FeatureContextFactory` bouwt per request een context (tenant, omgeving,
  gebruikerstype en een opaque gebruikers-id: HMAC van het subject met een geheime sleutel per tenant)
  uit de reactive security context. De context wordt expliciet aan elke evaluatie meegegeven, niet
  thread-gebonden opgeslagen, en bevat nooit persoonsgegevens.
- **Frontend**: de uitkomst voor de frontend komt via `GET /api/public/frontend`; de browser praat
  nooit met een flag-systeem.
- **Nu flag-gedreven**: alleen de toggle `messageCountEnabled` (`ops.frontend.message-count`). De
  overige toggles en de module-activatie volgen (#143).
- **Grens**: credentials en andere secrets horen niet in een flag-systeem.
