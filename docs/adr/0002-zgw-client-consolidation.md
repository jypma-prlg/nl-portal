# 2. Eén diepe ZGW-HTTP-client, i.p.v. één client per module

## Status

In uitvoering. Vastgelegd vóór de eerste implementatiestap (zie de bijbehorende spec,
[`0002-zgw-client-consolidation-spec.md`](0002-zgw-client-consolidation-spec.md)), zodat het besluit
vaststaat voordat er code verandert — niet achteraf gereconstrueerd.

## Context

De architectuurreview van 2026-10-20 (zie het bijgeleverde HTML-rapport, kandidaat 1 — de
top-aanbeveling) signaleerde dat elke ZGW-integratiemodule zijn eigen HTTP-client herimplementeert:
eigen `WebClient`-opbouw, eigen auth-token-injectie (`IdTokenGenerator()` handmatig aangemaakt
i.p.v. de al bestaande, genegeerde Spring-bean), en eigen foutafhandeling. Nader onderzoek tijdens
het grillen van deze beslissing (zie `docs/arc42/09-architectuurbeslissingen.md`, "kandidaat-ADR 1")
bracht aan het licht dat de situatie erger is dan "gedupliceerd maar consistent":

- **Vier verschillende foutafhandelingsvormen** bestaan naast elkaar: een uniforme
  404/401/500-mapping (`besluiten`, `zaken-api`, `openproduct`, `verificatie`), een catch-all met
  bericht (`objectenapi`), **helemaal geen foutafhandeling** (`catalogi-api` — een rauwe
  `WebClientResponseException` lekt door), en een generieke, inconsistent toegepaste
  try/catch-rethrow (`documenten-api`).
- **Geen van de clients parseert de response-body bij een fout** — alleen de HTTP-statuscode wordt
  gebruikt. De ZGW-API-foutrespons (doorgaans al in een RFC 9457/problem+json-achtige vorm) wordt nu
  weggegooid.
- **Vier byte-voor-byte identieke, los van elkaar gedefinieerde `ResultPage<T>`-klassen** bestaan in
  `besluiten`, `objectenapi`, `catalogi-api` en `documenten-api`.
- `zaken` bleek bij onderzoek geen eigen client te hebben — het is een pure orkestratielaag op
  `zaken-api`, `besluiten` en `catalogi-api`. Dus geen migratiedoel op zich, wel een module die
  automatisch meeprofiteert.
- Van de tien ZGW-clients in de backend heeft er vandaag precies één (`zaken-api`) een test die
  tegen een echte `MockWebServer` draait; de rest heeft geen test of alleen een servicelaag-test met
  de client weggemockt.

## Beslissing

1. Eén nieuwe, losse Gradle-module **`backend/zgw/zgw-client`** krijgt een **volledige generieke
   HTTP-client** (`AuthenticatedZgwClient`, `suspend inline fun <reified T> get/post/...`) —
   geen WebClient-factory, maar een module die `WebClient` volledig achter zijn seam houdt. Dit
   sluit aan bij het al bestaande `<reified T>`-patroon van `ObjectsApiClient`.
2. Eén uniform, **RFC 9457 (Problem Details)**-gebaseerd foutafhandelingsbeleid: de adapter
   parseert de upstream problem+json-body; levert de upstream geen geldige RFC 9457-body (niet-
   compliant backend, netwerkfout, proxy-502), dan synthetiseert de adapter er zelf één, zodat elke
   caller altijd exact dezelfde exceptievorm krijgt. De twee huidige afwijkende vormen
   (`catalogi-api` zonder foutafhandeling, `documenten-api`'s inconsistente rethrow) waren gaten,
   geen bewuste ontwerpkeuzes — unificeren is dus winst, geen verlies van functionaliteit.
3. De vier `ResultPage<T>`-kopieën worden samengevoegd tot één gedeelde generieke klasse in `core`.
   **Expliciete escape hatch**: mocht een van de vier domeinen op termijn een eigen paginatievorm
   nodig hebben die niet meer in één generieke klasse past, dan is het legitiem die weer te
   splitsen — dit besluit verplicht niet tot permanente samenvoeging tegen de zin van toekomstige
   vereisten in.
4. **Publieke interface van de vier domeinmodules verandert niet.** Alleen hun interne Client-klasse
   wordt vervangen door een aanroep op `AuthenticatedZgwClient`. Dit is het verdiepen van een
   bestaande implementatie achter een bestaande seam, geen herontwerp van de publieke interface.
5. **Scope**: `objectenapi`, `catalogi-api`, `zaken-api`, `documenten-api` (niet `zaken` — zie
   hierboven). Migratie verloopt **incrementeel**, één module per PR, te beginnen met **`zaken-api`**
   — de enige met een bestaande `MockWebServer`-test, die rechtstreeks bewijst dat de refactor het
   gedrag niet wijzigt.

## Consequenties

- **Locality**: een auth- of foutafhandelingsbug wordt op één plek gefixt, niet in (potentieel) tien
  bestanden.
- **Leverage**: een vijfde of zesde ZGW-integratie krijgt auth, foutafhandeling en paginatie gratis
  in plaats van dat iemand het patroon van de "minst slechte" bestaande client moet kopiëren.
- **Testbaarheid**: `zgw-client` krijgt twee echte adapters (de productie-`WebClient` en een
  `MockWebServer`-gebaseerde test-dubbel) — pas met twee adapters is de seam een echte seam, niet
  een hypothetische.
- **Gedragswijziging voor bestaande callers**: `catalogi-api` en `documenten-api` gaan een ander
  exceptietype gooien dan vandaag (nu: rauwe `WebClientResponseException` resp. generieke
  `RuntimeException`). Dit is bedoeld — de huidige vormen waren gaten — maar betekent dat bestaande
  callers/tests die op het oude exceptietype matchen, bij migratie van die specifieke module moeten
  worden nagelopen. Dit wordt per module gecontroleerd op het moment van migratie, niet in dit
  besluit vooraf uitgeput.
- **Dit besluit is nog niet (volledig) uitgevoerd** — zie de spec voor de concrete, nog te zetten
  stappen. Status wordt bijgewerkt naar "Geaccepteerd" zodra alle vier modules zijn gemigreerd.
