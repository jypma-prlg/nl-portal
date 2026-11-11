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
- **Omgevingsconfiguratie** — de set config-files die één deployment aan één gemeente bindt: thema,
  koppelingen en teksten. Wijzigen betekent een nieuwe uitrol, geen aanpassing in een draaiende
  omgeving. _Avoid_: configuration panel, instellingenbeheer.
- **Thema** — het uiterlijk van één gemeente in NL Portal: logo, stijl, favicon en paginatitel, zodat de
  portal herkenbaar is als die van de gemeente. _Avoid_: skin, branding.
- **Persistente state** — gegevens die bewaard blijven na een herstart en waarvoor een back-up of herstel
  nodig is: applicatieconfiguratie en gebruikersdata. _Avoid_: state (zonder bijvoeglijk
  naamwoord).
- **Vluchtige state** — gegevens die verloren mogen gaan zonder dataverlies, zoals een sessie of cache;
  uitval betekent hooguit opnieuw inloggen of een tragere respons. Geen back-up of herstel nodig.
  _Avoid_: state (zonder bijvoeglijk naamwoord).
- **Normatieve bron** — een extern of intern document (bijvoorbeeld OWASP ASVS 5.0, een RFC of het
  5-lagenmodel) waaraan NL Portal zich bewust toetst en waarvan per eis vastligt hoe die wordt afgedwongen
  of waarom niet. _Avoid_: standaard, richtlijn.

- **Tenant** — één overheidsorganisatie (meestal een gemeente; ook een gemeenschappelijke regeling,
  waterschap of provincie) als afnemer van de NL Portal-SaaS-dienst, met precies één portaal voor
  haar inwoners en ondernemers. Gegevens en configuratie van tenants zijn logisch van elkaar
  gescheiden. _Vermijd_: klant (dat is de burger/ondernemer), organisatie, instantie.
- **OIN (Organisatie-identificatienummer)** — het twintigcijferige nummer uit het OIN-register van
  Logius dat een tenant uniek identificeert. Bij een herindeling ontstaat een nieuwe organisatie met
  een nieuw OIN, en dus een nieuwe tenant. _Vermijd_: gemeentecode of CBS-code als tenantsleutel.
- **Cel** — een groep tenants die dezelfde NL Portal-runtime delen, afgeschermd van andere cellen; ook
  de eenheid waarin een release stapsgewijs wordt uitgerold. Een cel met één tenant is een eigen instantie
  voor die tenant. _Avoid_: cluster, shard, pool.
- **Tenantbestand** — de niet-geheime inrichting van één tenant in één omgeving: domeinen, koppelingen,
  thema en cel, met verwijzingen naar geheimen. Onderdeel van de omgevingsconfiguratie van die tenant.
- **Publieke ID-broker** — de Keycloak waarmee burgers en ondernemers inloggen; federeert naar DigiD,
  wallets en (alleen in acceptatie) lokale accounts.
- **Private ID-broker** — de Keycloak voor beheertoegang; federeert naar de Entra ID van Pink of van
  de gemeente. Nooit bedoeld voor burgers of ondernemers.

Zie ook `site/architectuur/arc42/12-glossarium.md` voor architectuurspecifieke
afkortingen (C1/C2/C3, ADR, SBOM, ...), en de `/codebase-design`-skill voor de architectuurvocabulaire
(module, interface, seam, ...) die in de architectuurreview wordt gebruikt.
