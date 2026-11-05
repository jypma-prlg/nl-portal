# Domeinwoordenlijst — NL Portal

Canonieke betekenis van domeintermen zoals gebruikt in code, commit messages en architectuurdocumentatie
(`site/architectuur/arc42`, `site/likec4`). Scherp een term aan zodra de term in gebruik verschuift van wat hier
staat; voeg een nieuwe term toe zodra een module ernaar vernoemd wordt (zie de
`improve-codebase-architecture`- en `domain-modeling`-skills).

- **Burger / ondernemer** — de klant: een natuurlijk persoon of organisatie die een verzoek indient of
  heeft ingediend, en inzicht krijgt via NL Portal.
- **Ketenpartner** — een organisatie die bijdraagt aan de afhandeling van een verzoek (niet de klant,
  niet NL Portal zelf).
- **Zaak** — de ZGW-eenheid die de afhandeling van één verzoek representeert (status, resultaat,
  betrokkenen, documenten). Beheerd in Open Zaak, getoond door NL Portal.
- **Verzoek** — de informatie en trigger die mogelijk leidt tot een product of dienst; wordt
  *niet* door NL Portal zelf ingediend (dat doet een formulierencomponent of vergelijkbaar), maar wel
  door NL Portal getoond. Zie `site/architectuur/patronen/verzoek.md`.
- **Externe taak** — een verzoek aan de klant/ketenpartner om (aanvullende) informatie, documenten of
  een betaling te leveren tijdens zaakafhandeling. Zie
  `site/architectuur/patronen/externe-taak.md`.
- **Besluit** — een formeel genomen beslissing binnen een zaak (ZGW Besluiten-API).
- **Common Ground** — de Nederlandse gemeentelijke architectuurbeweging waarop NL Portal is gebaseerd:
  data gescheiden van applicaties, componenten in plaats van monolieten.
- **5-lagenmodel** — het Common Ground-gelaagde model; NL Portal is laag 5 (interactielaag), zie
  `site/architectuur/arc42/04-oplossingsstrategie.md`.
- **ZGW (Zaakgericht Werken)** — de VNG-API-standaard-familie (Zaken, Catalogi, Documenten, Besluiten,
  Klantinteracties, Objecten) waarop NL Portal's backend-integraties zijn gebouwd.
- **BRP (Basisregistratie Personen)** — de landelijke persoonsregistratie; ontsloten via Haal Centraal
  BRP.
- **DigiD / eHerkenning** — respectievelijk de identiteitsvoorziening voor burgers en voor
  ondernemers/organisaties; gefedereerd via Keycloak in de huidige stack (zie
  `site/architectuur/arc42/08-dwarsdoorsnede-concepten.md`).
- **Machtiging** — het optreden namens een ander (burger voor burger, medewerker voor organisatie);
  relevant voor Haal Centraal Bewoning-gegevens.
- **Mijn Omgeving** — de productcategorie waartoe NL Portal behoort: een persoonlijk
  overheids-portaal voor klanten.
- **Feature flag** — een schakelaar die het gedrag van NL Portal stuurt vanuit externe
  configuratie in plaats van uit de code. Vier typen: *ops* (runtime-gedrag), *permission* (toegang;
  bij twijfel geweigerd), *experiment* en *release* (kortlevend). NL Portal werkt ook zonder
  aparte flag-voorziening (zie issue #143).

Zie ook `site/architectuur/arc42/12-glossarium.md` voor architectuurspecifieke
afkortingen (C1/C2/C3, ADR, SBOM, ...), en de `/codebase-design`-skill voor de architectuurvocabulaire
(module, interface, seam, ...) die in de architectuurreview wordt gebruikt.
