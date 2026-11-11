# Onderzoek #145: waarom vereist openproduct vijf andere modules?

Onderdeel van wayfinder-kaart #379 (doel: NL Portal start zonder alle afhankelijkheden) en voortkomend uit #143.
Alle bronverwijzingen zijn paden onder `backend/zgw/` op `main` (ea58815d1).

## Vraag

`OpenProductAutoConfiguration` is alleen actief als `openproduct`, `objectenapi`, `taak`, `zakenapi` en
`documentenapis` allemaal aanstaan. Welke classes/beans vereisen welke module, en is dat een echte technische
afhankelijkheid of een historische aanname?

## Kort antwoord

De vijf flags zijn grotendeels een **historische aanname plus Gradle- en bean-koppeling**, geen noodzaak van het
openproduct-domein zelf. De kern (thema's, producttypen, producten, acties, bestanden, contacten, locaties,
organisaties, prijzen, schema's, links, DMN) praat alleen met de Open Product API en met
`AuthenticationMachtigingsDienstService`. Drie functies hebben echt een andere module nodig (zaken, taken,
documenten), een vierde (prefill) heeft de Objecten API nodig. Die vier zijn elk afzonderlijk optioneel te maken.

## Hoe de huidige koppeling werkt

- `openproduct/autoconfigure/OpenProductAutoConfiguration.kt` zet `@ConditionalOnProperty` met de vijf flags op
  de hele klasse. Eén uitgezette flag schakelt dus alle openproduct-beans uit, inclusief
  `OpenProductClient`, `OpenProductTypeClient` en `OpenProductService`.
- `OpenProductGraphqlAutoConfiguration` is `@ConditionalOnBean(OpenProductService::class)`, dus ook de
  GraphQL-queries verdwijnen mee.
- `openproduct/build.gradle.kts` heeft `api(project(":zgw:zaken-api"))`, `api(project(":zgw:taak"))` en
  `api(project(":zgw:documenten-api"))`. De Objecten API komt alleen transitief binnen (via taak/zaken-api).
- Historie: de eerste versies (7465147a1, 3d9118414, fef242717; mei 2025) hadden alleen
  `nl-portal.config.openproduct.enabled`. De vijf-flags-conditie is later toegevoegd (laatste wijziging
  d9ae0c518, aug 2025), toen thema-zaken, thema-taken en productdocumenten erbij kwamen. Het is dus gegroeid, niet
  ontworpen.

## Per afhankelijkheid

### 1. zakenapi

- **Gebruikt door:** `OpenProductService.getThemaZaken` (r. ~799-860) en `getProductZaken` (r. ~935);
  `ZakenApiService` is constructorparameter van `OpenProductService`. GraphQL-velden `OpenProductThema.zaken`,
  `OpenProductProduct.zaken` en query `getOpenProductThemaZaken` (`OpenProductThemaQuery`, `OpenProductQuery`)
  retourneren het type `Zaak` (`nl.nlportal.zakenapi.domain.Zaak`).
- **Echte dependency?** Ja, maar smal: twee service-methoden en drie GraphQL-velden. Geen ander openproduct-pad
  raakt zaken.
- **Optioneel te maken?** Ja. `ZakenApiService` als `ObjectProvider`/nullable injecteren; velden `zaken` geven
  bij afwezigheid `null`/lege lijst (in het schema al nullable `[Zaak!]`, behalve `getOpenProductThemaZaken`
  dat `[Zaak!]!` is).
- **Let op:** `ZakenApiAutoConfiguration` eist zelf `objectenapi`, `catalogiapi` en `zakenapi`. Openproduct
  vereist dus **impliciet ook `catalogiapi.enabled`**, ook al staat die niet in de lijst; zonder die flag mist de
  `ZakenApiService`-bean en faalt de start.

### 2. taak

- **Gebruikt door:** `OpenProductService.getThemaTaken` (r. ~869) en `getProductTaken` (r. ~956) via
  `TaakService.getTakenV2` / `getTaakByIdV2`; type `TaakV2`. `TaakConfig` staat in `@EnableConfigurationProperties`
  en wordt als `taakObjectConfig` aan de service gegeven.
- **Echte dependency?** Deels. Taken: ja, voor twee methoden en drie velden. Maar de constructorparameter
  `objectsApiTaskConfigProperties: TaakConfigProperties` wordt in `OpenProductService` **nergens gelezen**, en de
  imports `ObjectsApiObject`, `ObjectSearchParameter`, `Comparator`, `TaakObjectV2` en `TaakPageV2` worden ook
  niet gebruikt (restant van eerdere code).
- **Optioneel te maken?** Ja. Verwijder de dode parameter en imports en maak `TaakService` optioneel zoals bij
  zaken. `TaakConfig` in `@EnableConfigurationProperties` van openproduct is dan overbodig.
- **Let op:** `TaakAutoConfiguration` eist zelf `objectenapi.enabled` + `taak.enabled`.

### 3. documentenapis

- **Gebruikt door:** `OpenProductService.getProductDocumentContent` (r. ~998) en `getOpenProductDocumenten`
  (r. ~1046): `DocumentenApiService.getDocument`, `getDocumentContentStreaming`, `filterDocuments`. GraphQL-veld
  `OpenProductProduct.documenten` (type `Document`). REST-endpoint `web/rest/ProductDocumentResource` (+
  `ProductDocumentResourceHttpSecurityConfigurer`) gebruikt `FilenameSanitizer` uit documenten-api.
- **Echte dependency?** Ja, voor documentfunctionaliteit. `getProductDocumentContent` vangt al
  `ModuleNotConfiguredException` af, wat op bedoeld gedeeltelijk-geconfigureerd gebruik wijst.
- **Optioneel te maken?** Ja. `DocumentenApiService` optioneel injecteren; `ProductDocumentResource` en de
  security-configurer alleen registreren als `DocumentenApiService` bestaat (`@ConditionalOnBean`).
- **Let op:** `clientSslContextResolver` (bean uit `DocumentenApiAutoConfiguration`) wordt door
  `openProductDmnClient` al als `@Autowired(required = false)` gebruikt, dus al optioneel. `FilenameSanitizer` is
  een generiek hulpmiddel dat beter in een gedeelde module past.

### 4. objectenapi

- **Gebruikt door:** uitsluitend `OpenProductPrefillService` (`objectsApiClient.createObject(...)`, r. ~107-117),
  via de bean `openProductPrefillService` die in `OpenProductAutoConfiguration` een `ObjectsApiClient` eist.
  Verder niets in `src/main`.
- **Echte dependency?** Ja, maar alleen voor de prefill-functie (`prefill.typeUrl`). Geen dependency van
  producten/thema's.
- **Optioneel te maken?** Ja, het makkelijkst van de vier: `openProductPrefillService` plus de verwijzing in
  `OpenProductActieQuery` (constructorparameter) optioneel maken, of de bean `@ConditionalOnBean(ObjectsApiClient::class)`.
- **Let op:** de Objecten API is daarnaast transitief nodig voor zaken (`ZakenApiService` krijgt een
  `ObjectsApiClient`) en taken (`TaakService`). Zonder zaken en taken is de flag voor openproduct alleen nog voor
  prefill nodig.

### 5. openproduct (de eigen flag)

Terecht. Dit is de enige flag die altijd nodig is.

### Niet in de lijst, maar wel nodig

- `AuthenticationMachtigingsDienstService` (`:zgw:common-ground-authentication`) wordt in `OpenProductService`
  gebruikt voor autorisatie op producttypen en zaaktypen. Geen aparte module-flag (altijd aanwezig via
  `AuthenticationConfiguration`), dus niet het probleem.
- `catalogiapi` (impliciet, zie zakenapi).

## Technische koppelpunten die optionaliteit lastiger maken

1. **Eén constructor.** `OpenProductService` neemt `ZakenApiService`, `TaakService` en `DocumentenApiService`
   als verplichte parameters. Het enige echte code-obstakel. Oplossing: nullable/`ObjectProvider`, of de zaken-,
   taak- en documentfuncties naar aparte services verplaatsen die elk alleen bestaan als hun module er is.
2. **GraphQL-schema.** `openproduct/src/main/resources/graphql/schemas.graphqls` verwijst naar de types `Zaak`,
   `TaakV2` en `Document` (r. 15, 23, 227, 242-243, 356-357). Die types leven in schema's van de andere modules
   en staan alleen op het classpath door de Gradle-`api`-afhankelijkheden. Bij losmaken van de Gradle-koppeling
   moeten die velden naar een schema-extensie die met de betreffende module meekomt, anders valt het schema om.
3. **Gradle-afhankelijkheid.** De drie `api(project(...))`-regels maken de compile-time-koppeling hard. Elke
   wijziging moet passen in het ArchUnit-model in `architecture-test/.../ModuleLayers.kt`.
4. **Dode koppelingen.** `TaakConfig` + `objectsApiTaskConfigProperties` en de ongebruikte objecten-API-imports
   kunnen direct weg, los van het optioneel maken.

## Gevolgen voor het afhankelijkheidsmodel van module-flags (fase 3 van #143)

- Een flag hoort per **functie** te gelden, niet als "alle vijf of niets". Voorstel: `openproduct` = harde kern;
  `zakenapi`, `taak`, `documentenapis` en `objectenapi` = optionele *uitbreidingen* die extra velden, endpoints en
  beans activeren.
- De huidige `@ConditionalOnProperty` met meerdere namen is een EN-conditie. Voor "kern altijd, extra's als ze
  aanstaan" moet de klasse-conditie terug naar alleen `openproduct.enabled`, met `@ConditionalOnBean` /
  `ObjectProvider` voor de extra's. Dat sluit aan bij het patroon dat `TaakAutoConfiguration` al gebruikt voor
  `documentenapis` (`@ConditionalOnProperty` op afzonderlijke beans, r. 66 en 76).
- Het model moet ook de **impliciete** ketens uitdrukken: `zakenapi` -> `objectenapi` + `catalogiapi`;
  `taak` -> `objectenapi`; `besluitenapi` -> `documentenapis` + `zakenapi`; `berichten` -> `documentenapis`.
  Zonder dat model is "openproduct zonder zaken" onbetrouwbaar, omdat ontbrekende transitieve flags pas bij het
  starten als `NoSuchBeanDefinitionException` zichtbaar worden.
- Fail-fast: zet iemand `zakenapi.enabled=true` zonder `objectenapi.enabled`, dan hoort het model dat te melden in
  plaats van Spring.

## Aanbevolen volgorde (klein naar groot)

1. Dode `TaakConfig`/`objectsApiTaskConfigProperties` en ongebruikte imports opruimen (geen gedragsverandering).
2. Prefill/objectenapi optioneel maken (`@ConditionalOnBean(ObjectsApiClient::class)`).
3. Documenten optioneel maken (service-injectie + `ProductDocumentResource` conditioneel).
4. Zaken en taken optioneel maken (service-injectie; thema-zaken/-taken nullable).
5. Klasse-conditie terug naar `openproduct.enabled` en schema-velden naar module-eigen extensies; daarna pas de
   Gradle-`api`-koppelingen losmaken.

## Niet onderzocht

- Of bestaande integratietests (`OpenProductQueryIT`, `OpenProductActieQueryIT`, `OpenProductThemaQueryIT`) de
  uitgeschakelde varianten dekken; dat is werk voor de bouwstap (tdd), niet voor dit onderzoek.
- Het gedrag van de frontend wanneer `zaken`/`taken`/`documenten` op een product `null` teruggeven.
