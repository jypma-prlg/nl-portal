# 3. OpenProduct: propagate-by-default foutbeleid i.p.v. 31x swallow-and-return-empty

## Status

Geaccepteerd. Beide PR's zijn gemerged: de foutbeleid-refactor
([PR #26](https://github.com/jypma-prlg/nl-portal/pull/26)) en de verwijdering van de
gedeprecieerde `backend/product`-module ([PR #27](https://github.com/jypma-prlg/nl-portal/pull/27),
Beslissing punt 8). Zie de bijbehorende spec,
[`0003-openproduct-failure-policy-spec.md`](0003-openproduct-failure-policy-spec.md).

## Context

De architectuurreview van 2026-10-21 (ronde 2, kandidaat 1 — de top-aanbeveling) signaleerde dat
`OpenProductService.kt` (1465 regels, ~25 publieke methoden: `getThemas`, `getProductTypes`,
`getActies`, `getBestanden`, `getContacten`, `getLocaties`, `getOrganisaties`, `getPrijzen`,
`getSchemas`, `getLinks`, `getProducten`, ...) 31 keer hetzelfde patroon herhaalt: bouw filter →
delegate naar `openProductTypeClient` → `try { ... } catch (e: Exception) { log.error(...); return
leeg/null/emptyList }`. Dit maakt de module shallow — de interface is bijna even groot als de
implementatie — en zonder locality: elke caller moet zelf gissen of een leeg resultaat "echt leeg"
of "mislukt" betekent.

Nader onderzoek tijdens het grillen van deze beslissing bracht aan het licht dat dit geen
onschuldige stijlkwestie is:

- Commit `b9ef5c25f` (PR #21) repareerde precies dit patroon in
  `OpenProductDmnService.getDecision`: een onbeveiligde `!!` gooide een `NullPointerException` die
  door de eigen `catch (e: Exception)` stilzwijgend tot een lege decision-lijst werd, waardoor
  SonarCloud de regel als "onbereikbare code" (`kotlin:S6611`) classificeerde terwijl het een live
  bug was — bewezen met een TDD-test (rood vóór de fix, groen erna). Het swallow-patroon is dus niet
  alleen duplicatie, maar het mechanisme dat een echte bug maskeerde.
- Van de 25+ methoden gebruiken 10 `ResultPage(count=0, results=emptyList())`, 12 `null`, 5
  `emptyList()`, en 3 een per-item-swallow-in-een-`forEach`-lus; 2 methoden (`getProduct`,
  `getProductDocumentContent`) wijken al af met gedeeltelijk bewuste rethrow-logica; 6 methoden
  hebben helemaal geen try/catch. Dit zijn dus minstens vier losse, nooit expliciet gekozen
  varianten van hetzelfde beleid.
- Geen van de 31 plekken onderscheidt een upstream-404 (potentieel legitiem "niet gevonden") van
  elke andere fout (500, netwerkfout, malformed data) — alles wordt identiek geslikt.
- Er bestaat al een globale GraphQL-foutafhandelaar, `GraphQLExceptionHandler`
  (`backend/graphql/.../GraphQLExceptionHandler.kt`, implementeert
  `DataFetcherExceptionResolverAdapter`), die een gegooide exception al netjes omzet in een
  gestructureerde GraphQL-fout — maar wordt door dit swallow-patroon nooit bereikt vanuit
  `openproduct`.
- Geen test legt het huidige swallow-gedrag vast als contract (`OpenProductServiceTest.kt` bestaat
  niet): het is dus vrij om zonder regressie te veranderen.
- Er bestaat nergens anders in de backend al een Result/Either-achtig patroon — dit zou de eerste
  introductie van zo'n constructie zijn, en is dus bewust *niet* gekozen (zie Beslissing, punt 4).

## Beslissing

1. **Default wordt propagate, niet swallow.** Een methode die een echte fout tegenkomt (niet
   "geen resultaat") laat die fout voortaan doorstromen naar de al bestaande
   `GraphQLExceptionHandler`, in plaats van hem te loggen en een lege/`null`-waarde terug te geven.
2. **Alleen upstream-404 op de 12 get-by-id-methoden is een legitieme "niet gevonden"**, en wordt
   als enige geval nog naar `null` vertaald. Alle overige fouten op deze methoden (500, netwerkfout,
   malformed data) propageren.
3. **De 15 lijst-methoden propageren altijd, zonder uitzondering.** Er is geen bewijs dat de
   OpenProduct-API ooit 404 geeft voor "geen matches" (REST-conventie: 200 + lege array); een 404 op
   een lijst-endpoint is dus een echte fout, geen legitiem "niets gevonden".
4. **Geen nieuw exception-type.** De rauwe `WebClientResponseException` (of wat `WebClient` anders
   gooit) propageert ongewijzigd naar `GraphQLExceptionHandler`. Een eigen foutvorm (zoals ADR
   0002's RFC 9457-aanpak voor de ZGW-HTTP-laag) is hier bewust *niet* gekozen: dat zou een nieuwe
   conventie invoeren voor een module die daar vandaag geen enkele aanleiding toe geeft. Als de
   foutboodschap ooit echt tekortschiet voor GraphQL-clients, is dat een losse vervolgstap.
5. **De 3 loop/verrijkingsmethoden (`getProductZaken`, `getProductTaken`,
   `getOpenProductDocumenten`) behouden hun skip-en-log-gedrag**, maar voortaan als expliciet
   benoemde, bewuste uitzondering (eigen helper, geen stilzwijgende restant van het oude patroon):
   bij het doorlopen van een lijst gerelateerde objecten is "sla het falende item over, toon de
   rest" een bewuste resilience-keuze voor een burgerportaal (liever 9 van de 10 documenten tonen
   dan een foutpagina).
6. **Eén gedeelde helper draagt het propagate/404-naar-null-onderscheid**, gebruikt door alle 12
   get-by-id-methoden én de twee eerder afwijkende methoden (`getProduct`,
   `getProductDocumentContent`), zodat er werkelijk één seam is in plaats van losse varianten.
   `OpenProductDmnService.getDecision` (lijst-vormig) valt onder punt 3 (volledige propagatie), niet
   onder deze helper.
7. **Scope**: alleen `backend/zgw/openproduct` (`OpenProductService`, `OpenProductDmnService`,
   `OpenProductPrefillService`). De gedeprecieerde `backend/product`-module (zie punt 8) wordt niet
   meeverdiept — die module verdwijnt in zijn geheel, los van deze beslissing.
8. **`backend/product` wordt volledig verwijderd**, in een aparte PR, los van dit foutbeleid.
   Bevestigd veilig: alle 23 klassen zijn `@Deprecated("Use instead openproduct functionality")`,
   geen enkele wordt elders in de backend geïmporteerd, en de bijbehorende Spring-beans worden in
   geen enkele huidige configuratie geactiveerd (`open-product-enabled: true` is de enige relevante
   vlag). Dit verwijdert tevens de 4 resterende open `kotlin:S6611`-Sonar-bevindingen, die
   uitsluitend in deze module zitten. De 5 bijbehorende, al niet-geïmporteerde frontend
   GraphQL-query/mutation-bestanden (`frontend/packages/api/src/queries/{get-producten,
   get-product-taken,get-product-zaken,get-product-verbruiks-objecten}.ts` en de bijbehorende
   mutation) worden in dezelfde PR meegenomen.

## Consequenties

- **Locality**: het onderscheid "wat betekent een fout hier" wordt op één plek bepaald (de gedeelde
  helper + drie expliciete categorieën: get-by-id, lijst, loop), niet 31 keer apart.
- **Leverage**: een 26e query-methode op `openproduct` erft correct foutgedrag in plaats van het
  "minst slechte" bestaande patroon te kopiëren.
- **Testbaarheid**: `OpenProductService` krijgt voor het eerst een eigen testsuite die het
  foutpad daadwerkelijk vastlegt (rood/groen per categorie), in plaats van nul dekking op dat pad.
- **Gedragswijziging voor bestaande callers**: GraphQL-consumenten van `openproduct`'s queries zien
  voortaan een structured GraphQL-fout waar ze vandaag stilzwijgend `null`/lege data kregen. Dit is
  bedoeld — de huidige stilte verborg al minstens één live bug — maar is een caller-zichtbare
  wijziging, vandaar deze ADR vóór implementatie.
- **Uitgevoerd**: zowel de foutbeleid-refactor (PR #26) als de verwijdering van
  `backend/product` (PR #27) zijn gemerged in `main`.
