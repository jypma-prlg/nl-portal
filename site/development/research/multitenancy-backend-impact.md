# Impact van een gedeelde runtime op de backend, per module

Geraadpleegd op: 2026-11-06. Onderzoek voor #146, onderdeel van de kaart #197 (multitenancy: gedeelde runtime voor NL Portal).

> **Kader:** één backend-runtime bedient meerdere tenants (overheidsorganisaties, sleutel: **OIN**) met logische scheiding. De backend is stateless zonder database ([ADR 0013](../adr/0013-stateless-backend-zonder-database.md)); "schema per tenant" vervalt. Configuratie komt uit Git/ArgoCD en OpenFeature, secrets via External Secrets Operator; wijzigingen gaan altijd via een rolling restart, **nooit** via hot reload. Toetsniveau ASVS 5.0 L3.
>
> Regelnummers verwijzen naar `main` op commit `775f21874`.

## Samenvatting

- **Alle moduleconfiguratie is vandaag een singleton.** Elke module bindt één `@ConfigurationProperties`-klasse onder `nl-portal.config.<module>` en bouwt daarmee in een `@AutoConfiguration` één client-bean. Er is geen enkele plek waar een tenant als begrip bestaat. Bijna alles wat per tenant verschilt, zit in die properties: URL's, client-id/secret of token, `rsin`, objecttype-URL's, PSP-profielen, zaaktype-uitsluitingen, toegestane gemeenten voor bewoning.
- **De kern van de wijziging is één nieuwe core-seam, niet negentien modulewijzigingen.** Nodig zijn: (1) een `TenantResolver` die per request de tenant bepaalt, (2) een tenantcontext in de **Reactor Context** (hetzelfde mechanisme dat `ReactiveSecurityContextHolder` al gebruikt), en (3) een **per-tenant clientregister** dat de bestaande clients per tenant bouwt bij het opstarten. Daarna is de wijziging per module klein tot middelgroot: client-bean vervangen door een registeropzoeking.
- **Aanbeveling clientregister:** onveranderlijke `Map<Tenant, Client>` die bij het opstarten uit Git-configuratie wordt gebouwd (optie R1 hieronder), ingebouwd in de `zgw-client`-seam van [ADR 0002](../adr/0002-zgw-client-consolidation.md), die nog niet gebouwd is. Wijzigingen landen via een rolling restart; dat sluit aan op "nooit hot reload".
- **Aanbeveling tenantcontext:** een `WebFilter` bepaalt de tenant uit het **host-domein** (elk portaal heeft een gemeentespecifiek domein) en schrijft die met `contextWrite` in de Reactor Context. Daarna wordt **gecontroleerd dat het token bij dezelfde tenant hoort** (issuer of claim). Geen `ThreadLocal`.
- **Hoogste risico's:**
  1. `common-ground-authentication`: één Keycloak-client (id/secret) en één `jwk-set-uri` voor de hele runtime, en de token-exchange-URL wordt afgeleid uit de `iss`-claim van het binnenkomende token.
  2. `payment-direct`: de postsale-webhook gebruikt de JVM-globale `InMemorySecretKeyStore.INSTANCE`. Een `keyId` van tenant A kan daar die van tenant B overschrijven, en de webhook heeft geen tenantrouting.
  3. `documenten-api`: kiest de API-configuratie op basis van een `contains`-match op de document-URL. In een gedeelde registry kan zo de configuratie van de verkeerde tenant gekozen worden.
- **`@ConditionalOnProperty` blijft bestaan, maar betekent iets anders:** de vlag zegt dan "module is in deze runtime geïnstalleerd" (de unie van wat alle tenants nodig hebben). Of een module voor een tenant aan staat, wordt per request beslist op basis van tenantconfiguratie.
- **Bijvangst voor het ASVS-lijstje van de kaart:** `/actuator/refresh` en `/actuator/restart` staan open (`unrestricted` + `permitAll`). `/actuator/refresh` is bovendien precies de hot reload die de kaart uitsluit. CORS staat op `*`.

## 1. Bronnen

| Bron | Link | Opmerking |
|---|---|---|
| Code `backend/` | deze repo, commit `775f21874` | Primaire bron voor de inventarisatie |
| Spring Security, OAuth2 Resource Server Multitenancy (reactive) | https://docs.spring.io/spring-security/reference/reactive/oauth2/resource-server/multitenancy.html | `JwtIssuerReactiveAuthenticationManagerResolver`; gelezen via samenvattende fetch-tool |
| Spring Framework, Kotlin Coroutines (context propagation) | https://docs.spring.io/spring-framework/reference/languages/kotlin/coroutines.html | `PropagationContextElement`, `Hooks.enableAutomaticContextPropagation()` |
| Spring Framework, `CoWebFilter.kt` (bron) | https://github.com/spring-projects/spring-framework/blob/main/spring-web/src/main/kotlin/org/springframework/web/server/CoWebFilter.kt | Gelezen via `gh api`; `COROUTINE_CONTEXT_ATTRIBUTE`, sinds 6.0.5 |
| kotlinx-coroutines-reactor, `ReactorContext` | https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-reactor/kotlinx.coroutines.reactor/-reactor-context/ | Propagatie Reactor Context ↔ coroutine |
| Spring for GraphQL, Request Execution | https://docs.spring.io/spring-graphql/reference/request-execution.html | Reactor Context van WebFlux bereikt data fetchers |

Versies uit de build: Spring Boot 4.1.2 (`backend/gradle.properties:7`), kotlinx-coroutines 1.10.2, OpenFeature SDK 1.23.0, flagd-provider 0.15.0, Spring Cloud 5.0.4 (`backend/buildSrc/src/main/kotlin/Versions.kt:20-31`). GraphQL loopt via Spring for GraphQL (`backend/graphql/.../GraphqlAutoConfiguration.kt`, `RuntimeWiringConfigurer`).

## 2. Hoe configuratie en clients nu gebouwd worden

Het patroon is in elke module gelijk:

```kotlin
@AutoConfiguration
@EnableConfigurationProperties(ZakenApiConfig::class)
@ConditionalOnProperty(prefix = "nl-portal.config", name = ["objectenapi.enabled", "catalogiapi.enabled", "zakenapi.enabled"], havingValue = "true")
class ZakenApiAutoConfiguration {
    @Bean
    fun zakenApiClient(zakenApiConfig: ZakenApiConfig, catalogiApiConfig: CatalogiApiConfig, webClientBuilder: WebClient.Builder) =
        ZakenApiClient(zakenApiConfig.properties, catalogiApiConfig.properties, webClientBuilder)
}
```

(`backend/zgw/zaken-api/.../ZakenApiAutoConfiguration.kt:36,62-63`). De client bouwt één `WebClient` `by lazy` met een vaste `baseUrl` en een token uit de properties (`ZakenApiClient.kt:54-80`). Dit patroon zit ook in `besluiten`, `haalcentraal2`, `payment-direct`, `openproduct` en `verificatie` (`by lazy`), of de `WebClient` wordt per aanroep opnieuw gebouwd (`ObjectsApiClient.kt:120-133`, `DocumentenApiClient.kt:106-140`).

Gevolg: er is per proces precies één configuratie per module. Een tenant toevoegen betekent nu een extra deployment.

Wat al tenant-neutraal is:

- `IdTokenGenerator` heeft geen state; secret en client-id komen per aanroep binnen (`backend/zgw/idtoken-authentication/.../IdTokenGenerator.kt:26-53`).
- `ClientSslContextResolver.resolve(keyData, trustedCertificate)` krijgt sleutelmateriaal per aanroep (`backend/core/.../ssl/ClientSslContextResolver.kt:27-30`, `ResourceClientSslContextResolver.kt:30-55`).
- `documenten-api` heeft al een benoemde map van configuraties (`DocumentApisConfig.kt:50-71`), dus een klein register binnen één tenant.

## 3. Tabel: module → wijziging → omvang → risico

Omvang: **S** = binnen één klasse of autoconfig, **M** = autoconfig + client + service, **L** = nieuw mechanisme of beveiligingskritisch pad.

| Module | Wat per tenant verschilt (bron) | Wijziging | Omvang | Risico |
|---|---|---|---|---|
| **core (nieuw): tenancy** | n.v.t. | Nieuw: `Tenant` (OIN), `TenantResolver` (host → tenant), `TenantContext` in de Reactor Context, `TenantRegistry<T>`, binding van `nl-portal.tenants.<oin>.*`, validatie bij opstarten (fail fast), ArchUnit-regels (§6). | L | Hoog: alle scheiding hangt hiervan af. Wel goed te testen op één seam. |
| **core/security** | `jwk-set-uri` (één, `app/.../application.yml:28`), CORS `*` (`application.yml:55-60`), `unsecured` (`SecurityEndpointsConfig.kt:20-22`) | Resource server via `authenticationManagerResolver` (§5.2) in plaats van één `jwt { }` (`OauthSecurityAutoConfiguration.kt:92-96`). Controle "token-tenant = host-tenant". CORS-origins per tenant-domein. | M | Hoog: verkeerd gezet is cross-tenant toegang. `/graphql` staat op filterniveau op `permitAll` (`OauthSecurityAutoConfiguration.kt:82`); tenantresolutie moet daarom vóór de resolvers draaien, ook voor anonieme requests. |
| **common-ground-authentication** | Keycloak-client `resource`, `audience`, `credentials.secret` (`KeycloakConfig.kt:20-29`). Machtigingsdiensten-JSON (`AuthenticationMachtigingsDienstConfig.kt:21-35`) | Token exchange met de client van de tenant uit het register. Token-endpoint niet meer afleiden uit `jwt.issuer` (`CommonGroundAuthenticationConverter.kt:83`) maar uit de vertrouwde tenantconfiguratie. `ReactiveJwtDecoder` per issuer. Machtigingsdiensten per tenant laden; nu één keer in `init` (`AuthenticationMachtigingsDienstService.kt:31-43`). | L | Hoog. Vandaag beschermt alleen de handtekeningcontrole tegen één JWKS dat `iss` wordt vertrouwd. Bij meerdere issuers moet `iss` eerst tegen een allowlist (Spring-waarschuwing, §5.2). Anders gaat het client-secret naar een token-endpoint dat de aanvaller kiest. |
| **portal-authentication** | geen | Optioneel: tenant als veld op `PortalAuthentication`, als tweede bron naast de Reactor Context (§5.1 optie C4). | S | Laag |
| **idtoken-authentication** | geen state | Geen functionele wijziging. Wel de spreiding van `IdTokenGenerator()` opruimen (ADR 0002). Let op: HS256 met een gedeeld secret per ZGW-API (`IdTokenGenerator.kt:44-51`); secrets per tenant via ESO. | S | Laag |
| **core/ssl** | sleutelmateriaal per aanroep | Geen wijziging aan de resolver. De `SslContext` per tenant en API cachen in het register; `documenten-api` bouwt die nu per request (`DocumentenApiClient.kt:121-131`). `ResourceLoader`-paden per tenant (ESO-mount). | S | Laag |
| **core/featureflag** | `context.tenant` is een vaste property (`FeatureFlagProperties.kt:33`); `userIdSecret` "per tenant" maar één waarde (`:42`) | `FeatureContextFactory.create` (`FeatureContextFactory.kt:47-56`) haalt de tenant uit de `TenantContext` in plaats van uit properties; `userIdSecret` per tenant uit het register. `OpenFeatureAPI.getInstance()` (`FeatureFlagAutoConfiguration.kt:52`) is JVM-globaal, maar met één provider en een context per evaluatie is dat geen lek. | S/M | Middel: een vaste `tenant`-waarde laat flags voor alle tenants gelijk uitvallen. Zie ook §4 over de flagd-poll. |
| **core/frontend** | thema, logo, links, toggles, `modules-whitelist` (`FrontendConfigurationProperties.kt:22-64`), modulelijst (`FrontendModuleConfigurationProperties.kt:20-25`) | Per tenant uit het register. `/api/public/frontend` (`FrontendConfigurationResource.kt:28-37`) is anoniem, dus de tenant komt hier **alleen** uit het host-domein. `getModules()` (`FrontendConfigurationService.kt:60-65`) per tenant. | M | Middel: een fout toont het thema of de modules van een andere gemeente. Geen persoonsgegevens. |
| **zaken-api** | `url`, `clientId`, `secret`, `zaakTypesIdsExcluded`, `useNnpKvkQueryIdentificators` (`ZakenApiConfig.kt:21-31`) | Client per tenant via het register. Service leest uitsluitingen uit tenantconfig. | M | Middel: zaakdata van de verkeerde bron is een datalek. |
| **zaken** (orkestratie) | geen eigen config | Volgt `zaken-api`, `besluiten` en `catalogi-api`. Document-endpoint `ZaakDocumentResource.kt:35-39` gebruikt de tenantcontext. | S | Laag |
| **catalogi-api** | `url`, `clientId`, `secret` (`CatalogiApiConfig.kt:20-28`) | Client per tenant (`CatalogiApiAutoConfiguration.kt:37-40`). | S | Laag (catalogusdata) |
| **besluiten** | `url`, `clientId`, `secret` (`BesluitenApiConfig.kt:20-28`) | Client per tenant (`BesluitenAutoConfiguration.kt:43-44`). | S | Middel |
| **documenten-api** | per benoemde API: `url`, `clientId`, `secret`, **`rsin`**, `documentTypeUrl`, `ssl` (`DocumentApisConfig.kt:64-71`), plus whitelists en MIME-types (`:36-50`) | Configuratiemap per tenant. `getConfigForDocumentUrl` (`:56-62`) gebruikt `documentUrl.contains(url)`: vervangen door een prefixmatch op de genormaliseerde URL, **binnen** de tenant. `SslContext` cachen. ClamAV (`ClamAVVirusScanConfig.kt:20-27`) kan gedeelde infra blijven. | M | Hoog: `rsin` en de keuze van de bron bepalen waar een upload landt. Een substringmatch over tenants heen kan een document naar de bron van een andere gemeente sturen. |
| **objectenapi** | `url`, `token` (`ObjectsApiClientConfig.kt:21-28`) | Client per tenant (`ObjectsApiAutoConfiguration.kt:32`). `getObjectByUrl` stuurt de token naar elke URL die binnenkomt (`ObjectsApiClient.kt:58-64,128-133`): beperken tot het host-domein van de tenant. | M | Hoog: wordt door `taak`, `berichten`, `form`, `payment-direct` en `openproduct` gebruikt. Tokenlek naar een andere host. |
| **openklant2** | `contactgegevensApiUrl`, `klantinteractiesApiUrl`, `token`, `digitalAdressenReferentie`, codes (`OpenKlantModuleConfiguration.kt:21-35`) | Client per tenant (`OpenKlantAutoConfiguration.kt:30`). | M | Middel: contactgegevens zijn persoonsgegevens. |
| **openproduct** | `productApiUrl`, `productTypeApiUrl`, `token`, DMN (`clientId`, `secret`, `username`, `password`, `ssl`), prefill `typeUrl` (`OpenProductModuleConfiguration.kt:22-43`) | Drie clients per tenant (`OpenProductAutoConfiguration.kt:51-93`); DMN- en prefillconfiguratie per tenant. | M | Middel |
| **taak** | `objectTypeUrl` (`TaakConfig.kt:20-26`) | Objecttype-URL per tenant; volgt het `objectenapi`-register. | S | Middel: een verkeerde objecttype-URL filtert taken van een andere tenant als die dezelfde Objecten-API delen. |
| **berichten** | `berichtObjectTypeUrl` (`BerichtenConfiguration.kt:20-26`) | Idem | S | Middel |
| **form** | `formDefinitionObjectTypeUrl` (`FormConfig.kt:20-25`) | Idem | S | Laag |
| **verificatie** | `url`, `clientId`, `secret`, `apiKey`, templates, `ssl`, `typesNeedVerification` (`VerificatieModuleConfiguration.kt:23-36`) | Client per tenant (`VerificatieAutoConfiguration.kt:34-40`). | S/M | Middel |
| **haalcentraal2** | `brpApiUrl`, `bewoningApiUrl`, `apiKey`, headers, `ssl`, **`allowedCitiesForBewoning`**, `brpFields` (`HaalCentraal2ModuleConfiguration.kt:21-33`) | Twee clients per tenant (`HaalCentraal2AutoConfiguration.kt:39-57`). `allowedCitiesForBewoning` en `brpFields` per tenant (doelbinding BRP). De gemachtigde-query hangt aan de HR-module (`HaalCentraal2GraphQLConfiguration.kt:45`); per tenant opnieuw toetsen. | M | Hoog: de BRP-velden zijn per gemeente doelgebonden. Een gedeelde `brpFields` geeft een tenant mogelijk meer velden dan zijn grondslag toestaat. |
| **haalcentraal-hr** | `url`, `apiKey`, `ssl` (`HaalCentraalHrConfig.kt:21-29`) | Client per tenant (`HandelsregisterAutoConfiguration.kt:42-53`). | S | Laag/middel |
| **payment-direct** | `url`, `ssl`, `webhookUrl`, profielen met `pspId`, `apiKey`, `apiSecret`, `webhookApiKey`, `webhookApiSecret`, `returnUrl` (`DirectPaymentModuleConfiguration.kt:21-58`) | (1) `InMemorySecretKeyStore.INSTANCE` (`DirectPaymentWebhookService.kt:41,45`) vervangen door een eigen `SecretKeyStore` die `keyId → (tenant, secret)` bijhoudt en bij een dubbele `keyId` het opstarten weigert. (2) Postsale (`DirectPaymentController.kt:28-42`) is anoniem: de tenant komt uit het host-domein van `webhookUrl` en moet gelijk zijn aan de tenant van de `keyId`. (3) Daarna `ObjectsApiClient` van die tenant (`DirectPaymentWebhookService.kt:80-89`). (4) Client en profielen per tenant. | L | Hoog: vandaag overschrijft een tweede `keyId` stil een eerste. Een geldig ondertekende webhook kan een taak in de Objecten-API van een andere tenant op `AFGEROND` zetten als de routing alleen op `orderId` vertrouwt. |
| **graphql** | geen | Geen wijziging. Schema blijft de unie van alle geïnstalleerde modules (`spring.graphql.schema.locations`, `application.yml:32-33`). Gating per tenant via een `WebGraphQlInterceptor` of in de resolvers. | S | Laag |
| **app** (configuratie) | alles hierboven | Configuratieschema naar `nl-portal.tenants.<oin>.config.<module>`. Actuator `refresh`/`restart` dichtzetten (`application.yml:10-19`, `OauthSecurityAutoConfiguration.kt:84-85`). | M | Middel |

## 4. Tenantgevoelige globale toestand

| Plek | Wat | Gevolg bij multitenancy | Voorstel |
|---|---|---|---|
| `DirectPaymentWebhookService.kt:41,45` | `InMemorySecretKeyStore.INSTANCE` (`com.onlinepayments`-SDK, JVM-singleton), gevuld in `init` | Eén `keyId`-namespace voor alle tenants. Dubbele sleutels overschrijven elkaar stil. Ook twee bean-instanties (tests) delen deze store. | Eigen `SecretKeyStore`-implementatie per tenant of met tenantkoppeling. Bij een dubbele `keyId` niet opstarten. |
| `FeatureFlagAutoConfiguration.kt:52` | `OpenFeatureAPI.getInstance()` | Globaal, maar één provider voor alle tenants is de bedoeling: de tenant zit in de `EvaluationContext`. Geen lek, mits `FeatureContextFactory` de juiste tenant zet. | Tenant uit de `TenantContext`. Geen globale `setEvaluationContext` gebruiken (dat zou wél lekken). |
| `FeatureFlagProperties.kt:26` + `FeatureFlagAutoConfiguration.kt:45-46` | flagd file-provider pollt het bestand elke 5 s | Flagwijzigingen worden zonder herstart actief. Dat is een vorm van hot reload, en de kaart zegt "nooit hot reload". | Expliciet besluiten in de ADR: flags zijn runtime per definitie, configuratie niet. Of de poll uitzetten en flags ook via een rolling restart laten landen. |
| `KeycloakConfig.kt:20-29`, `CommonGroundAuthenticationConverter.kt:42,83,98-99` | Eén client-secret, één `WebClient.create()`, token-endpoint afgeleid uit `iss` | Zie tabel | Per tenant uit het register. Token-endpoint uit config, niet uit het token. |
| `application.yml:28` | Eén `jwk-set-uri` | Eén issuer-sleutelset | §5.2 |
| `AuthenticationMachtigingsDienstService.kt:31-43` | Machtigingsdiensten-JSON eenmalig geladen in `init` | Eén set voor alle tenants | Per tenant laden in het register |
| `ZakenApiClient.kt:54`, `BesluitenApiClient.kt:39`, `HaalCentraal2BrpClient.kt:38`, `HaalCentraal2BewoningClient.kt:38`, `DirectPaymentClient.kt:50`, `OpenProductClient.kt:37`, `OpenProductTypeClient.kt:37`, `VerificatieClient.kt:47` | `val webClient by lazy` met vaste `baseUrl` | Geen lek op zich, maar één instantie = één tenant | Eén clientinstantie per tenant in het register |
| `app/.../application.yml:10-19` + `OauthSecurityAutoConfiguration.kt:84-85` | `/actuator/refresh` en `/actuator/restart` `unrestricted` en `permitAll` | Anonieme herstart van de gedeelde runtime raakt **alle** tenants (DoS). `refresh` herbindt `@ConfigurationProperties` (Spring Cloud): hot reload. | Buiten tenancy beleggen (ASVS-bijvangst op de kaart). Endpoints uitzetten. |

Er zijn geen `ThreadLocal`, `GlobalScope`, `runBlocking`, `@Scheduled`, `@Cacheable` of `ConcurrentHashMap` in productiecode gevonden (`grep` op `backend/**/main`). Er zijn dus geen achtergrondtaken die de tenantcontext kwijtraken en geen caches die tenantdata kunnen mengen. De `object`-singletons (`Mapper`, `ObjectValidator`, `ZgwUrls`, `FilenameSanitizer`, `PrefillSecurity`) hebben geen tenantgebonden state.

## 5. Opties

### 5.1 Tenantcontext: bepalen en doorgeven

**Bepalen.** Elk portaal draait op een gemeentespecifiek domein (kaart #197). Het host-domein is daarom de enige bron die op **alle** inbound paden bestaat: geauthenticeerde GraphQL- en REST-calls, de anonieme `/api/public/frontend` en de server-to-server-postsale-webhook. Het token is een tweede bron. Die moet overeenkomen met de host, maar kan hem niet vervangen, omdat anonieme paden geen token hebben.

- Host komt via de ingress. Vertrouw `X-Forwarded-Host` alleen als de ingress dat afdwingt (`server.forward-headers-strategy`). Map host → OIN via een allowlist uit de tenantconfiguratie. Een onbekende host geeft 404 zonder fallback naar een standaardtenant.
- Bij een geauthenticeerd request: tenant uit het token (issuer per realm, of een tenantclaim) **moet gelijk zijn** aan de host-tenant, anders 403. Dit dekt ook het open ASVS-punt over `iss`/`aud`-validatie (zie [asvs-5-l3-token-exchange.md](./asvs-5-l3-token-exchange.md)).

**Doorgeven.** WebFlux met coroutines; `ThreadLocal` valt af (`.out-of-scope/virtual-threads.md`).

| Optie | Hoe | Voor | Tegen |
|---|---|---|---|
| **C1. Reactor Context (aanbevolen)** | `WebFilter` doet `chain.filter(exchange).contextWrite { it.put(Tenant::class.java, tenant) }`. Lezen met `Mono.deferContextual` of in een `suspend fun` via `coroutineContext[ReactorContext]`. | Hetzelfde mechanisme als `ReactiveSecurityContextHolder`, dat de code al in coroutines gebruikt (`DocumentenApiService.kt:69-72`, `FeatureContextFactory.kt:41-45`). kotlinx-coroutines-reactor propageert de Reactor Context naar coroutines en terug: "implicitly propagated through subscribers' context by all Reactive integrations" (bron: `ReactorContext`-docs). Spring for GraphQL geeft de Reactor Context van de WebFlux-keten door aan data fetchers (bron: Request Execution). Eén filter dekt REST en GraphQL. | Impliciet: een vergeten `contextWrite` of een nieuwe `CoroutineScope(...)` verliest de tenant. Opvangen met fail-closed (`TenantContext.current()` gooit een exceptie als de tenant ontbreekt) en een ArchUnit-regel (§6). |
| C2. `CoWebFilter` + `CoroutineContext`-element | `CoWebFilter` zet de coroutinecontext op `COROUTINE_CONTEXT_ATTRIBUTE`, en `InvocableHandlerMethod` geeft die aan suspend-controllers (bron: `CoWebFilter.kt`, sinds 6.0.5). | Idiomatisch Kotlin | Alleen gedocumenteerd voor `InvocableHandlerMethod` (REST-controllers). Niet aangetoond voor GraphQL-controllers van Spring for GraphQL. Voor de GraphQL-hoofdroute dus onzeker. |
| C3. Expliciete parameter | `tenant: Tenant` als argument door services en clients | Maximaal expliciet, compiler dwingt het af | Raakt vrijwel elke service- en clientsignatuur. Veel churn bij 19 modules. |
| C4. Tenant op `PortalAuthentication` | Converter zet de tenant op het authentication-object | Komt gratis mee via `ReactiveSecurityContextHolder` | Werkt niet op anonieme paden (frontend-config, webhook). Alleen als controlebron naast C1. |

Voor logging en tracing per tenant (MDC) is `io.micrometer:context-propagation` met `Hooks.enableAutomaticContextPropagation()` de route die Spring documenteert (bron: Kotlin Coroutines-docs). Dat valt onder "observability per tenant" op de kaart en niet onder dit ticket.

### 5.2 Inkomende tokens per tenant (resource server)

Spring Security heeft hiervoor `JwtIssuerReactiveAuthenticationManagerResolver`:

```java
JwtIssuerReactiveAuthenticationManagerResolver resolver =
    JwtIssuerReactiveAuthenticationManagerResolver.fromTrustedIssuers(issuerA, issuerB);
http.oauth2ResourceServer(o -> o.authenticationManagerResolver(resolver));
```

De `JwtReactiveAuthenticationManager` per issuer wordt pas bij het eerste request met die issuer gemaakt, zodat het opstarten niet afhangt van de IdP. De docs waarschuwen: "It would be unsafe to simply take any issuer and construct an `ReactiveAuthenticationManager` from it". Gebruik dus een allowlist (bron: Spring Security Multitenancy).

- **Realm per tenant** (issuer verschilt per tenant): `fromTrustedIssuers` met de issuers uit de tenantconfiguratie. Daarna controleert een eigen stap issuer → OIN gelijk aan host → OIN. De `jwtAuthenticationConverter` (`OauthSecurityAutoConfiguration.kt:93-95`) moet per manager meegegeven worden. Dat kan met de constructorvariant die per issuer een eigen `ReactiveAuthenticationManager` teruggeeft.
- **Eén realm, tenant als claim**: één decoder. Valideer `iss` en `aud` expliciet en lees de tenant uit een claim (bijv. `azp` per tenant-client, of een eigen `oin`-claim). De keuze tussen deze twee hoort bij het IdP-onderzoek van de kaart. Dit onderzoek beslist dat niet.

Bij beide varianten moet de token exchange (`CommonGroundAuthenticationConverter.kt:80-109`) het token-endpoint en de clientgegevens uit de tenantconfiguratie halen, niet uit `jwt.issuer`.

### 5.3 Per-tenant clientregister

| Optie | Hoe | Voor | Tegen |
|---|---|---|---|
| **R1. Onveranderlijk register bij opstarten (aanbevolen)** | `TenantRegistry<T>`: `Map<Oin, T>` die bij het opstarten gebouwd wordt uit `nl-portal.tenants.<oin>.config.<module>` (Spring Boot bindt dit als `Map<String, …>`). De module levert een factory `(TenantModuleConfig) -> Client`; aanroepers gebruiken `registry.current()` (leest de `TenantContext`, fail-closed). Ingebouwd in de `AuthenticatedZgwClient` van [ADR 0002](../adr/0002-zgw-client-consolidation.md). | Past bij "Git + rolling restart, nooit hot reload": de map verandert alleen bij een herstart. Configuratiefouten vallen bij het opstarten op, tijdens de canary, niet tijdens een request. `SslContext`, `WebClient` en connection pool per tenant en API worden één keer gebouwd. Eenvoudig te testen. | 50 tenants × ~12 API's ≈ 600 `WebClient`s. Elke `WebClient` deelt standaard de Reactor Netty-`LoopResources`, maar per `HttpClient.create()` komt een eigen `ConnectionProvider`. Meten of één gedeelde provider volstaat. Een tenant toevoegen vraagt een herstart van de gedeelde runtime (rolling, dus zonder downtime). |
| R2. Eén tenantbewuste client per module | Eén `WebClient`; een `ExchangeFilterFunction` zet per request `baseUrl` en `Authorization` op basis van de Reactor Context | Weinig objecten | mTLS (`ssl` in documenten-api, haalcentraal, verificatie, payment-direct, openproduct-DMN) zit in de `HttpClient` en kan niet per request wisselen, dus voor die modules is R2 niet bruikbaar. Relatieve URI's moeten herschreven worden. Fouten in de filter lekken credentials naar de host van een andere tenant. |
| R3. Child-`ApplicationContext` per tenant | Gedeelde parent (security, GraphQL, WebFlux). Per tenant een child-context die de bestaande autoconfigs met tenant-properties draait. | Bestaande `@ConditionalOnProperty` en autoconfig blijven ongewijzigd en gelden per tenant. Activatie per tenant volgt vanzelf. | De GraphQL-schema- en controllerbeans zitten in de child. Routering van parent naar de juiste child is eigen maatwerk dat niemand in Spring standaard ondersteunt. 50 contexts kosten opstarttijd en geheugen. Moeilijk te beoordelen voor een Java-dev, en lastig met ArchUnit af te dwingen. |
| R4. Lazy register met hot reload | Clients op aanvraag bouwen; config live herladen | Tenant toevoegen zonder herstart | Valt af: in strijd met "nooit hot reload". Fouten verschijnen pas tijdens een request. |

**Advies:** R1. De wijziging per module is dan: de client-bean uit de autoconfig wordt een factory, en de service vraagt `registry.current()` in plaats van een geïnjecteerde client. Doe dit in de stap waarin ADR 0002 de clients toch al naar `zgw-client` verhuist, dan wordt elke client maar één keer aangeraakt.

### 5.4 Module-activatie per tenant

Vandaag bepaalt `@ConditionalOnProperty(... "enabled" ...)` of een module **bestaat** (beans, GraphQL-resolvers, REST-controllers). Voorbeelden: `ZakenApiAutoConfiguration.kt:36`, `OpenProductAutoConfiguration.kt:47`, `DirectPaymentAutoConfiguration.kt:35`. In een gedeelde runtime:

1. **Installatie (runtime-niveau):** `@ConditionalOnProperty` blijft, maar is de unie: een module is geïnstalleerd als minstens één tenant haar gebruikt. Zo blijft het GraphQL-schema stabiel per release.
2. **Activatie (tenant-niveau):** `nl-portal.tenants.<oin>.config.<module>.enabled`. Een tenant zonder configuratie heeft geen entry in het register, en `registry.current()` gooit dan `ModuleNotConfiguredException` (bestaat al: `core/util/ModuleConfiguration.kt:22`). Die wordt naar GraphQL vertaald als "module niet beschikbaar". De frontend-modulelijst (`FrontendConfigurationService.kt:60-65`) komt per tenant uit hetzelfde register, zodat de UI niets toont wat de backend weigert.
3. **Afhankelijkheden** tussen modules (bijv. `openproduct` vereist `objectenapi`, `taak`, `zakenapi`, `documentenapis`: `OpenProductAutoConfiguration.kt:47`) worden per tenant bij het opstarten gevalideerd, niet meer via de `name = [...]`-lijst in de annotatie.
4. Een OpenFeature-flag is **niet** de plek voor module-activatie. Een module zonder configuratie (URL, secret) kan niet aan, dus activatie is configuratie. Flags blijven voor release- en ops-toggles ([ADR 0014](../adr/0014-feature-flags-via-openfeature.md)).

### 5.5 Inbound endpoints met tenantrouting

| Endpoint | Auth | Tenantbron |
|---|---|---|
| `POST /api/public/payment/direct/postsale` (`DirectPaymentController.kt:28-42`) | Anoniem, HMAC via `X-GCS-Signature`/`X-GCS-KeyId` | Host van `webhookUrl` **en** tenant van de `keyId`; beide moeten gelijk zijn |
| `GET /api/public/frontend` (`FrontendConfigurationResource.kt:28-37`) | Anoniem | Alleen de host |
| `GET /api/public/ping` (`PingResource.kt:26`) | Anoniem | Niet nodig |
| `/graphql` (`OauthSecurityAutoConfiguration.kt:82`) | `permitAll` op filterniveau, autorisatie in resolvers | Host + token |
| Documentdownloads: `/api/zakenapi/zaakdocument/...` (`ZaakDocumentResource.kt:35-39`), `/api/berichten/...` (`BerichtDocumentResource.kt:35-39`), `/api/besluiten/...` (`BesluitDocumentResource.kt:33-37`), `/api/product/...` (`ProductDocumentResource.kt:33-37`), `/api/taak` upload (`TaakDocumentResource.kt:39-46`) | Token | Host + token |
| `/actuator/**` (`OauthSecurityAutoConfiguration.kt:84-85`) | `permitAll` | n.v.t.; dichtzetten (bijvangst) |

## 6. Afdwingen met architectuurchecks

`backend/architecture-test` gebruikt ArchUnit (`ArchitectureRules.kt:18-25`) met lagen per Gradle-module. Regels die de scheiding bewaken:

1. **Feature-modules injecteren geen `*Config`/`*Configuration`-klasse met `@ConfigurationProperties` meer buiten hun autoconfig.** Services lezen configuratie alleen via het register. ArchUnit: `noClasses().that().resideOutsidePackage("..autoconfigure..").should().dependOnClassesThat().areAnnotatedWith(ConfigurationProperties)`.
2. **Geen `WebClient.create()` / `WebClient.builder()` buiten `zgw-client` en core.** Sluit aan op ADR 0002 en vangt `CommonGroundAuthenticationConverter.kt:42`.
3. **Geen JVM-globale stores:** geen verwijzing naar `InMemorySecretKeyStore`, `OpenFeatureAPI.getInstance` buiten `core.featureflag`, en geen `ThreadLocal`.
4. **Geen nieuwe coroutine-scopes:** geen verwijzing naar `kotlinx.coroutines.GlobalScope`, `CoroutineScope(...)`-constructors of `runBlocking` in `main`. Dit bewaakt dat de Reactor Context (en dus de tenant) niet wegvalt.
5. **Fitness-test op de configuratie:** elke tenant in `nl-portal.tenants` levert bij het opstarten een volledig register op (bestaand patroon: `CapabilityInventoryFitnessTest.kt`).

## 7. Niet geverifieerd

- Of Spring for GraphQL de `CoWebFilter`-coroutinecontext (optie C2) doorgeeft aan suspend-controllers. Niet gevonden in de docs; reden om C1 te kiezen.
- Gedrag en geheugengebruik van ~600 `WebClient`-instanties met afzonderlijke `ConnectionProvider`s: niet gemeten.
- Of de `com.onlinepayments`-SDK een eigen `SecretKeyStore`-implementatie toestaat naast `InMemorySecretKeyStore`. Afgeleid uit de constructor `WebhooksHelper(marshaller, secretKeyStore)` (`DirectPaymentWebhookService.kt:41`); de SDK-docs zijn niet gelezen.
- Of PSP-profielen van verschillende gemeenten dezelfde `keyId` kunnen hebben (één Pink-merchant-account versus één per gemeente): navragen.
- De multitenancy-pagina van Spring Security is via een samenvattende fetch-tool gelezen; de `CoWebFilter`-bron direct via `gh api`.
