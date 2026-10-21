# Spec: OpenProduct propagate-by-default foutbeleid

Volgt [`docs/workflows/feature-spec-template.md`](../workflows/feature-spec-template.md). Hoort bij
[ADR 0003](0003-openproduct-failure-policy.md). Geschreven om direct opgepakt te worden. Kopieer de
relevante secties in een issue in [`nl-portal/nl-portal-issues`](https://github.com/nl-portal/nl-portal-issues)
zodra je met de eerste stap begint (zie `CONTRIBUTING.md`, "Open an issue first").

## Context / waarom

Zie [ADR 0003](0003-openproduct-failure-policy.md) — de architectuurreview van 2026-10-21 (ronde 2,
kandidaat 1) en het daaropvolgende grillen stelden vast dat `OpenProductService`'s 31x
swallow-and-return-empty-patroon niet alleen shallow is, maar al één live bug heeft gemaskeerd als
"onbereikbare Sonar-code" (PR #21). Raakt `docs/arc42/10-kwaliteitseisen.md`'s testbaarheidsdoel
(het foutpad van `OpenProductService` heeft vandaag nul dekking).

## Doel

Na deze PR:

- `OpenProductService.kt`, `OpenProductDmnService.kt` en `OpenProductPrefillService.kt` hebben één
  gedeeld, expliciet foutbeleid in plaats van 31 losse varianten.
- Echte fouten (niet "geen resultaat") propageren naar `GraphQLExceptionHandler` i.p.v. te worden
  geslikt.
- Elke categorie (get-by-id, lijst, loop) heeft minimaal één test die het beleid voor die categorie
  vastlegt.

## Niet-doelen

- `backend/product` (de gedeprecieerde voorganger) wordt **niet** in deze PR aangeraakt — zie de
  aparte opruim-PR (sectie "Verwante, losse PR" hieronder).
- Geen nieuw exception-type — de rauwe `WebClientResponseException` propageert ongewijzigd (zie ADR
  0003, Beslissing punt 4).
- Geen wijziging aan ADR 0002's scope (`objectenapi`/`catalogi-api`/`zaken-api`/`documenten-api`) —
  dit raakt uitsluitend `openproduct`'s eigen servicelaag, een laag boven de HTTP-client.
- Geen wijziging aan kandidaat 3 uit de architectuurreview (de 11 GraphQL `*Page.kt`-wrappers) — zie
  die kandidaat apart.

## Geraakte bouwblokken

`backend/zgw/openproduct` (`service/OpenProductService.kt`, `service/OpenProductDmnService.kt`,
`service/OpenProductPrefillService.kt`, plus een nieuwe interne helper, bijv.
`service/OpenProductFailurePolicy.kt`). Geen publieke-interfacewijziging van buiten de module: de
GraphQL-schema-vorm blijft gelijk, alleen het foutpad verandert van "stil" naar "zichtbaar".

## Nieuwe externe integratie?

Nee.

## Alternatieven overwogen

- **Alles laten swallowen, alleen centraliseren (geen gedragswijziging)** — afgewezen: dit had het
  PR #21-type bug niet voorkomen; de hele winst van deze verdieping zit in het zichtbaar maken van
  echte fouten, niet alleen in het dedupliceren van identieke code.
- **Een nieuw `Result<T>`/`Either`-type introduceren** — afgewezen (zie ADR 0003, punt 4): geen
  bestaande conventie in de backend, en de bestaande `GraphQLExceptionHandler` maakt het overbodig —
  exceptions zijn hier al een eersteklas, ondersteunde foutvorm.
- **Ook de 3 loop/verrijkingsmethoden volledig laten propageren (geen skip-en-log meer)** —
  afgewezen: dat zou een UX-regressie zijn voor een burgerportaal (één falende gerelateerde taak zou
  de hele lijst laten mislukken in plaats van 9 van de 10 te tonen).
- **404-als-leeg ook voor lijst-methoden behouden** — afgewezen: geen bewijs dat de API dit ooit
  doet; het zou een niet-bestaand geval beschermen ten koste van echte fouten die daardoor alsnog
  stil blijven.

## Architectuurimpact

Vastgelegd in [ADR 0003](0003-openproduct-failure-policy.md) (status: in uitvoering). Geen conflict
met ADR 0002 (andere laag, andere modules — zie Niet-doelen).

## Dependencies

Geen nieuwe dependency.

## Architectuurdocumentatie bijwerken (per stap, niet vooraf)

- [ ] `docs/arc42/10-kwaliteitseisen.md`: testbaarheidsscenario toevoegen/bijwerken zodra de PR
  landt (foutpad van `openproduct`'s servicelaag is nu gedekt).
- [ ] `docs/arc42/11-risicos-en-technische-schulden.md`: rij voor dit kandidaat bijwerken van
  "risico" naar "opgelost, zie ADR 0003".
- [ ] ADR 0003's status bijwerken naar "Geaccepteerd" zodra zowel deze PR als de
  `backend/product`-opruim-PR zijn gemerged.
- [ ] `GLOSSARY.md`: geen wijziging verwacht — dit is architectuurvocabulaire (codebase-design), geen
  domeinbegrip.
- [ ] `docs/likec4`: geen wijziging verwacht — geen nieuw bouwblok, alleen intern gedrag van een
  bestaand component.

## Test- en uitrolplan

Per categorie, TDD (rood/groen), in één PR (de categorieën horen bij hetzelfde ontwerp, zijn niet
onafhankelijk inzetbaar):

1. **Get-by-id (12x)**: nieuwe `OpenProductServiceTest.kt`. Rood: test die aantoont dat een
   niet-404-fout (bijv. een 500 of connectie-fout) vandaag stil tot `null` leidt. Groen: na de
   gedeelde helper, dezelfde fout propageert; een losse test bevestigt dat een upstream-404 nog
   steeds naar `null` vertaalt.
2. **Lijst (15x)**: rood/groen-paar dat aantoont dat een fout vandaag tot een lege `ResultPage`/lege
   lijst leidt, en na de fix propageert.
3. **Loop/verrijking (3x)**: test die bevestigt dat één falend item wordt overgeslagen (gelogd) maar
   de overige resultaten van de lijst behouden blijven — dit gedrag blijft ongewijzigd, de test
   legt het nu expliciet vast als bewust beleid in plaats van toevallig.
4. **`getDecision`**: meeverhuisd naar de lijst-categorie (punt 2) — eigen rood/groen-paar of
   hergebruik van hetzelfde patroon als de overige lijst-methoden.
5. **`getProduct` / `getProductDocumentContent`**: test die aantoont dat hun bestaande (deels al
   juiste) gedrag intact blijft na overzetten op de gedeelde helper — geen gedragswijziging
   verwacht, wel een regressietest voor de zekerheid.

Na implementatie: volledige `./gradlew build -x integrationTest` (unit-tests); integratietest tegen
de lokale docker-compose-stack waar mogelijk (zie `CONTRIBUTING.md`) vóór het openen van de PR.

## Verwante, losse PR

Verwijdering van de gedeprecieerde `backend/product`-module (23 klassen, alle `@Deprecated`, nergens
anders geïmporteerd, beans nergens actief geconfigureerd) inclusief de 5 al dode frontend
GraphQL-query/mutation-bestanden die er wees van worden. Dit is een apart onderwerp (verwijdering,
geen gedragsrefactor) en landt als eigen PR — zie ADR 0003, Beslissing punt 8. Verwijdert ook de 4
resterende open `kotlin:S6611`-Sonar-bevindingen.
