# Startfouten per module bij ontbrekende configuratie (#380)

Onderzoeksvraag (issue #380, onderdeel van wayfinder-map #379): wat laat het opstarten van de backend
mislukken als een module aan staat (`nl-portal.config.<module>.enabled=true`) maar de URL of credentials
ontbreken, of de API onbereikbaar is? Per module: oorzaak van de startfout (met `bestand:regel`) en of die
naar een foutmelding per request kan worden verplaatst.

Methode: statische analyse van de broncode onder `backend/` op commit `ea58815d1` (main). De applicatie is
niet gestart. Alle paden zijn relatief aan `backend/`. Elke claim verwijst naar de bron die hem bevat.

## Samenvatting

1. **Ontbrekende URL, token, clientId of secret laat de start nergens meer falen.** Sinds #150 (commit
   `3b9cabf0d`) bouwen de module-clients hun `WebClient` lazy of per aanroep op en valideren ze met
   `requireConfigured` (`core/src/main/kotlin/nl/nlportal/core/util/ModuleConfiguration.kt:28`). De fout is
   dan een `ModuleNotConfiguredException` bij het eerste gebruik.
2. **Een onbereikbare API laat de start nergens falen.** Geen enkele module verbindt bij het opstarten. Alle
   HTTP-verbindingen ontstaan bij een request; ClamAV verbindt per scan.
3. **Wat nog wel bij het opstarten faalt, valt in vier groepen** (zie "Resterende startfouten"):
   - de `OpenProductDmnClient` leest SSL-bestanden en genereert een token in zijn constructor;
   - bean-afhankelijkheden tussen modules waarvan de `@ConditionalOnProperty`-voorwaarden niet kloppen met de
     constructor-parameters (zaken, zaken-api, besluiten, berichten, openproduct);
   - het binden van `java.net.URI`-properties bij een ongeldige waarde, en verplichte constructorparameters
     in `AuthenticationMachtigingsDienst`;
   - de basis (core/app): placeholders zonder default in `application.yml` en de feature-flag-provider.
4. Alleen groep 1 en 2 hebben een aanwijsbare per-request-variant; groep 3 en 4 zijn kleine, gerichte
   aanpassingen of bewuste fail-fast-keuzes (zie per module).

## Basis: wat niet per module is te schakelen

| Oorzaak | Bron | Per request uit te stellen? |
|---|---|---|
| Placeholder `${JWKS_URI}` zonder default: Spring kan het niet oplossen en de start faalt | `app/src/main/resources/config/application.yml:28` | Nee, het is de resource-server van de portal zelf (geen module). Fail-fast is hier gewenst. |
| `${KEYCLOAK_CLIENT_ID}` en `${KEYCLOAK_CLIENT_SECRET}` zonder default | `app/src/main/resources/config/application.yml:48` en `:51` | Alleen relevant voor common-ground-authentication (zie daar). De defaults staan op `""` in `KeycloakConfig.kt:24-29`, dus de placeholders in de YAML zijn de enige oorzaak. |
| `ReactiveJwtDecoder` is een verplichte bean voor de converter | `zgw/common-ground-authentication/.../AuthenticationConfiguration.kt:42-44` | Het bestaan van de bean is afhankelijk van `spring.security.oauth2.resourceserver.jwt.*`. Een `jwk-set-uri` maakt de decoder zonder verbinding te leggen; Spring Boot haalt de sleutels pas bij de eerste validatie op (aanname uit Spring Boot-gedrag, niet in deze repo geverifieerd). |
| Feature-flag-provider wacht bij de start op initialisatie (`setProviderAndWait`). Standaard is dat het meegeleverde bestand, dus geen externe afhankelijkheid. Een `nl-portal.feature-flags.file` dat niet bestaat laat de start falen. | `core/.../featureflag/FeatureFlagAutoConfiguration.kt:45,52` | Niet nodig: standaard zonder afhankelijkheid. |
| `FeatureFlag` weigert een permission-flag met default `true` | `core/.../featureflag/FeatureFlag.kt:28` | Codefout, geen configuratiefout. |

## Per module

Voor alle modules geldt: `ConfigurationProperties`-klassen hebben defaults (`""` of `null`) en binden dus
zonder waarde. Er zijn geen `@Validated`, `@NotBlank` of `@NotNull` annotaties in `backend/**/src/main`
(zoekopdracht op die annotaties en op `requireNotNull`, `@PostConstruct`, `ApplicationRunner`,
`@EventListener`, `afterPropertiesSet` gaf geen treffers in module-startcode).

### core
- Bean-aanmaak: `CoreAutoConfiguration` en `FeatureFlagAutoConfiguration` (zie de tabel hierboven). Geen
  externe verbindingen.
- `ping-endpoint.enabled` heeft geen invloed op de start (`CoreAutoConfiguration.kt:63`).

### form (voorwaarde: `objectenapi.enabled`)
- Eigen property `formDefinitionObjectTypeUrl` is `String?` (`form/.../FormConfig.kt:25`), geen controle bij de
  start. Fout komt per request via de objectenapi-client.
- Afhankelijkheid: `ObjectenApiService` (`FormAutoConfiguration.kt:33`); aanwezig doordat de voorwaarde dezelfde
  is als die van objectenapi.

### zgw/objectenapi (`objectenapi.enabled`)
- Startfout door ontbrekende waarden: geen. `ObjectsApiClient.webClient()` bouwt per aanroep en roept
  `requireConfigured("objectenapi", "url" | "token")` aan (`zgw/objectenapi/.../ObjectsApiClient.kt:120-133`).
- Resterend: `url` is een `URI` met default `URI.create("")` (`ObjectsApiClientConfig.kt:27`). Een syntactisch
  ongeldige waarde (bijvoorbeeld met spaties) laat de binding bij de start falen. Een lege waarde niet.
- Per request uitstelbaar: al het bovenstaande is al per request. De URI-binding kan naar `String` om ook
  dat uit te stellen.

### zgw/catalogi-api (`catalogiapi.enabled`)
- `CatalogiApiClient.webClient()` valideert per aanroep secret, clientId en url (`zgw/catalogi-api/.../CatalogiApiClient.kt:105-127`).
- `IdTokenGenerator` eist een secret van minstens 32 tekens (`zgw/idtoken-authentication/.../IdTokenGenerator.kt:30`).
  Dat gebeurt per aanroep, dus ook per request.
- Startfout: geen.

### zgw/documenten-api (`documentenapis.enabled`)
- `DocumentenApiClient.webClient(documentApi)` zoekt de configuratie per aanroep (`getConfig` gooit
  `ModuleNotConfiguredException`, `DocumentApisConfig.kt:52-54`) en valideert de URL
  (`DocumentenApiClient.kt:106-137`). Het token wordt alleen gemaakt als clientId en secret beide gevuld zijn
  (`:141-146`).
- SSL: `clientSslContextResolver.resolve` leest certificaatbestanden per aanroep (`:121-133`); een ontbrekend
  bestand geeft dus een fout per request.
- ClamAV (`virusscan.clamav.enabled`, `ClamAVConfiguration.kt:30`): de bean wordt zonder verbinding gemaakt;
  `ClamavClient` ontstaat per scan (`ClamAVService.kt:29-34`). Een onbereikbare ClamAV faalt per upload.
- Startfout: geen.

### zgw/zaken-api (`objectenapi`, `catalogiapi` en `zakenapi` allen `enabled`)
- `ZakenApiClient.webClient` is `by lazy`; url, secret en clientId worden bij gebruik gecontroleerd
  (`zgw/zaken-api/.../ZakenApiClient.kt:54-85`).
- **Startfout door bean-afhankelijkheid:** `ZakenApiService` vereist `DocumentenApiService`
  (`ZakenApiAutoConfiguration.kt:43`), maar de voorwaarde noemt `documentenapis.enabled` niet
  (`ZakenApiAutoConfiguration.kt:36`). Staat documentenapis uit, dan faalt de start met een
  `NoSuchBeanDefinitionException`.
- Daarnaast `ZakenApiConfig` wordt zowel via `@EnableConfigurationProperties` (`:33-35`) als via een eigen
  `@Bean` (`:55-56`) geregistreerd. De `@Bean`-variant maakt een lege instantie zonder binding; welke de andere
  verdringt hangt van de volgorde af. Dit is geen startfout, maar verdient controle (buiten de scope van deze vraag).
- Per request uitstelbaar: de afhankelijkheid kan als optionele parameter (nullable type) en een fout bij gebruik.

### zgw/zaken (zelfde drie voorwaarden als zaken-api)
- **Startfout door bean-afhankelijkheid:** `ZaakQuery` vereist `BesluitenService` en `CatalogiApiService`
  (`zgw/zaken/.../ZakenAutoConfiguration.kt:36-38`). `BesluitenService` bestaat alleen bij
  `besluitenapi.enabled` en `documentenapis.enabled` en `zakenapi.enabled` (`BesluitenAutoConfiguration.kt:37`),
  maar de voorwaarde van zaken (`:31`) noemt besluitenapi en documentenapis niet. Zaken aan, besluitenapi uit
  geeft dus een startfout.
- Per request uitstelbaar: ja, als `besluitenService` een optionele parameter wordt.

### zgw/besluiten (`besluitenapi`, `documentenapis`, `zakenapi` allen `enabled`)
- `BesluitenApiClient.webClient` is `by lazy` met `requireConfigured` (`zgw/besluiten/.../BesluitenApiClient.kt:39-51,136-137`).
- **Startfout door bean-afhankelijkheid:** `BesluitenQuery` vereist `CatalogiApiService`
  (`BesluitenAutoConfiguration.kt:63-64`) en `BesluitenService` vereist `ZakenApiService` (`:52`).
  `ZakenApiService` vraagt `objectenapi` en `catalogiapi` aan (`ZakenApiAutoConfiguration.kt:36`), `CatalogiApiService`
  `catalogiapi` (`CatalogiApiAutoConfiguration.kt:30`). De voorwaarde van besluiten (`:37`) noemt die twee niet.

### zgw/berichten (`berichten`, `documentenapis` `enabled`)
- **Startfout door bean-afhankelijkheid:** `BerichtenService` vereist `ObjectenApiService`
  (`zgw/berichten/.../BerichtenAutoConfiguration.kt:37`), maar de voorwaarde (`:33`) noemt `objectenapi.enabled`
  niet. Berichten zonder objectenapi geeft een startfout.
- `berichtObjectTypeUrl` heeft default `""` (`BerichtenConfiguration.kt:26`); fout per request.

### zgw/taak (`objectenapi` en `taak` `enabled`)
- `TaakService` krijgt `ObjectsApiClient`, `AuthenticationMachtigingsDienstService` en `TaakConfig`
  (`zgw/taak/.../TaakAutoConfiguration.kt:43-47`). `TaakFormQuery` vereist `ObjectsApiFormDefinitionService`
  (`:59-62`), die dezelfde objectenapi-voorwaarde heeft (`FormAutoConfiguration.kt:28`). Consistent: geen startfout.
- De documentresource is apart voorwaardelijk op documentenapis (`:66,76`); `VirusScanService?` is optioneel (`:70`).
- `objectTypeUrl` default `""` (`TaakConfig.kt:26`): fout per request.

### zgw/openproduct (`openproduct`, `objectenapi`, `taak`, `zakenapi`, `documentenapis` allen `enabled`)
- `OpenProductClient` en `OpenProductTypeClient` zijn `by lazy` met `requireConfigured` op productApiUrl,
  productTypeApiUrl en token (`zgw/openproduct/.../OpenProductClient.kt:37-51`).
- **Startfout 1, constructor van `OpenProductDmnClient`:** de WebClient wordt in een `init`-blok gebouwd
  (`OpenProductDmnClient.kt:43-88`). Daar gebeurt (a) `clientSslContextResolver.resolve` met bestandslezen als
  `dmn.ssl` is gezet (`:58-63`), en (b) `getToken()` (`:74`), dat een `IdTokenGenerator` aanroept zodra
  `dmn.clientId` en `dmn.secret` gevuld zijn (`:106-110`). Een secret korter dan 32 tekens
  (`IdTokenGenerator.kt:30`) of een ontbrekend certificaatbestand laat de bean-aanmaak, en dus de start, falen.
  Zonder DMN-configuratie is er geen probleem (alles blijft leeg).
  Per request uitstelbaar: ja, door de client `by lazy` te maken zoals de twee andere openproduct-clients.
- **Startfout 2, bean-afhankelijkheid:** `OpenProductService` vereist `ZakenApiService`
  (`OpenProductAutoConfiguration.kt:68`), die `catalogiapi.enabled` nodig heeft (`ZakenApiAutoConfiguration.kt:36`).
  De voorwaarde van openproduct (`:47`) noemt catalogiapi niet.
- `productApiUrl`, `productTypeApiUrl` zijn `URI?` (`OpenProductModuleConfiguration.kt:28-29`): ongeldige
  syntaxis faalt bij de binding.

### zgw/openklant (`openklant2.enabled`)
- `OpenKlant2KlantinteractiesClient.webClient()` bouwt per aanroep met `requireConfigured` op URL en token
  (`zgw/openklant/.../OpenKlant2KlantinteractiesClient.kt:37-43`).
- **Let op, voorwaarde ontbreekt:** `OpenKlantAutoConfiguration` heeft geen `@AutoConfiguration` en geen
  `@ConditionalOnProperty` (`OpenKlantAutoConfiguration.kt:24-27`); ze wordt wel geladen via de `.imports`-lijst.
  Client en service bestaan dus ook als openklant2 uit staat (onschadelijk, want niets verbindt bij de start).
  Alleen de GraphQL-laag is voorwaardelijk (`OpenKlantGraphqlAutoConfiguration.kt:30`).
- De `init`-blokken in `client/domain/*.kt` valideren lengtes van request-velden en vuren pas bij het maken
  van een request-object, dus per request.
- `klantinteractiesApiUrl` en `contactgegevensApiUrl` zijn `URI?` (`OpenKlantModuleConfiguration.kt:27-28`).
- Verificatie uit: `OpenKlantGraphqlAutoConfiguration` krijgt `VerificatieModuleConfiguration` (altijd
  geregistreerd, `VerificatieAutoConfiguration.kt:29`) en `VerificatieService?` (optioneel, `:45,57`). Geen startfout.

### zgw/verificatie (`verificatie.enabled`)
- `VerificatieClient.webClient` is `by lazy`; URL gecontroleerd met `requireConfigured`
  (`zgw/verificatie/.../VerificatieClient.kt:47-96`). Token en SSL lezen daarom ook pas bij gebruik.
- `url` is `URI?` (`VerificatieModuleConfiguration.kt:29`).
- Startfout: geen.

### zgw/common-ground-authentication (altijd actief)
- `AuthenticationConfiguration` is niet voorwaardelijk (`AuthenticationConfiguration.kt:27-29`) en vereist een
  `ReactiveJwtDecoder` (`:42-44`); die komt van de resource-server-configuratie (zie de tabel bij "Basis").
- `AuthenticationMachtigingsDienstService` leest in zijn `init` optioneel een resource-URL, maar vangt elke
  uitzondering af en logt een waarschuwing (`AuthenticationMachtigingsDienstService.kt:31-44`). Een ontbrekend
  bestand laat de start dus niet falen.
- **Startfout bij partiële configuratie:** `AuthenticationMachtigingsDienst` heeft verplichte constructor-
  parameters `uuid` en `naam` zonder default (`AuthenticationMachtigingsDienstConfig.kt:30-34`). Een
  `configurations`-item zonder die twee laat de binding falen. Zonder items is er geen fout.
- Keycloak: defaults zijn leeg (`KeycloakConfig.kt:24-29`); alleen de YAML-placeholders zijn de oorzaak
  (zie "Basis").

### zgw/idtoken-authentication (altijd actief)
- Bean zonder configuratie (`IdTokenGeneratorAutoConfiguration.kt:25-26`); de 32-tekens-controle gebeurt per
  token (`IdTokenGenerator.kt:30,61`). Startfout: geen.

### haalcentraal-hr (`haalcentraal.hr.enabled`)
- `HandelsregisterClient.webClient()` bouwt per aanroep en valideert URL (`haalcentraal-hr/.../HandelsregisterClient.kt:50-79`).
  SSL-bestanden worden per aanroep gelezen (`:63-74`).
- `url` is een `String` met default `""` (`HaalCentraalHrConfig.kt:27`). Startfout: geen.

### haalcentraal2 (`haalcentraal2.enabled`)
- Beide clients hebben `by lazy` WebClients met `requireConfigured` (`haalcentraal2/.../HaalCentraal2BrpClient.kt:38-67`,
  `HaalCentraal2BewoningClient.kt:38-67`).
- `HaalCentraal2GraphQLConfiguration` heeft geen `@AutoConfiguration` maar staat in de `.imports`-lijst. De
  Gemachtigde-query vereist `HandelsregisterService` en heeft daarvoor een extra voorwaarde op
  `haalcentraal.hr.enabled` (`HaalCentraal2GraphQLConfiguration.kt:45-48`): consistent, geen startfout.
- Startfout: geen.

### payment-direct (`payment.direct.enabled`)
- `DirectPaymentClient.webClient` is `by lazy` met `requireConfigured` (`payment-direct/.../DirectPaymentClient.kt:50-79`).
- Webhook-onderdelen zijn voorwaardelijk op `objectenapi.enabled` (`DirectPaymentAutoConfiguration.kt:61,79`):
  consistent.
- `DirectPaymentWebhookService` voert in zijn `init` voor elk profiel `storeSecretKey(webhookApiKey, webhookApiSecret)`
  uit (`DirectPaymentWebhookService.kt:43-47`). Profielen hebben defaults `""` (`DirectPaymentModuleConfiguration.kt:55-56`).
  Of de externe bibliotheek een lege sleutel weigert is niet nagegaan (de bibliotheek is niet gelezen); dat
  is het enige punt in deze module waar een profiel zonder webhook-gegevens bij de start kan falen.

## Resterende startfouten, op volgorde van urgentie

| # | Module | Oorzaak | Bron | Per request te maken? |
|---|---|---|---|---|
| 1 | openproduct | DMN-client leest SSL-bestanden en genereert token in `init` | `OpenProductDmnClient.kt:43-88,106-110` | Ja: `by lazy` |
| 2 | zaken, zaken-api, besluiten, berichten, openproduct | Constructor-bean wordt niet geleverd door voorwaarde van de eigen module | `ZakenAutoConfiguration.kt:31,37-38`; `ZakenApiAutoConfiguration.kt:36,43`; `BesluitenAutoConfiguration.kt:37,63`; `BerichtenAutoConfiguration.kt:33,37`; `OpenProductAutoConfiguration.kt:47,68` | Ja: optionele parameter of voorwaarde aanvullen |
| 3 | objectenapi, verificatie, openproduct, openklant | `URI`-property met ongeldige waarde | `ObjectsApiClientConfig.kt:27`; `VerificatieModuleConfiguration.kt:29`; `OpenProductModuleConfiguration.kt:28-29`; `OpenKlantModuleConfiguration.kt:27-28` | Ja: `String` in plaats van `URI` |
| 4 | common-ground-authentication | Item in `configurations` zonder `uuid` of `naam` | `AuthenticationMachtigingsDienstConfig.kt:30-34` | Nee en ook niet gewenst: fail-fast bij een foute configuratie is verdedigbaar |
| 5 | payment-direct | Lege webhook-sleutel in `init` (onzeker) | `DirectPaymentWebhookService.kt:43-47` | Ja, onderzoek eerst het gedrag van de bibliotheek |
| 6 | app (basis) | `${JWKS_URI}`, `${KEYCLOAK_CLIENT_ID}`, `${KEYCLOAK_CLIENT_SECRET}` zonder default | `app/.../application.yml:28,48,51` | Nee: de portal zelf heeft deze nodig |

## Als een module uit staat maar andere modules zijn bean gebruiken

Alleen punt 2 hierboven is een echte uitval: voorwaarde van module A noemt niet alles wat A's constructor
nodig heeft. Dat leest als de omgekeerde richting van het doel (#379: opstarten zonder alle afhankelijkheden).
De overige verwijzingen zijn veilig:

- Taak is voorwaardelijk op objectenapi en krijgt dus nooit een ontbrekende `ObjectsApiClient`
  (`TaakAutoConfiguration.kt:39`).
- `VerificatieService` is voor openklant en payment nullable en wordt dus `null` als verificatie uit staat
  (`OpenKlantGraphqlAutoConfiguration.kt:45,57`).
- `VirusScanService` is nullable voor de taak-documentresource (`TaakAutoConfiguration.kt:70`).
- `AuthenticationMachtigingsDienstService` en `IdTokenGenerator` zijn altijd aanwezig, dus afhankelijkheden
  daarop falen nooit.
- De modules `ObjectenApiService`, `DocumentenApiService` en `CatalogiApiService` bestaan alleen als hun eigen
  `enabled` aan staat. Alle consumenten die niet op die vlag voorwaardelijk zijn, vallen onder punt 2.
- Een GraphQL-schema wordt uit `classpath*:graphql/**` geladen (`app/.../application.yml:33`). Of schema-
  velden zonder resolver-bean (bij een uitgeschakelde module) een startfout geven, is niet onderzocht en
  verdient een eigen controle in het vervolg.

## Open punten voor het vervolg (#382)

1. De exacte regel van `zaken` aan/besluiten uit en `zaken-api` zonder documentenapis moet met een
   `ApplicationContextRunner`-test worden bevestigd; deze analyse is statisch.
2. Het `ZakenApiConfig`-dubbelbeheer (`@Bean` plus `@EnableConfigurationProperties`) controleren op
   daadwerkelijke binding.
3. Gedrag van de payment-bibliotheek bij een lege webhook-sleutel nagaan.
4. Beslissen of `app/application.yml` defaults krijgt voor de Keycloak-placeholders voor modules die ze niet
   nodig hebben.
