# Startup-gebonden wiring versus per-request gating per backendmodule

Onderzoek voor [#381](https://github.com/pinkroccade/nl-portal/issues/381), onderdeel van wayfinder-map #379
(NL Portal starten zonder alle afhankelijkheden) en aansluitend op fase 3 van #143 (per-request modulegates met
`FeatureGate`, ADR 0014). Bronnen: de broncode op `main` (commit `ea58815d1`). Alle paden zijn relatief aan
`backend/`. Er is geen code gewijzigd.

## Samenvatting

1. **Alle modulewiring hangt nu aan opstarten.** Elke module-autoconfiguratie zit achter een
   `@ConditionalOnProperty` op `nl-portal.config.<module>.enabled`. De beans (client, service, GraphQL-controller,
   REST-resource, `HttpSecurityConfigurer`) bestaan dus alleen als de property aan staat. Het `FeatureGate`-gebruik in
   de code is nu beperkt tot `FrontendConfigurationService` (`ops.frontend.message-count`).
2. **De clients zijn al lui geconfigureerd.** Bijna elke client bouwt zijn `WebClient` pas bij het eerste request
   (`by lazy` of een `webClient()`-functie) en valideert dan met `requireConfigured(...)`, dat
   `ModuleNotConfiguredException` gooit. Een ontbrekende URL of token laat de applicatie dus niet crashen bij het
   opstarten. Uitzondering: `OpenProductDmnClient` bouwt zijn `WebClient` in een `init`-blok (zie tabel openproduct).
3. **De beans kunnen dus grotendeels altijd geregistreerd worden en per request gegate.** Wat startup-gebonden
   blijft: de security filter chain, de GraphQL-schemabestanden, de property-binding, de OpenFeature-provider en de
   `ObjectMapper`. Er is geen persistentielaag (ADR 0013 en 0017).
4. **Er zijn impliciete koppelingen die niet in de `@ConditionalOnProperty`-lijsten staan** (zie
   "Verborgen koppelingen"). Dit zijn bestaande bugs of risico's die het afhankelijkheidsmodel (ticket #383) expliciet
   moet maken.
5. **De GraphQL-schema's zijn schema-first en worden bij het opstarten geladen** (`spring.graphql.schema.locations:
   classpath*:graphql/**/`, `app/src/main/resources/config/application.yml`). Het schema bevat dus alle velden van
   alle modules die als dependency in `:app` zitten, ongeacht of de `@Controller`-bean bestaat. Dit komt overeen met
   het uitgangspunt van #143 dat het schema niet runtime verandert.

## Hoe module-wiring nu werkt

- Elke module registreert zijn autoconfiguratie via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
  (bijv. `zgw/zaken/src/main/resources/META-INF/spring/...imports`).
- De module-vlag staat als `@ConditionalOnProperty(prefix = "nl-portal.config", name = [...], havingValue = "true")`
  op de autoconfiguratieklasse. Bij meerdere namen moeten ze allemaal `true` zijn (dit is de "vijf andere vlaggen"
  van #143).
- De `@Controller`-klassen (`@QueryMapping`, `@MutationMapping`) en REST-resources worden als `@Bean` geleverd door de
  autoconfiguratie, niet door component scanning.
- Elke module heeft een `@ConfigurationProperties` (bijv. `ObjectsApiClientConfig`, prefix
  `nl-portal.config.objectenapi`). De property-binding is startup-gebonden en blijft dat.
- Modules die aan de filter chain bijdragen leveren een `HttpSecurityConfigurer`-bean
  (`core/.../security/config/HttpSecurityConfigurer.kt`); `springSecurityWebFilterChain` roept ze allemaal aan bij
  het opbouwen van de chain.

## Wat per definitie startup-gebonden blijft

| Wiring | Waarom startup-gebonden | Bron |
|---|---|---|
| `springSecurityWebFilterChain`, `corsWebFilter`, `@EnableWebFluxSecurity`, `@EnableReactiveMethodSecurity` | Spring Security bouwt de chain eenmalig; alle `HttpSecurityConfigurer`-beans worden daarbij samengevoegd | `core/src/main/kotlin/nl/nlportal/core/security/OauthSecurityAutoConfiguration.kt` |
| JWT-decoder (`spring.security.oauth2.resourceserver.jwt.jwk-set-uri`) | Vereist `JWKS_URI` bij het opstarten; de ReactiveJwtDecoder is nodig voor `CommonGroundAuthenticationConverter` | `app/src/main/resources/config/application.yml`, `zgw/common-ground-authentication/.../AuthenticationConfiguration.kt` |
| `PortalAuthenticationConverter` (`portal-authentication`) en `commonGroundAuthenticationConverter` | De converter wordt in de chain gehangen | `portal-authentication/.../AuthenticationConfiguration.kt` |
| GraphQL-schemabestanden en scalars (`RuntimeWiringConfigurer`) | Schema-first, geladen bij het opstarten uit alle jars op het classpath | `graphql/.../GraphqlAutoConfiguration.kt`, `*/src/main/resources/graphql/*.graphqls` |
| OpenFeature-provider (`FeatureProvider`, `OpenFeatureAPI`, `FeatureGate`, `FeatureContextFactory`) | Provider wordt bij het opstarten gezet en afgewacht (`setProviderAndWait`); de gate is de infrastructuur voor alles hieronder | `core/.../featureflag/FeatureFlagAutoConfiguration.kt` |
| `@ConfigurationProperties`-binding van alle modules | Spring bindt properties eenmalig; hot reload van properties valt buiten scope van #379 | per module `*Config.kt` / `*ModuleConfiguration.kt` |
| `objectMapper`, `FrontendConfigurationService`, `FrontendConfigurationResource`, `PingResource` | Kern; `FrontendConfigurationService` is per request al flag-gedreven voor `message-count` | `core/.../autoconfiguration/CoreAutoConfiguration.kt` |
| Persistentie | Bestaat niet: geen JPA, R2DBC, Flyway of Liquibase in `backend/` (zoekopdracht gaf nul treffers). ADR 0013 sluit persistente state uit; ADR 0017 staat vluchtige state (Redis voor BFF-sessie of cache) toe | `site/development/adr/0013-*.md`, `0017-*.md` |
| De module-set zelf (welke jars in `:app`) | Gradle-dependencies van `:app`; `:form` staat uitgecommentarieerd (`// implementation(project(":form"))`) maar komt transitief binnen via `:zgw:taak` | `app/build.gradle.kts` (regels 23 tot 34), `zgw/taak/build.gradle.kts` |

Opmerking over de filter chain: de document-resources leveren elk een `HttpSecurityConfigurer` die alleen
`authenticated()` zet op hun pad (bijv. `ZaakDocumentResourceHttpSecurityConfigurer`). Omdat `anyExchange().authenticated()`
al de default is en een pad zonder resource gewoon een 404 geeft, kan een module-uit-pad hier veilig de
configurer houden of verliezen. Het is dus geen echte startup-grens, maar het is wel wiring die nu aan de
module-vlag hangt (zie tabellen).

## Tabel per module

Legenda kolom "Grens": **S** = moet startup-gebonden blijven; **R** = kan altijd geregistreerd en per request
gegate worden met `FeatureGate`; **R*** = kan per request, maar met een kanttekening.

### core, graphql, portal-authentication, idtoken-authentication, common-ground-authentication

Geen module-vlag; altijd actief.

| Wiring | Soort | Grens | Toelichting | Bron |
|---|---|---|---|---|
| Security filter chain en CORS | Security | S | Zie boven | `core/.../security/OauthSecurityAutoConfiguration.kt` |
| `FrontendConfigurationService` / `Resource` | REST | S (infra) | Per request flag-evaluatie bestaat al; `modules` komt nu uit properties (`modulesWhitelist`, `FrontendModuleConfigurationProperties`), niet uit flags (#156/#18) | `core/.../frontend/service/FrontendConfigurationService.kt` regels 45 tot 70 |
| `PingResource` | REST | R of S | Aan `nl-portal.support.ping-endpoint.enabled`; ops-toggle kandidaat, laag risico | `CoreAutoConfiguration.kt` regel 63 |
| `GraphqlAutoConfiguration` (scalars, `GraphQLExceptionHandler`) | GraphQL | S | Schema-infra. De exception handler is de plek om een "module niet beschikbaar"-fout naar GraphQL-fouten te vertalen | `graphql/.../GraphqlAutoConfiguration.kt` |
| `IdTokenGenerator` | Bean zonder I/O | S (altijd aanwezig) | Zonder dependencies; wordt door catalogi, documenten, besluiten, zaken-api gebruikt | `zgw/idtoken-authentication/.../IdTokenGeneratorAutoConfiguration.kt` |
| `AuthenticationMachtigingsDienstService`, `KeycloakConfig` | Auth | S | Altijd aanwezig; `ZakenApiService`, `TaakService` en `OpenProductService` hangen er hard aan. Vereist `KEYCLOAK_CLIENT_ID`/`SECRET` in `application.yml` | `zgw/common-ground-authentication/.../AuthenticationConfiguration.kt` |

### zgw:objectenapi (`objectenapi.enabled`)

| Wiring | Soort | Grens | Toelichting | Bron |
|---|---|---|---|---|
| `ObjectsApiClient` | HTTP-client | R | `WebClient` per call via `webClient()`; `requireConfigured("objectenapi","url"/"token")` gooit pas bij gebruik | `zgw/objectenapi/.../client/ObjectsApiClient.kt` regels 120 tot 133 |
| `ObjectenApiService` | Service | R | Geen I/O in constructor | `zgw/objectenapi/.../autoconfiguration/ObjectsApiAutoConfiguration.kt` |
| Gate-voorwaarde | Intern | | Dit is de grondmodule: `form`, `taak`, `zaken-api`, `zaken`, `openproduct`, `berichten`, `payment-direct` hangen eraan | zie "Afhankelijkheidsgraaf" |

### zgw:catalogi-api (`catalogiapi.enabled`)

| Wiring | Soort | Grens | Toelichting | Bron |
|---|---|---|---|---|
| `CatalogiApiClient` | HTTP-client | R | `webClient()` per call; `requireConfigured` op url, secret, clientId | `zgw/catalogi-api/.../client/CatalogiApiClient.kt` regels 105 tot 127 |
| `CatalogiApiService`, `CatalogiApiConfig` | Service, properties | R (service), S (properties) | `ZakenApiClient` leest `CatalogiApiConfig` ook direct (`getZaakTypeUrl`) | `CatalogiApiAutoConfiguration.kt` |

### zgw:documenten-api (`documentenapis.enabled`, `virusscan.clamav.enabled`)

| Wiring | Soort | Grens | Toelichting | Bron |
|---|---|---|---|---|
| `DocumentenApiClient` | HTTP-client | R | `webClient(documentApi)` per call; `url` via `requireConfigured`, maar `secret!!` en `clientId!!` geven een `NullPointerException` in plaats van `ModuleNotConfiguredException` (regel 144) | `zgw/documenten-api/.../client/DocumentenApiClient.kt` regels 106 tot 144 |
| `DocumentApisConfig` (lookup per documentApi/URL) | Properties | S | Gooit `ModuleNotConfiguredException` bij onbekende configuratie | `zgw/documenten-api/.../client/DocumentApisConfig.kt` regels 54, 62 |
| `ClientSslContextResolver` | Bean | S | Staat dubbel gedefinieerd (documenten en haalcentraal-hr), beide met `@ConditionalOnMissingBean` | `DocumentenApiAutoConfiguration.kt` regel 37, `HandelsregisterAutoConfiguration.kt` regel 37 |
| `DocumentenApiService` | Service | R | | `DocumentenApiAutoConfiguration.kt` |
| `ClamAVService` (`VirusScanService`) | Virusscan | R | Maakt een `ClamavClient` per `scan()`-call (host/poort uit config), dus geen verbinding bij opstarten. Aangeroepen vanuit `TaakDocumentResource` via nullable `VirusScanService?`. Per request gate = `if (flag) service.scan()`; de unavailable-case moet fail-closed of fail-open expliciet kiezen (beslispunt voor #382/#383) | `ClamAVConfiguration.kt` regel 30, `service/impl/ClamAVService.kt` regels 26 tot 35, `TaakDocumentResource.kt` regel 61 |

### zgw:zaken-api (`zakenapi.enabled`, vereist `objectenapi`, `catalogiapi`)

| Wiring | Soort | Grens | Toelichting | Bron |
|---|---|---|---|---|
| `ZakenApiClient` | HTTP-client | R | `webClient` is `by lazy`; `requireConfigured` op url, secret, clientId. Bouwt zelf geen verbinding bij start | `zgw/zaken-api/.../client/ZakenApiClient.kt` regels 54 tot 85 |
| `ZakenApiService` | Service | R | Constructor vraagt `DocumentenApiService`, `ObjectsApiClient`, `AuthenticationMachtigingsDienstService` | `ZakenApiAutoConfiguration.kt` |
| `ZakenApiConfig`-bean | Properties | S | Staat zowel als `@Bean` als `@EnableConfigurationProperties`, dubbel | `ZakenApiAutoConfiguration.kt` |

### zgw:zaken (GraphQL en REST voor zaken; conditie `objectenapi`, `catalogiapi`, `zakenapi`)

| Wiring | Soort | Grens | Toelichting | Bron |
|---|---|---|---|---|
| `ZaakQuery` (`@Controller`, `@QueryMapping getZaken` e.a.) | GraphQL | R | Het schema (`zgw/zaken/src/main/resources/graphql/schema.graphqls`) bevat de velden altijd. Gate in de resolver (of een `@ControllerAdvice`/`WebGraphQlInterceptor` per veldprefix) | `zgw/zaken/.../graphql/ZaakQuery.kt`, `ZakenAutoConfiguration.kt` |
| `ZaakDocumentResource` (`GET /api/zakenapi/zaakdocument/{id}/content`) | REST | R | Gate per request, antwoord 404 of 503 met "niet beschikbaar" | `zgw/zaken/.../web/rest/ZaakDocumentResource.kt` |
| `ZaakDocumentResourceHttpSecurityConfigurer` | Security | S (mag altijd geregistreerd) | Zet alleen `authenticated()`; zie opmerking boven | `zgw/zaken/.../security/config/...Configurer.kt` |

### zgw:besluiten (`besluitenapi`, `documentenapis`, `zakenapi`)

| Wiring | Soort | Grens | Toelichting | Bron |
|---|---|---|---|---|
| `BesluitenApiClient` | HTTP-client | R | `webClient` `by lazy` (regel 39), `requireConfigured` op url, secret, clientId | `zgw/besluiten/.../client/BesluitenApiClient.kt` |
| `BesluitenService` | Service | R | Vraagt `DocumentenApiService` en `ZakenApiService` | `BesluitenAutoConfiguration.kt` |
| `BesluitenQuery` | GraphQL | R | Vraagt `CatalogiApiService`, dat niet in de conditielijst staat (zie verborgen koppelingen) | idem |
| `BesluitDocumentResource` + `...HttpSecurityConfigurer` | REST, security | R, S | | idem |

### zgw:taak (`taak.enabled`, `objectenapi.enabled`; documentresource ook `documentenapis.enabled`)

| Wiring | Soort | Grens | Toelichting | Bron |
|---|---|---|---|---|
| `TaakService` | Service | R | Hangt aan `ObjectsApiClient` en `AuthenticationMachtigingsDienstService` | `zgw/taak/.../autoconfigure/TaakAutoConfiguration.kt` |
| `TaakQueryV2`, `TaakMutationV2`, `TaakFormQuery` | GraphQL | R | `TaakFormQuery` vraagt `ObjectsApiFormDefinitionService` (module `form`) | idem |
| `TaakDocumentResource` | REST | R | Extra conditie `documentenapis`; vraagt `VirusScanService?` | regels 66 tot 80 |
| `TaakDocumentResourceHttpSecurityConfigurer` | Security | S (mag altijd) | | idem |
| `TaakConfig` | Properties | S | Wordt ook door `OpenProductAutoConfiguration` ingeschakeld | `OpenProductAutoConfiguration.kt` regel 45 |

### form (`objectenapi.enabled`)

| Wiring | Soort | Grens | Toelichting | Bron |
|---|---|---|---|---|
| `ObjectsApiFormDefinitionService` | Service | R | Geen eigen vlag, volgt `objectenapi`. In `:app` niet rechtstreeks, wel transitief via taak. Het schema `form/schemas.graphqls` heeft geen eigen controller | `form/.../autoconfigure/FormAutoConfiguration.kt` regel 28 |

### zgw:berichten (`berichten.enabled`, `documentenapis.enabled`)

| Wiring | Soort | Grens | Toelichting | Bron |
|---|---|---|---|---|
| `BerichtenService` | Service | R | Vraagt `ObjectenApiService` en `DocumentenApiService`; `objectenapi` ontbreekt in de conditielijst | `zgw/berichten/.../autoconfigure/BerichtenAutoConfiguration.kt` |
| `BerichtenQuery` | GraphQL | R | | idem |
| `BerichtDocumentResource` + configurer | REST, security | R, S | | idem |

### zgw:openklant (`openklant2.enabled` alleen op GraphQL-config)

| Wiring | Soort | Grens | Toelichting | Bron |
|---|---|---|---|---|
| `OpenKlant2KlantinteractiesClient` | HTTP-client | R | **Niet voorwaardelijk**: `OpenKlantAutoConfiguration` heeft geen `@ConditionalOnProperty`, dus client en service bestaan altijd. `webClient()` is lui, dus veilig | `zgw/openklant/.../autoconfigure/OpenKlantAutoConfiguration.kt`, `client/OpenKlant2KlantinteractiesClient.kt` regels 37 tot 43 |
| `OpenKlant2Service` | Service | R | Idem altijd aanwezig | idem |
| `PartijQuery`/`PartijMutation`/`DigitaleAdres*`/`KlantContactQuery` | GraphQL | R | Nu aan `openklant2.enabled`. `DigitaleAdres*` vragen `VerificatieModuleConfiguration` en een nullable `VerificatieService?` | `OpenKlantGraphqlAutoConfiguration.kt` regel 30 |

Gevolg: dit is al het gewenste eindbeeld voor clients en services; alleen de GraphQL-controllers moeten van
bean-conditie naar request-gate.

### zgw:verificatie (`verificatie.enabled`)

| Wiring | Soort | Grens | Toelichting | Bron |
|---|---|---|---|---|
| `VerificatieClient`, `VerificatieService` | HTTP-client, service | R | `webClient` `by lazy`; `secret!!`/`clientId!!` (NPE bij gebruik, regels 131 tot 132) | `zgw/verificatie/.../client/VerificatieClient.kt`, `VerificatieAutoConfiguration.kt` regels 33, 45 |
| `VerificatieMutation` | GraphQL | R | Conditioneel | `VerificatieGraphqlAutoConfiguration.kt` regel 28 |
| `VerificatieQuery` | GraphQL | R | **Niet conditioneel**: staat altijd aan, ook zonder `verificatie.enabled`, en leest alleen `VerificatieModuleConfiguration` | idem regel 33 |

### zgw:openproduct (`openproduct`, `objectenapi`, `taak`, `zakenapi`, `documentenapis`)

| Wiring | Soort | Grens | Toelichting | Bron |
|---|---|---|---|---|
| `OpenProductClient`, `OpenProductTypeClient` | HTTP-client | R | `webClient` `by lazy`, `requireConfigured` | `zgw/openproduct/.../client/OpenProductClient.kt` regel 37, `OpenProductTypeClient.kt` regel 37 |
| `OpenProductDmnClient` | HTTP-client | **S** tot aangepast | Bouwt de `WebClient` in een `init`-blok en roept daar `ClientSslContextResolver.resolve(...)` en `getToken()` aan: dat kan bij het opstarten falen bij een ontbrekende of ongeldige DMN-SSL-config. Moet lui worden voordat de bean altijd geregistreerd kan worden | `zgw/openproduct/.../client/OpenProductDmnClient.kt` regels 41 tot 90 |
| `OpenProductService`, `OpenProductDmnService`, `OpenProductPrefillService` | Services | R | Vragen `ZakenApiService`, `TaakService`, `DocumentenApiService`, `ObjectsApiClient`; foutafhandeling kent al `ModuleNotConfiguredException` (`OpenProductService.kt` regel 1035) | `OpenProductAutoConfiguration.kt` |
| Alle `OpenProduct*Query` en `OpenProductMutation` (10 controllers) | GraphQL | R | `OpenProductGraphqlAutoConfiguration` hangt aan `@ConditionalOnBean(OpenProductService::class)`, een bean-conditie die bij altijd registreren zinloos wordt | `OpenProductGraphqlAutoConfiguration.kt` regel 38 |
| `ProductDocumentResource` + configurer | REST, security | R, S | | `OpenProductAutoConfiguration.kt` regel 119 |

### haalcentraal-hr (`haalcentraal.hr.enabled`)

| Wiring | Soort | Grens | Toelichting | Bron |
|---|---|---|---|---|
| `HandelsregisterClient` | HTTP-client | R | `webClient()` per call; `apiKey!!` bij gebruik (regel 82); `requireConfigured("haalcentraal-hr","url")` | `haalcentraal-hr/.../client/HandelsregisterClient.kt` |
| `HandelsregisterService`, `HandelsregisterQuery` | Service, GraphQL | R | | `HandelsregisterAutoConfiguration.kt` |

De module-sleutel is `haalcentraal.hr` (afwijkend van haalcentraal2); dat valt buiten scope van #379.

### haalcentraal2 (`haalcentraal2.enabled`; `Gemachtigde` ook `haalcentraal.hr.enabled`)

| Wiring | Soort | Grens | Toelichting | Bron |
|---|---|---|---|---|
| `HaalCentraal2BrpClient`, `HaalCentraal2BewoningClient` | HTTP-client | R | `webClient` `by lazy`; `requireConfigured` op URL, `apiKey!!` bij gebruik | `haalcentraal2/.../client/*.kt` |
| `HaalCentraal2Service` | Service | R | | `HaalCentraal2AutoConfiguration.kt` |
| `HaalCentraal2BrpQuery`, `...BewoningQuery` | GraphQL | R | Aparte configuratieklasse zonder `@AutoConfiguration`-annotatie, wel in `.imports` | `HaalCentraal2GraphQLConfiguration.kt` regel 27 |
| `HaalCentraal2GemachtigdeQuery` | GraphQL | R* | Hangt aan twee modules (`haalcentraal2` en `haalcentraal.hr`); gate op beide flags, `HandelsregisterService` moet dus ook zonder `haalcentraal.hr` beschikbaar zijn (of de query geeft de unavailable-fout) | regel 45 |

### payment-direct (`payment.direct.enabled`; webhook en controller ook `objectenapi.enabled`)

| Wiring | Soort | Grens | Toelichting | Bron |
|---|---|---|---|---|
| `DirectPaymentClient`, `DirectPaymentService` | HTTP-client, service | R | `webClient` `by lazy` | `payment-direct/.../autoconfiguration/DirectPaymentAutoConfiguration.kt` |
| `DirectPaymentMutation`, `DirectPaymentQuery` | GraphQL | R | Zonder eigen conditie, hangen aan de klassevlag | regels 71 tot 75 |
| `DirectPaymentController` (webhook) + `DirectPaymentWebhookService` | REST | R* | Een webhook van buiten: de gate moet de externe aanroeper een duidelijke status geven (503), niet een 404 | regels 59 tot 80 |

## Afhankelijkheidsgraaf (intern, zoals nu in code)

Gelezen uit de constructor-argumenten van de `@Bean`-methoden en uit de `build.gradle.kts`-dependencies:

```
objectenapi  <-  form, taak, zaken-api, berichten, payment-direct (webhook), openproduct
catalogiapi  <-  zaken-api, (besluiten via BesluitenQuery)
documentenapis <- zaken-api, besluiten, berichten, taak (resource), openproduct
zakenapi     <-  zaken, besluiten, openproduct
besluiten    <-  zaken (ZaakQuery)
taak         <-  openproduct, payment-direct (Gradle)
form         <-  taak (TaakFormQuery)
verificatie  <-  openklant (DigitaleAdres*, optioneel)
haalcentraal.hr <- haalcentraal2 (Gemachtigde)
```

## Verborgen koppelingen (bestaande gaten in de conditielijsten)

Dit zijn de plekken waar de code een bean vereist die niet in de `@ConditionalOnProperty`-lijst van de module staat.
Met de vlaggen "zakenapi + objectenapi + catalogiapi" aan en "documentenapis" of "besluitenapi" uit start de
applicatie waarschijnlijk niet (`NoSuchBeanDefinitionException`). Dit is niet gedraaid; het is gelezen uit de
constructors en moet met een startup-test bevestigd worden.

| Module | Vereist maar niet in de lijst | Bron |
|---|---|---|
| zaken (`ZaakQuery`) | `BesluitenService` (`besluitenapi`, `documentenapis`) | `ZakenAutoConfiguration.kt` |
| zaken-api (`ZakenApiService`) | `DocumentenApiService` (`documentenapis`) | `ZakenApiAutoConfiguration.kt` |
| besluiten (`BesluitenQuery`) | `CatalogiApiService` (`catalogiapi`) | `BesluitenAutoConfiguration.kt` |
| berichten | `ObjectenApiService` (`objectenapi`) | `BerichtenAutoConfiguration.kt` |
| taak (`TaakFormQuery`) | `ObjectsApiFormDefinitionService` (module `form`; volgt `objectenapi`, dus gedekt) | `TaakAutoConfiguration.kt` |
| openproduct | `catalogiapi` indirect via `ZakenApiService` | `OpenProductAutoConfiguration.kt` |

Dat de lijsten onvolledig zijn is op zichzelf het argument voor een expliciet afhankelijkheidsmodel (#383): met
altijd-geregistreerde beans verdwijnt de crash bij opstarten, en de gate checkt zelf of de module en haar
vereisten aan staan.

## Conclusie: de grens

**Startup-gebonden (blijft):**
- Security filter chain, CORS, JWT-decoder, authenticatieconverters.
- GraphQL-schema, scalars, `GraphQLExceptionHandler`.
- Property-binding en bijbehorende bean-instanties van `@ConfigurationProperties`.
- OpenFeature-provider, `FeatureGate`, `FeatureContextFactory`, `ObjectMapper`.
- Het feit welke module-jars op het classpath staan (Gradle).
- Geen persistentielaag.

**Per request te gaten (verplaatsen van `@ConditionalOnProperty` naar `FeatureGate`):**
- Alle `@QueryMapping`/`@MutationMapping`-controllers (zaken, besluiten, taak, berichten, openklant, openproduct,
  verificatie, haalcentraal-hr, haalcentraal2, payment-direct).
- Alle REST-resources: `ZaakDocumentResource`, `BesluitDocumentResource`, `BerichtDocumentResource`,
  `ProductDocumentResource`, `TaakDocumentResource`, `DirectPaymentController`.
- De virusscan-aanroep (`VirusScanService?` in `TaakDocumentResource`).
- Clients en services: ze kunnen altijd geregistreerd worden, omdat de `WebClient` lui wordt gebouwd en
  `requireConfigured` bij het eerste gebruik een `ModuleNotConfiguredException` geeft.
- De `HttpSecurityConfigurer`-beans van de document-resources: mogen altijd geregistreerd worden.

**Eerst aan te passen voordat "altijd registreren" veilig is:**
1. `OpenProductDmnClient`: `WebClient` in `init` (SSL-resolve en token bij opstarten) lui maken.
2. `DocumentenApiClient`, `VerificatieClient`, `HandelsregisterClient`, `HaalCentraal2*Client`: `secret!!`/`apiKey!!`
   vervangen door `requireConfigured`, anders geeft een ontbrekende secret een `NullPointerException` in plaats van
   een duidelijke "niet geconfigureerd"-melding.
3. `@ConditionalOnBean(OpenProductService::class)` op `OpenProductGraphqlAutoConfiguration` vervalt.
4. De `ModuleNotConfiguredException` heeft nu alleen in `OpenProductService` een afhandeling. Een centrale
   vertaling (GraphQL-fout via `GraphQLExceptionHandler`, HTTP 503 voor REST) ontbreekt en is de kern van het
   gedrag "API niet beschikbaar" uit de map-bestemming.
5. `ObjectsApiAutoConfiguration` e.a. gebruiken `@ConditionalOnMissingBean`; dit blijft nodig voor
   overschrijfbaarheid door integrators, ook als de property-conditie verdwijnt.

## Aanbevelingen voor de vervolgtickets (#382, #383)

- Splits de modulevlag in twee lagen: de gate (`FeatureGate`, per request, per tenant) bepaalt of een module
  beschikbaar is; `ModuleNotConfiguredException` bepaalt of een module bruikbaar is. Beide leveren dezelfde
  foutvorm ("niet beschikbaar"), maar met een andere oorzaak in de log.
- Houd het schema ongewijzigd (conform #143 en "Out of scope" van #379); velden van een uitgezette module geven de
  unavailable-fout in plaats van `null`.
- Leg de afhankelijkheidsgraaf hierboven vast in een expliciet model (bij startup valideren of fail-closed
  per request), en neem de gevonden verborgen koppelingen daarin op.
- Een startup-test per module-combinatie (alles uit, één module aan) is de manier om de bovenstaande
  `NoSuchBeanDefinitionException`-vermoedens te bevestigen of te weerleggen.

## Beperkingen van dit onderzoek

- Statische leesanalyse van `main`; er is geen applicatie gestart en geen test gedraaid.
- Alleen productiecode in `backend/` is bekeken. De frontend-`modules`-lijst (`modulesWhitelist`) is alleen vanaf
  de kant van de backend bekeken.
- `app-dev` is niet meegenomen (volgt uit #143 fase 4: verdwijnt).
- Regelnummers zijn die van commit `ea58815d1` en kunnen verschuiven.
