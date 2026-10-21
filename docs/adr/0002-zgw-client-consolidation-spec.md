# Spec: `zgw-client` — één diepe ZGW-HTTP-adapter

Volgt [`docs/workflows/feature-spec-template.md`](../workflows/feature-spec-template.md). Hoort bij
[ADR 0002](0002-zgw-client-consolidation.md). Geschreven om later opgepakt te worden — nog niet
geïmplementeerd. Kopieer de relevante secties in een issue in
[`nl-portal/nl-portal-issues`](https://github.com/nl-portal/nl-portal-issues) zodra je met de eerste
stap begint (zie `CONTRIBUTING.md`, "Open an issue first").

## Context / waarom

De architectuurreview van 2026-10-20 (kandidaat 1, top-aanbeveling) en het daaropvolgende grillen
(zie ADR 0002) brachten aan het licht dat de vier ZGW-integratiemodules met een eigen HTTP-client
(`objectenapi`, `catalogi-api`, `zaken-api`, `documenten-api`) niet alleen dupliceren, maar
daadwerkelijk **vier verschillende, deels kapotte foutafhandelingen** hebben — `catalogi-api` heeft
er helemaal geen, `documenten-api`'s rethrow is inconsistent per methode. Dit raakt
`docs/arc42/01-inleiding-en-doelen.md`'s kwaliteitsdoel "interoperabiliteit" (een vijfde ZGW-bron
toevoegen kost vandaag een copy-paste van het "minst slechte" bestaande patroon) en "testbaarheid"
(negen van de tien ZGW-clients hebben geen test).

## Doel

Na deze reeks PR's:

- Eén module, `backend/zgw/zgw-client`, biedt een generieke, geauthenticeerde HTTP-client
  (`AuthenticatedZgwClient`, `suspend inline fun <reified T> get/post/...`) met een uniform,
  RFC 9457-gebaseerd foutafhandelingsbeleid.
- `objectenapi`, `catalogi-api`, `zaken-api` en `documenten-api` gebruiken die client intern; hun
  publieke Service/Query-klassen zijn ongewijzigd.
- Eén gedeelde `ResultPage<T>` in `core` vervangt de vier huidige kopieën.
- Elke gemigreerde module heeft een `MockWebServer`-gebaseerde test van zijn client-gedrag.

## Niet-doelen

- `zaken`, `openklant`, `taak`, `berichten`, `verificatie`, `besluiten`, `openproduct` worden in
  deze reeks **niet** gemigreerd. (`zaken` heeft sowieso geen eigen client; de overige zes zijn een
  bewust uitgestelde fast-follow — zie ADR 0002, context.)
- Geen wijziging aan de publieke interface van de vier gemigreerde modules.
- Geen wijziging aan hoe NL Portal zelf fouten aan de klant toont (de generieke foutpagina, zie
  `docs/arc42/08-dwarsdoorsnede-concepten.md`) — dit is een interne-implementatie-verdieping, geen
  UX-wijziging.

## Geraakte bouwblokken

Nieuw: `backend/zgw/zgw-client` (zie `docs/arc42/05-bouwblokkenzicht.md`, nieuw onderdeel van de
"ZGW-integraties"-component in `docs/likec4`, container `backend`).

Gewijzigd (intern, publieke laag ongewijzigd): `backend/zgw/objectenapi`, `backend/zgw/catalogi-api`,
`backend/zgw/zaken-api`, `backend/zgw/documenten-api`. Plus `backend/core` (nieuwe gedeelde
`ResultPage<T>`, en de drie modules die hun eigen kopie laten vallen).

## Nieuwe externe integratie?

Nee — geen nieuw extern systeem, alleen een interne herstructurering van hoe bestaande ZGW-bronnen
worden aangeroepen. Het [threat-model-werkblad](../workflows/threat-model-template.md) is hier niet
van toepassing.

## Alternatieven overwogen

- **Alleen een WebClient-factory** (modules bouwen zelf nog requests/responses) — afgewezen:
  minder leverage, `WebClient` blijft lekken over de seam, en het bestaande `<reified T>`-patroon
  van `ObjectsApiClient` bewijst dat een volledige generieke client al haalbaar is in deze codebase.
- **Big-bang migratie van alle vier modules in één PR** — afgewezen: hoger risico, moeilijker te
  reviewen/terugdraaien. Incrementeel, startend met de module die al een test heeft, bewijst de
  vorm goedkoper.
- **Per-client overschrijfbare foutafhandeling behouden** — afgewezen: de huidige verschillen zijn
  gaten, geen bewuste keuzes; een uniform beleid is pure winst.

## Architectuurimpact

Vastgelegd in [ADR 0002](0002-zgw-client-consolidation.md) (status: in uitvoering). Geen conflict
met een bestaande ADR (dit is de tweede ADR van het project).

## Dependencies

Geen nieuwe externe dependency verwacht — RFC 9457-parsing kan met de al aanwezige Jackson-stack.
Als tijdens implementatie alsnog een library nodig blijkt (bijv. een kant-en-klare
`problem+json`-parser), doorloop dan de licentiecheck uit
`docs/workflows/dependency-updates.md`, sectie 3, vóór merge.

## Architectuurdocumentatie bijwerken (per stap, niet vooraf)

**Nog niet gedaan** — expres: `docs/likec4` en `docs/arc42` beschrijven de huidige structuur, geen
streefbeeld (zie `docs/likec4/README.md`). Zodra de eerste stap (`zaken-api`) daadwerkelijk landt,
in diezelfde PR:

- [ ] `docs/likec4/model.c4` / `components.c4`: nieuwe `component zgwClient` onder `nlPortal.backend`,
  plus een relatie vanaf `zakenApi` (en later de andere drie) naar `zgwClient`.
- [ ] `docs/likec4/views.c4`: `zgwClient` toevoegen aan de `backendComponents`-view (komt al mee via
  `include *`, dus waarschijnlijk geen wijziging nodig — controleer na het toevoegen van het component).
- [ ] `mise run likec4:validate` draaien.
- [ ] `docs/arc42/05-bouwblokkenzicht.md`: de alinea over de ZGW-integraties bijwerken — de
  "observatie uit de architectuurreview" is dan opgelost (voor de gemigreerde modules), niet meer
  alleen een signalering.
- [ ] `docs/arc42/11-risicos-en-technische-schulden.md`: rijen voor kandidaat 1 (en de
  `ResultPage<T>`-duplicatie) bijwerken van "risico" naar "opgelost, zie ADR 0002" zodra alle vier
  modules zijn gemigreerd.
- [ ] ADR 0002's status bijwerken naar "Geaccepteerd" zodra alle vier modules klaar zijn.
- [ ] `GLOSSARY.md`: **geen wijziging verwacht** — "zgw-client"/"AuthenticatedZgwClient" zijn
  architectuurnaamgeving (codebase-design-vocabulaire), geen domeinbegrip. Alleen bijwerken als
  tijdens implementatie alsnog een nieuw domeinbegrip nodig blijkt.

## Test- en uitrolplan

Per module, in volgorde:

1. **`zaken-api`** (spike): converteer `ZakenApiClientTest.kt` om tegen `AuthenticatedZgwClient` te
   testen i.p.v. de huidige client rechtstreeks; moet ongewijzigd slagen (bewijst gedragsgelijkheid).
   Voeg een `zgw-client`-eigen test toe (`MockWebServer`) voor de generieke `get<T>`/foutafhandeling
   zelf, los van `zaken-api`.
2. **`objectenapi`**: geen bestaande test om tegen te verifiëren — nieuwe `MockWebServer`-test
   schrijven die het huidige catch-all-gedrag eerst vastlegt (rood/groen: eerst de oude client
   testen, dan de nieuwe) vóór de migratie, zodat er wél een vangnet is.
3. **`catalogi-api`**: nieuwe `MockWebServer`-test — dit is de module waar de meeste winst zit
   (vandaag geen enkele foutafhandeling), dus hier concreet verifiëren dat een 404/401/500 nu een
   bruikbare exception oplevert i.p.v. een rauwe `WebClientResponseException`.
4. **`documenten-api`**: laatste, want meest afwijkend (meerdere backends per sleutel, optionele
   mTLS, voorwaardelijke token-injectie — zie de feiten in ADR 0002's context). Controleer expliciet
   of de mTLS- en multi-backend-ondersteuning een plek krijgt in `AuthenticatedZgwClient`'s interface
   of een documenten-api-specifieke extensie blijft.

Na elke stap: volledige `./gradlew build` (unit-tests) +, waar mogelijk, de integratietest tegen de
lokale docker-compose-stack (zie `CONTRIBUTING.md`) vóór het openen van de PR.
