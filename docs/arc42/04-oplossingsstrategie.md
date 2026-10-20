# 4. Oplossingsstrategie

Dit hoofdstuk benoemt de fundamentele keuzes die het systeem vormgeven, en waarom — niet de
uitwerking (die staat in hoofdstuk 5 e.v.).

## 4.1 Positie in het Common Ground 5-lagenmodel

NL Portal is bewust **laag 5 (interactielaag)**: het bevat geen eigen "bron"-data, maar haalt die
op bij de datalaag (primair ZGW-bronnen). Dit volgt direct uit twee Common Ground-uitgangspunten
(zie [`documentation/fundamentals/architectuur/README.md`](../../documentation/fundamentals/architectuur/README.md)):
componenten in plaats van monolieten, en data gescheiden van business-logica. Consequentie: NL
Portal's eigen database (zie `docs/likec4`, container `database`) bevat vrijwel uitsluitend
portal-eigen state (autorisatie, formulierinzendingen, caches) — geen kopie van brondata die elders
leidend is.

## 4.2 Eén backend als enige ZGW-cliënt

Alleen de backend praat met het ZGW-landschap; de frontend praat uitsluitend GraphQL met de eigen
backend. Dit geeft één plek voor autorisatie, foutafhandeling en credential-beheer richting externe
systemen, in plaats van dat elke SPA-pagina zijn eigen ZGW-aanroepen doet.

## 4.3 Eén module per ZGW-bron (backend)

De backend is opgeknipt in Gradle-modules die grotendeels 1-op-1 corresponderen met een ZGW-bron of
-API (`zgw:zaken-api`, `zgw:openklant`, `zgw:besluiten`, `zgw:documenten-api`, ...). Dit volgt het
"component in plaats van monoliet"-principe door naar binnen toe: een bron kan (in theorie)
losgekoppeld worden zonder de andere modules te raken. De architectuurreview (deliverable 1,
zie [`docs/adr`](../adr) voor eventuele vervolgbeslissingen) signaleert dat deze modules sterk op
elkaar lijken in vorm — dat is een kandidaat voor een gedeelde onderliggende abstractie, maar is
hier bewust nog niet als vaststaande beslissing opgenomen.

## 4.4 GraphQL als extern contract van de backend

De backend biedt GraphQL (`backend/graphql`) als contract naar de frontend, in plaats van losse
REST-resources. Dit past bij een UI die uiteenlopende, samengestelde weergaven nodig heeft (zaak +
status + documenten in één scherm) zonder dat de backend een eigen BFF-laag per scherm hoeft te
bouwen.

## 4.5 Gedeelde React-bibliotheken, twee apps

Eén pnpm-workspace met herbruikbare `@nl-portal/*`-packages (`user-interface`, `api`,
`authentication`, `localization`) die door twee apps gebruikt worden: de **shippable app**
(`frontend/packages/app`, productie-vorm) en de **dev app** (`frontend/packages/app-dev`, alle
modules aan plus demodata, nooit gepubliceerd). Dezelfde scheiding bestaat aan backend-zijde
(`:app` vs. `:app-dev`). Dit maakt snelle feature-ontwikkeling mogelijk zonder de productieconfiguratie
te vervuilen met demo-only functionaliteit.

## 4.6 Build- en kwaliteitskeuzes

- **Conventional Commits + squash-merge**: de PR-titel is de changelog-eenheid; ondersteunt
  geautomatiseerde release notes.
- **Renovate** voor dependency-updates (niet-major automatisch gebatcht, major handmatig) — zie
  deliverable 4.
- **SonarCloud + OWASP Dependency-Check** als kwaliteits-/beveiligingspoort in CI — zie hoofdstuk 8
  en deliverable 6.
