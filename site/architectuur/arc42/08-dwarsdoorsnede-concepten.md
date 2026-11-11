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

- **Ingang**: `mise run test`, `mise run lint` en `mise run arch-check` vanaf de repo-root dekken
  backend en frontend tegelijk; `mise tasks` toont de taken per kant. Zie `CONTRIBUTING.md`.
- **Backend**: `mise run backend:test` draait de unit-tests, `mise run backend:integration-test` de
  integratietests; die hebben geen database nodig; alleen de `zgw:openklant`-tests starten via de
  Gradle docker-compose-plugin een OpenKlant-container (Docker moet dan draaien). De CI-poort
  `mise run backend:check` draait beide via `./gradlew build`.
- **Frontend**: `mise run frontend:test` (vitest, workspace-breed via `vitest.workspace.ts`) en
  `mise run frontend:lint` (ESLint en Stylelint); de CI-poort `mise run frontend:check` voegt
  build, typecheck en Prettier toe.
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
  `_stage-be.yml`, CycloneDX-JSON als GitHub-Release-asset) maar is backend- en
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

## 8.9 Tenantscheiding en configuratiemodel (voorgesteld)

> **Voorgesteld, nog niet gebouwd.** Uitgewerkt in
> [ADR 0017](../../development/adr/0017-gedeelde-multi-tenant-runtime-met-cellen.md). Vandaag is NL Portal één
> tenant per deployment en geldt het beeld uit 8.1 en 8.8 ongewijzigd; dit is het doelbeeld voor een gedeelde
> runtime.

- **Tenantresolutie.** Het hostdomein bepaalt de tenant (allowlist van domein naar OIN; een onbekend domein
  geeft 404, nooit een standaardtenant). De tenant leeft in de Reactor Context, niet in een `ThreadLocal`.
  Bij een geauthenticeerd request moet de issuer van het token bij dezelfde tenant horen, anders 403: de
  issuer is de tenantbinding, er komt geen aparte tenant-claim bij.
- **Identiteit.** Een realm per tenant op de publieke ID-broker, gegenereerd uit één template. De runtime
  vertrouwt alleen issuers uit de tenantconfiguratie en nooit de private ID-broker (zie ook 8.1).
- **Per-tenant clientregister.** Een onveranderlijk register `OIN naar client`, bij opstarten gebouwd uit het
  tenantbestand en ingebouwd in de `zgw-client`-seam van [ADR 0002](../../development/adr/0002-zgw-client-consolidation.md).
  Architectuurchecks (`backend/architecture-test`) bewaken dat services het register gebruiken en geen
  losse clients, globale stores of nieuwe coroutine-scopes.
- **Drie lagen configuratie.** Het *tenantbestand* (Git: domeinen, URL's, thema, verwijzingen naar secrets;
  rolling restart), de *flag* (alleen aan/uit per module en tenant; live) en de *secret store* (de secrets
  zelf, via ESO). Een module is bruikbaar als configuratie aanwezig is én de flag aanstaat. Zie
  [Tenantbestand, flag en secret store](../../hosting-en-configuratie/tenantbestand-flag-en-secret-store.md).
- **Geen hot reload, één uitzondering.** Wijzigingen in tenantconfiguratie en secrets gaan altijd via een
  rolling restart van de eigen cel. De enige live wijziging is de flagd-polling van het flagbestand
  (8.8), uitsluitend voor aan/uit-schakelaars; flags dragen geen configuratie en geen beveiligingsbesluiten.
- **Logging.** De tenant (OIN) hoort in elke logregel (BIO 8.15.01, ASVS 16.2.1); de uitwerking van
  observability per tenant is nog niet besloten.
