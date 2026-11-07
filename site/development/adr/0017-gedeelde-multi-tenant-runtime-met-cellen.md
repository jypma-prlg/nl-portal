# 17. Gedeelde multi-tenant runtime met logische tenantscheiding, ingedeeld in cellen

## Status

Voorgesteld, ter review bij senior devs. Voortgekomen uit wayfinder-kaart #197, ticket #205. Nog niets
hiervan is gebouwd; er draait nog niets in PRD, dus wijzigingen zijn nu goedkoop.

Raakt eerdere ADR's:

- [ADR 0016](./0016-stateless-configuratie-en-thema-via-bestanden.md) (ook voorgesteld): punt 2 ("één
  deployment is één gemeente, geen tenantresolutie per request") en punt 6 ("module aan/uit blijft een
  opstart-keuze") worden bij acceptatie van deze ADR gewijzigd. Het overige van 0016 (config-files,
  fail-fast, geen hot reload, thema als bestand) blijft staan en wordt hier per tenant toegepast.
- [ADR 0013](./0013-stateless-backend-zonder-database.md): ongewijzigd. Deze keuze steunt erop; "schema per
  tenant" bestaat niet.
- [ADR 0014](./0014-feature-flags-via-openfeature.md): aangevuld met een permission-flag per module per
  tenant en een expliciete uitzondering op "nooit hot reload" (besluit 5).
- [ADR 0002](./0002-zgw-client-consolidation.md): de `zgw-client`-seam (nog niet gebouwd) is de plek waar het
  per-tenant clientregister landt.

**Leeswijzer voor markeringen.** *(onderzoek)* betekent: bevinding uit het onderzoek achter dit voorstel,
niet door ons zelf in onze omgeving gecontroleerd. *(niet geverifieerd)* betekent: het onderzoek zelf
markeert dit als niet gecontroleerd. *(prototype)* betekent: bewezen in een weggooibaar prototype, nog
niet in de echte code. *(redactie)* betekent: een afleiding bij het schrijven van deze ADR, niet uit een
eerder besluit; die verdient bij de review extra aandacht. De onderzoeksbestanden staan op de branches
`research/multitenancy-*` en het prototype op `prototype/tenantcontext-seam`; geen van beide wordt
gemerged. Ze zijn in [Bronnen](#bronnen) opgesomd.

## Context

NL Portal wordt door Pink als SaaS-dienst aangeboden aan overheidsorganisaties (meestal gemeenten). Een
**tenant** is zo'n organisatie met één portaal, met het **OIN** als sleutel. De verwachte groei is circa
50 tenants binnen twee jaar. Vandaag is NL Portal één tenant per deployment (ADR 0016 bevestigt dat); 50
gemeenten betekent dan 50 deployments, 50 rollouts en 50 plekken waar een fout kan zitten.

Randvoorwaarden:

- **BIO 2.x** is verplicht (voor de gemeente; Pink valt er als leverancier onder via inkoop). **OWASP ASVS
  5.0 L3** is het toetsniveau.
- De backend is stateless en heeft geen database (ADR 0013).
- Er zijn twee gedeelde Keycloaks per omgeving: een **publieke ID-broker** (DigiD, eHerkenning, wallets;
  lokale login alleen op acceptatie) en een **private ID-broker** (beheer, federeert naar Entra ID). De
  portal-runtime accepteert nooit tokens van de private broker; het internet-facing portaal heeft geen
  beheertoegang.
- Het portaal draait altijd op een gemeentespecifiek domein (eigendom gemeente of Pink); het
  certificaatproces bestaat al.
- Configuratie loopt alleen via Git (uitgerold door gitops-tooling) en OpenFeature. Het Configuration Panel
  vervalt. Wijzigingen gaan altijd via een rolling restart.
- **Context: ArgoCD.** De beoogde gitops-tool is ArgoCD, maar de Helm-charts blijven gitops-tool-agnostisch:
  deze ADR schrijft geen Argo-specifieke configuratie voor in de charts. Argo is alleen de context waartegen
  we verifiëren dat de generieke charts niet botsen met Argo-best practices (zie de verificatiestap onder
  [Vóór livegang](#vóór-livegang-en-na-livegang)).
- DigiD loopt via een eigen Pink DigiD-Keycloak-plugin (jar); sommige tenants gaan via een makelaar
  (Oosterhout: Signicat). De IdP-inrichting verschilt dus per tenant.
- Ingress is de Kubernetes **Gateway API** (`HTTPRoute`), niet ingress-nginx.
- Ingrepen in de core moeten door een Java-dev te beoordelen zijn en door architectuurchecks worden
  afgedwongen (`backend/architecture-test`).

Wat het onderzoek over de kaders zegt *(onderzoek)*: geen van beide kaders sluit een gedeelde runtime
uit. BIO2 vraagt dat informatie met specifiek belang "fysiek en/of logisch" wordt geïsoleerd
(overheidsmaatregel 8.03.01); ASVS vraagt cross-tenant controls (8.4.1, L2) en geen procesisolatie. De
voorwaarden zitten in sleutels, tokens, logging en bewijs. De gedeelde runtime is wel één "gescheiden
groep" (BIO 8.22.01): de strengste tenant bepaalt het niveau, en elke release raakt alle tenants.

## Overwogen opties

### Vorm van de runtime

| | A. Instance per tenant | B. Eén gedeelde pool | C. Gedeelde runtime met cellen (gekozen) |
|---|---|---|---|
| Wat | Eigen deployment per tenant (status quo, ADR 0016) | Eén deployment voor alle tenants | Dezelfde runtime (image en chart), een deployment per groep tenants |
| Beheer bij 50 tenants | 50 rollouts, 50 monitors, 50 ingress-regels | Eén | Aantal cellen, klein |
| Gevolg van een fout of kwetsbaarheid | Eén tenant | Alle tenants | Eén cel |
| Secrets in geheugen van een pod | Eén tenant | Alle tenants (restrisico bij ASVS 13.3.2) | Alleen tenants van de cel |
| Beveiligingsniveau (BIO 8.22.01, 5.12.01) | Per tenant | De strengste tenant bepaalt alles | De strengste tenant per cel; hogere eisen = eigen cel |
| Canary-release | Per tenant, handwerk | Alleen met verkeerssplitsing | Een cel is de canary |
| Hogere SLA of fysieke scheiding | Vanzelf | Niet mogelijk | Eigen cel |
| Extra codepad | Nee | Nee | Nee |

- **A** is de uitwijkoptie, geen apart pad: een tenant met een eis tot fysieke scheiding krijgt een cel met
  één tenant. Als standaard faalt A op de kosten van 50 deployments en op het ontbreken van één plek om
  een release te sturen.
- **B** is C met één cel. Ze is eenvoudiger, maar maakt elke fout en elke kwetsbaarheid vlootbreed, laat
  alle tenantsecrets in elke pod staan en dwingt het hoogste beveiligingsniveau over iedereen af. B is
  daarom niet de ontwerpkeuze, maar wel een geldige beginconfiguratie van C (een cel) voor dev en acc.
- **C** kost één extra begrip (de cel) en een routering van domein naar cel. Dat is de prijs voor beperking
  van de gevolgen van een fout, canary-releases en een uitwijkroute voor zwaardere eisen.

Voor de frontend geldt hetzelfde *(onderzoek)*: een eigen frontend-deployment per tenant (optie A van het
frontend-onderzoek) botst met C; nginx kan per host het juiste configuratiebestand kiezen (besluit 7).

## Besluit

Dit is een voorstel. Elk besluit noemt de verworpen alternatieven.

### 1. Gedeelde runtime, ingedeeld in cellen

1. **De cel is de indelingseenheid.** Een cel is dezelfde multi-tenant runtime met een eigen deployment
   voor een groep tenants. Het tenantbestand bevat per tenant de cel; de ingress routeert het domein naar
   de cel; de runtime laadt alleen de tenants van zijn eigen cel en krijgt alleen de secrets van die
   tenants gemount (zie ESO hieronder). Dat verkleint het restrisico dat een pod alle tenantsecrets in
   geheugen heeft (ASVS 13.3.2).
2. **Livegang met minimaal twee productiecellen: canary en hoofdcel.** De canarycel bevat tenants die bewust
   eerst een release krijgen: een Pink-eigen tenant en één of twee pilotgemeenten. Later komen cellen bij,
   bijvoorbeeld per SLA of risicobereidheid. Het aantal tenants per cel is een richtwaarde die nog met
   belastingtests onderbouwd moet worden; die is er niet (zie [Open punten](#open-punten-en-voorbehouden)).
3. **Een cel is ook een beveiligingsgroep.** Elke cel krijgt een vastgelegd beveiligingsniveau (BIO
   8.22.01). Een tenant met hogere eisen of een eis tot fysieke scheiding krijgt een eigen cel;
   instance-per-tenant is een cel met één tenant. Zelfde techniek, geen apart codepad.
4. **Canary is een cel, geen percentage verkeer.** Een release gaat eerst naar de canarycel, blijft daar een
   vaste tijd draaien met automatische controles (foutpercentage, cross-tenant-tests) en gaat daarna naar
   de hoofdcel. Een cel is een Helm-release met eigen values (cel, replica's, tenantlijst); binnen een cel
   geldt de standaard Kubernetes rolling update en is geen progressive-delivery-controller nodig. De
   uitrolvolgorde canarycel naar hoofdcel en de wachttijd en health-controle ertussen zijn **eisen aan de
   gitops-tooling**, niet aan de chart.
5. **Hoge beschikbaarheid per cel:** minimaal 2 replica's, een PodDisruptionBudget (altijd minimaal 1
   beschikbaar) en anti-affinity over nodes of zones. Een cel met een hogere SLA krijgt meer replica's. Dit
   hoort bij de celdefinitie.
6. **Een wijziging aan één tenant herstart alleen zijn cel.** Door fail-fast (besluit 4) worden pods met een
   foute configuratie niet ready en blijven de oude pods draaien.

Verworpen alternatieven: zie de tabel hierboven. Voor de canary-vorm: *percentage verkeer* (bijvoorbeeld met
een progressive-delivery-controller of weighted routes) is verworpen omdat tenantverkeer dan over versies verspringt en een
tenant tijdens een release van versie wisselt; een cel geeft een vaste tenantgroep per versie.

**Eisen aan de gitops-tooling voor de uitrolvolgorde.** De chart bevat geen uitrolvolgorde. De tooling moet:
(1) cellen in een vaste volgorde uitrollen, canarycel eerst; (2) pas doorgaan naar de hoofdcel na een
health-controle van de canarycel; (3) een vaste wachttijd en automatische controles (foutpercentage,
cross-tenant-tests) tussen die twee stappen kunnen afdwingen; en (4) bij een mislukte controle stoppen zonder
de hoofdcel aan te raken. Hoe dat gebeurt, is een uitvoeringskeuze van de tooling en wordt bij de eerste cel
beproefd (open punt).

### 2. Tenantresolutie en identiteit per request

1. **Het hostdomein is de primaire tenantbron.** Het is de enige bron die op alle inbound paden bestaat
   (GraphQL, REST, het anonieme `/api/public/frontend` en de postsale-webhook). Een `WebFilter` zoekt het
   domein op in een allowlist (domein naar OIN) uit de tenantconfiguratie en zet de tenant in de Reactor
   Context. Een onbekend domein geeft 404, nooit een standaardtenant. Het token is een controle, geen
   bron.
2. **Consistentiecheck host en token.** Bij een geauthenticeerd request moet de tenant van de issuer gelijk
   zijn aan de host-tenant, anders 403 plus een securityevent in de log (OIN's, geen tokeninhoud).
3. **De hostheader is alleen betrouwbaar via de ingress.** De ingress overschrijft `Host` en
   `X-Forwarded-Host`; de runtime is alleen via de ingress bereikbaar (NetworkPolicy). De bron in de code is
   `request.uri.host`. De actuator draait op een aparte managementpoort.
4. **Eén domein hoort bij precies één tenant per omgeving.** Een tenant mag meerdere domeinen hebben; een
   dubbel domein weigert het opstarten. Aliassen redirecten bij de ingress naar het canonieke domein.
   *(redactie)* Omdat een cel alleen zijn eigen tenants laadt, ziet het opstarten alleen dubbelen binnen de
   cel; de uniciteit over cellen heen moet de CI-validatie van alle tenantbestanden afdwingen.
5. **Postsale-webhook:** de tenant komt uit het hostdomein van de `webhookUrl`; de tenant van de `keyId` moet
   gelijk zijn, anders weigeren. Een dubbele `keyId` over tenants weigert het opstarten (zelfde
   kanttekening over cellen als hierboven).
6. **Token exchange uit tenantconfiguratie.** `iss` wordt eerst tegen een allowlist gevalideerd;
   token-endpoint, client-id en secret komen uit de tenantconfiguratie, niet uit `jwt.issuer`.
   Audience-validatie staat aan met een unieke `aud` per tenant. Basis: de Spring Security-waarschuwing
   over het vertrouwen van willekeurige issuers en ASVS 5.0 9.2.3, 9.2.4 en 10.3.1.
7. **De private broker is structureel uitgesloten.** De issuer-allowlist komt alleen uit tenantconfiguratie
   en het opstarten faalt als een issuer niet onder de basis-URL van de publieke broker valt. Een
   architectuur- of fitness-test borgt dit. Een egress-NetworkPolicy naar de private broker is
   diepteverdediging.

#### Afweging: issuer als tenantbinding versus een aparte tenant-claim

Dit is de afweging die ticket #205 expliciet toetsbaar wil hebben. Gekozen: **issuer als binding, geen
tenant-claim.** De mapping issuer naar OIN staat in de tenantconfiguratie.

| Criterium | Issuer als binding (gekozen) | Aparte tenant-claim |
|---|---|---|
| Waar zit de binding | In het token zelf (`iss`), een verplicht veld | In een extra claim die per realm of client via een mapper moet worden gezet |
| Vervalsbaar | Alleen met de ondertekeningssleutel van het realm; een realm per tenant heeft een eigen sleutel | Alleen als de mapper correct staat; een verkeerd ingestelde mapper of client geeft een geldig token met de verkeerde claim |
| Extra veld dat verkeerd kan staan | Nee | Ja: een per tenant te onderhouden instelling |
| Vereist unieke `aud` (ASVS 9.2.4) | Niet voor de binding (eigen sleutel per realm); `aud` blijft aan als aparte controle | Ja, zodra tenants dezelfde sleutel delen |
| Past bij realm per tenant (besluit 3) | Ja, volgt vanzelf | Overbodig naast de issuer |
| Past bij één gedeeld realm | Nee: één issuer voor alle tenants | Ja, daar is de claim de enige binding |
| Gebruikerssleutel (ASVS 10.3.3) | `iss` plus `sub` is al per tenant uniek | Bij een gedeeld realm is `sub` alleen niet uniek per tenant |

*Reden voor de keuze:* de issuer zit al in het token, is niet te vervalsen zonder de realmsleutel en vraagt
geen extra veld dat verkeerd ingesteld kan worden. Het besluit hangt af van **realm per tenant**: kiezen we
later toch voor één gedeeld realm, dan is een claim nodig (en een unieke `aud`) en moet deze afweging
opnieuw. Dat is de herzieningsvoorwaarde.

*Toetsbaar:* (a) een geldig token van de realm van tenant A op het domein van tenant B geeft 403 en een
securityevent; (b) een token met een issuer buiten de allowlist wordt geweigerd vóór de handtekening;
(c) een token met een issuer onder de private broker wordt geweigerd, en een configuratie met zo'n issuer
laat de runtime niet opstarten; (d) bij een verloren Reactor Context is het resultaat een fout, geen
standaardtenant. (a), (b) en (d) zijn in het prototype aangetoond *(prototype)*; (b) en (c) tegen de echte
`OauthSecurityAutoConfiguration` en JWT-decoder zijn niet getest.

*Lezing van ASVS 8.3.1.* *(redactie)* Het onderzoek leest 8.3.1 (L1) als "tenant niet alleen uit `Host` of
een door de gebruiker zetbare header, maar uit het gevalideerde token (`iss`/`aud`) controleren tegen het
domein". Ons ontwerp voldoet daaraan door de host als bron te nemen *na* allowlist-validatie en het token
daar verplicht tegen te controleren; een auditor kan dit anders lezen. Leg de lezing vast in het
verificatierapport.

### 3. Keycloak: realm per tenant op de publieke broker

**Besluit:** een realm per tenant op de publieke ID-broker (naam bijvoorbeeld de OIN), gegenereerd uit één
template in Git. Per realm: de DigiD-, eHerkenning- en wallet-koppelingen van de tenant, de clients
`nl-portal` en `nl-portal-m2m`, het thema en een *Frontend URL* op het gemeentedomein. De private broker is
een aparte Keycloak-instantie en staat nooit in een allowlist van de runtime.

| Criterium *(onderzoek)* | Realm per tenant (gekozen) | Eén realm, client en IdP per tenant | Eén realm met Organizations |
|---|---|---|---|
| Sleutels, issuer, JWKS | Per tenant | Eén voor alle | Eén voor alle |
| DigiD-ondertekening (SAML) | Realmsleutel per aansluiting | Eén sleutel voor alle aansluitingen | Als links |
| SSO-sessies en gebruikers | Gescheiden per realm | Gedeeld: login bij A geeft een sessie bij B, tenzij per client dichtgezet | Gedeeld |
| Brute force, wachtwoordbeleid | Per tenant | Gedeeld | Gedeeld |
| Login op gemeentedomein | *Frontend URL* per realm | Eén frontend-URL | Eén frontend-URL |
| Token exchange | Binnen het realm; eigen `nl-portal-m2m` per tenant | Eén `nl-portal-m2m`: elke tenant kan tokens van elke tenant omwisselen | Idem |
| Een fout in configuratie raakt | Eén tenant | Alle tenants | Alle tenants |
| Beheer | 50 realms uit één template | Eén grote export | Eén export plus organizations |

Waarom niet de andere twee *(onderzoek)*: **Organizations** is een B2B-concept binnen één realm dat de
organisatie bepaalt via het e-maildomein; het heeft geen eigen sleutels, sessies of issuer en past niet bij
burgers met DigiD. **Eén realm met clients** deelt sleutel, sessies en gebruikersbestand; scheiding hangt
dan af van een flow-override per client en van claimcontroles, en elke vergeten instelling lekt tussen
tenants.

Schaal *(onderzoek)*: volgens de Keycloak-maintainers werkt 1k+ realms sinds 26.4 mits de realmcache wordt
verhoogd; 50 realms valt daar ruim binnen. Dit is niet in onze eigen inrichting gemeten (geheugen en
opstarttijd). Let op regressies in de Admin API bij upgrades.

**Voorbehoud op het DigiD-argument.** Het doorslaggevende argument uit het onderzoek is dat Keycloak SAML
naar DigiD ondertekent met de sleutel van het realm. Of de **eigen Pink DigiD-plugin** per aansluiting of met
de realmsleutel tekent, is uit publieke bronnen niet te halen en moet uit de plugincode of door het team
worden bevestigd *(niet geverifieerd)*. Tekent hij per aansluiting, dan vervalt alleen dit argument; de
overige blijven staan (sessie-isolatie, brute force, issuer en JWKS per tenant, *Frontend URL*, grens van
de token exchange). Tenants via een makelaar (Signicat) zijn voor Keycloak een gewone OIDC- of SAML-IdP in
het realm van die tenant; bij OIDC speelt het SAML-sleutelargument niet. Het besluit staat dus niet of valt
niet met dit ene punt, maar het is een open feit.

### 4. Configuratiemodel: tenantbestand, flag en secret store

Drie lagen met elk een eigen soort inhoud en een eigen wijzigingsroute. Dit onderscheid staat ook in de
gebruikersdocumentatie (zie [Tenantbestand, flag en secret store](../../hosting-en-configuratie/tenantbestand-flag-en-secret-store.md)).

| Laag | Wat erin staat | Wat er nooit in staat | Wijzigt via |
|---|---|---|---|
| **Tenantbestand** (Git) | Domeinen, issuer, API-URL's, thema, cel, *verwijzingen* naar secrets op naam | Secrets, tokens | Pull request, uitrol door de gitops-tooling, rolling restart van de cel |
| **Flag** (OpenFeature/flagd) | Alleen aan/uit per module, gericht op de tenant (type *permission*) | URL's, credentials, tenantconfiguratie, beveiligingsbesluiten | Flagbestand; live (besluit 5) |
| **Secret store** (via External Secrets Operator) | De secrets zelf: client secrets, API-sleutels, sleutelmateriaal | Niets anders | Rotatie in de store; synchronisatie leidt tot een rolling restart van de cel |

Besluiten:

1. **Eén bestand per tenant per omgeving in Git** (bijvoorbeeld `tenants/<omgeving>/<oin>.yaml`), gevalideerd
   met een JSON-schema in CI. Alleen niet-geheime waarden; geheimen alleen als verwijzing naar een naam.
   **OIN-sleutels in tenantbestanden moeten gequote zijn**: een ongequote OIN wordt door YAML octaal gelezen
   *(prototype)*. Het schema dwingt dit af.
2. **Gedeelde basis per omgeving, het tenantbestand overschrijft.** Credentials en sleutels worden nooit
   geërfd: elke tenant geeft ze zelf op, anders weigert het opstarten.
3. **Modules in twee niveaus.**
   - *Geïnstalleerd* (runtime, bij opstarten): een module bestaat in de runtime als minstens één tenant er
     configuratie voor heeft. Het GraphQL-schema blijft stabiel per release.
   - *Beschikbaar* (per tenant, live): een booleanflag per module (bijvoorbeeld `module.zakenapi`), gericht
     op de tenant, type *permission* (standaard uit, uit bij een fout; ADR 0014). Een nieuwe tenant heeft alle
     modules uit tot de flag aan gaat.
   - Een module is bruikbaar voor een tenant als configuratie aanwezig is **én** de flag aanstaat. Flag aan
     zonder configuratie betekent uit, met een CI-waarschuwing. De frontend-modulelijst volgt dezelfde regel.
   - Uitrollen in twee stappen: eerst de configuratie *dark* via een rolling restart, daarna de flag live aan
     per tenant. Een flag per tenant uitzetten is een kill switch zonder herstart. Een CI-overzicht voegt
     tenantbestand en flagbestand samen tot één modulematrix.
4. **Module-URL en credentials staan niet in een flag.** ADR 0014 punt 6 sluit secrets in een flag-systeem
   uit en een URL is zonder secret niet bruikbaar. Flags zijn alleen aan/uit.
5. **Secrets per tenant via ESO**, eigen pad per tenant (bijvoorbeeld `nl-portal/<omgeving>/<oin>/<module>`)
   en een eigen `ExternalSecret`; het tenantbestand verwijst op naam. Een cel mount alleen de secrets van
   zijn eigen tenants.
6. **Fail fast.** CI valideert elk tenantbestand; bij opstarten weigert de runtime bij een incomplete tenant,
   een dubbel domein of dubbele `keyId`, of een issuer buiten de publieke broker. Fouten vallen zo op in de
   canary.
7. **Het Configuration Panel en de open `/actuator/refresh` en `/actuator/restart` verdwijnen vóór livegang.**

#### Afweging: module aan/uit als configuratie of als flag

| Optie | Voor | Tegen |
|---|---|---|
| Alleen configuratie (aanwezig of niet) | Eenvoudig; een module zonder configuratie kan toch niet aan *(onderzoek)* | Aan/uit per tenant vraagt een rolling restart van de cel; geen kill switch zonder herstart |
| Alleen een flag | Live aan/uit | Een flag kan geen URL of credentials dragen (ADR 0014 punt 6); zonder configuratie is "aan" betekenisloos |
| **Beide (gekozen):** configuratie aanwezig én flag aan | Configuratie dark uitrollen, daarna live aanzetten; kill switch per tenant; GraphQL-schema stabiel | Twee bronnen; CI moet ze samenvoegen en flag-zonder-configuratie signaleren |

Het backend-onderzoek (#146) adviseerde nog "activatie volgt uit tenantconfiguratie, niet uit een flag". Het
configuratiemodel (#202) heeft dat bewust verfijnd naar bovenstaande twee niveaus; dit is dus een
bewuste afwijking van het onderzoeksadvies. Het prototype heeft de flag als stub (`ModulePermission`) en
heeft de echte flagd-koppeling niet getest *(prototype)*.

### 5. Geen hot reload, met één uitzondering: flagd-polling

**Regel:** elke wijziging in tenantconfiguratie of secrets leidt tot een rolling restart, nooit tot hot
reload. Een wijziging in het tenantbestand of een nieuwe waarde in de secret store moet de pod-template laten
veranderen.

*(redactie)* Dit is een eis aan de chart, tool-agnostisch: voor wat Helm zelf rendert (het tenantbestand)
volstaat een checksum-annotatie op de pod-template. Een Helm-checksum ziet alleen wat Helm rendert; een Secret
dat ESO aanmaakt is voor Helm onzichtbaar. Voor ESO-gedreven rotatie is dus iets anders nodig (Stakater
Reloader of een eigen mechanisme dat een annotatie wijzigt). #202 laat de keuze tussen Reloader en eigen
checksum als uitvoeringsdetail; deze kanttekening hoort erbij. Beide werken met elke gitops-tool.

**Uitzondering (besluit):** flagd pollt het flagbestand (standaard elke 5 s, ADR 0014) en past
flagwijzigingen **live** toe. Dat is het doel van OpenFeature en gebeurt binnen deze voorwaarden:

- Flags dragen alleen aan/uit per module, geen tenantconfiguratie en geen beveiligingsbesluiten.
- Een flag-wijziging mag nooit een andere tenant bereikbaar maken of een module aanzetten zonder
  configuratie (besluit 4.3).
- Een permission-flag valt altijd terug op uit (ADR 0014).

Verworpen: de flagd-poll uitzetten en flags ook via een rolling restart laten landen. Dat zou de kill switch
zonder herstart opgeven. Het backend-onderzoek noemde dit expliciet als de keuze die deze ADR moest maken.
`/actuator/refresh` is *geen* onderdeel van de uitzondering: dat is hot reload van `@ConfigurationProperties`
en verdwijnt.

### 6. Realm-as-code via gitops

**Besluit:** keycloak-config-cli (kcc) als gewone Kubernetes Job per tenant-realm (of als Helm-hook), in een
Helm-chart rond één realm-template met per tenant een eigen values-bestand dat uit het tenantbestand komt.
De chart bevat geen tool-specifieke annotaties. De volgorde (eerst de secrets, dan de Job) en het opnieuw
draaien bij wijziging zijn eisen aan de gitops-tooling. Tool-specifieke hooks (bijvoorbeeld een Argo
`PostSync`-hook) horen hooguit als optionele toelichting buiten de chart.

| Optie *(onderzoek)* | Beoordeling |
|---|---|
| **keycloak-config-cli** (gekozen) | Past op het bestaande exportformaat, geen extra control-plane of state-backend, remote state ruimt verwijderde clients, IdP's en flows op |
| Crossplane provider-keycloak | Continue drift-correctie en elk Keycloak-object als Kubernetes-resource; vraagt een Crossplane-control-plane en circa 1.000 tot 2.000 managed resources, en is dus niet tool-agnostisch in de chart. Heroverwegen als kcc Keycloak 26.8+ niet binnen enkele maanden ondersteunt of het platformteam Crossplane toch draait |
| Terraform/OpenTofu | Functioneel het rijkst; vraagt een state-backend en runner. Alleen bij een bestaande Terraform-straat |
| `KeycloakRealmImport` (Operator) | Valt af: alleen aanmaken, niet bijwerken of verwijderen |

Voorwaarden *(onderzoek)*: `import.cache.enabled=false` (anders geen drift-herstel); remote state versleuteld
(`import.remote-state.encryption-key`); een nachtelijke CronJob voor drift met alarm bij falen; de
Keycloak-minor vastzetten op een versie die in de kcc-CI staat en eerst in dev testen; Keycloak admin events
per realm aan; geen master-`admin` (het token draagt de rollen van alle realms, keycloak#26883); een
bootstrap-identiteit met alleen `create-realm` voor onboarding en daarna een service-account per
tenant-realm (federated Kubernetes-JWT vanaf Keycloak 26.6, vanaf 26.8 met de scope `kc-admin-api`).
Offboarding (een realm verwijderen) is een expliciete, handmatige stap.

**Het grootste risico** *(onderzoek)*: kcc loopt achter op Keycloak. De laatste release is 6.5.1 (2026-05-22);
26.7.4 staat alleen op `main` en 26.8 staat nog niet in de CI-matrix. Niet getest: of `manage-realm`,
`manage-clients` en `manage-identity-providers` volstaan voor alle onderdelen van het template, het gedrag
met de Pink-DigiD-plugin, en `private_key_jwt` voor `nl-portal-m2m` met token exchange *(niet geverifieerd)*.
Een kleine proef (twee realms, een met de DigiD-plugin en een met Signicat, plus een Keycloak-upgrade in dev)
is een voorwaarde vóór livegang.

### 7. Frontend

**Besluit** *(onderzoek, optie B plus D)*: nginx `map $host $tenant` met een expliciete allowlist, een
`default_server` met `return 444` voor onbekende hosts, en per tenant vooraf gegenereerde `config.js`,
`manifest.json` en assets uit één tenantbestand in Git. API's zijn same-origin (`/api`, `/graphql`) en
redirect-URI's komen uit `window.location.origin`, zodat de CSP voor alle tenants gelijk is. Het thema blijft
via `/api/public/frontend` lopen; de backend antwoordt dan per tenant. De host-allowlist en het certificaat
per tenant horen op de edge (Gateway API), niet in de frontend-nginx; nginx is het tweede vangnet.

Verworpen: een frontend-deployment per tenant (strijdig met de gedeelde runtime), alles via een
backend-endpoint (extra roundtrip en backend-afhankelijkheid vóór de OIDC-redirect, en titel en manifest
worden vóór JavaScript gelezen). Een nieuwe tenant betekent een rolling restart van de frontend van de cel.

### 8. Core-seam en architectuurchecks

**Besluit** *(onderzoek en prototype)*: de wijziging in de core is één seam, geen negentien modulewijzigingen.
Prototypeomvang: circa 243 regels productiecode (circa 209 in `core.tenancy`, 34 in zakenapi). De seam
bestaat uit:

- een **tenantresolutie-filter** (`WebFilter`) dat host naar OIN afleidt en de tenant met `contextWrite` in
  de **Reactor Context** zet (hetzelfde mechanisme als `ReactiveSecurityContextHolder`), geen `ThreadLocal`;
- een **issuercontrole** na Spring Security;
- een **onveranderlijk per-tenant register** (`Map<OIN, Client>`), bij opstarten gebouwd uit het
  tenantbestand, ingebouwd in de `zgw-client`-seam van ADR 0002. Aanroepers gebruiken `registry.current()`,
  dat de tenantcontext leest en fail-closed is.

Het prototype heeft bewezen *(prototype)*: de juiste tenant per host in suspend- en Mono-resolvers, geneste
resolvers en na een dispatcherwissel; 200 parallelle requests over twee hosts zonder lek; anonieme paden
krijgen de tenant alleen uit de host; onbekend domein geeft 404; een issuer van een andere tenant geeft 403;
fail-closed bij verloren context; en acht architectuurregels die op een overtredende fixture echt vangen. Het
werkt ook in Spring for GraphQL.

Verworpen alternatieven voor het register *(onderzoek)*: één tenantbewuste client per module (mTLS zit in de
`HttpClient` en wisselt niet per request; een fout in het filter lekt credentials naar een andere host);
een child-`ApplicationContext` per tenant (eigen routering, 50 contexts, moeilijk te beoordelen en af te
dwingen); een lazy register met hot reload (strijdig met besluit 5). Verworpen voor het doorgeven van de
tenant: een `CoWebFilter`-coroutinecontext (voor GraphQL niet aangetoond), een expliciete parameter door alle
signaturen (te veel churn) en de tenant alleen op `PortalAuthentication` (werkt niet op anonieme paden).

#### De zes regels voor de implementatie

Deze regels komen uit het prototype en zijn bindend voor de implementatie-epic:

1. **Architectuurregel 1 ("geen `@ConfigurationProperties`-klasse buiten autoconfiguratie") geldt voor de
   geannoteerde klasse óf een geneste klasse ervan, en alleen als ratchet**: baseline 289 overtredingen op
   `main`, het aantal mag alleen dalen. Het is geen harde regel vanaf dag 1. *Properties* (binding) en
   *domeinmodel* (`TenantDefinition`) blijven gescheiden.
2. **OIN-sleutels in tenantbestanden zijn gequote**; het JSON-schema dwingt dit af (zie besluit 4.1).
3. **De hostbron is `request.uri.host`**, niet de Host-header, en alleen achter de ingress.
4. **De issuercheck draait ná Spring Security en controleert elk `JwtAuthenticationToken`, ook bij
   `isAuthenticated=false`** (fail-closed). De eerste prototypeversie liet stil alles door.
5. **De aparte managementpoort is een harde eis.** Een 404 voor onbekende hosts raakt anders ook probes en
   actuator op dezelfde poort.
6. **Alleen `suspend`- of `Mono`-code kan de tenant lezen** (geen synchrone data fetchers);
   `TenantContext.current()` is de enige leesroute. Moduleconfiguratie per tenant wordt per module een eigen
   getypeerde klasse, gevalideerd bij opstarten. Dit is **niet afdwingbaar met architectuurregels**: het
   vraagt code review of een runtime-test. Het gaat om een module die zelf een client vasthoudt in plaats van
   `registry.current()` te gebruiken, en om een vergeten `contextWrite`.

Het prototype bevat daarnaast acht ArchUnit-regels (`TenancyRules.kt`) die op een overtredende fixture echt
vangen: (1) geen `@ConfigurationProperties` buiten autoconfiguratie (de ratchet van regel 1 hierboven),
(2) geen `WebClient.create()` of `.builder()` buiten core en `zgw-client`, (3) geen `ThreadLocal`, (4) geen
`InMemorySecretKeyStore`, (5) geen `OpenFeatureAPI.getInstance()` buiten `core.featureflag`, (6) geen
`GlobalScope`, (7) geen `runBlocking` en (8) geen nieuwe `CoroutineScope(...)` of `MainScope()`. Ze zijn
een bron voor de implementatie-epic; het prototype zelf wordt niet gemerged. Regel (2) verwijst naar het
pakket `nl.nlportal.zgw.client`, dat pas bestaat zodra ADR 0002 is uitgevoerd (zie
[Open punten](#open-punten-en-voorbehouden)). Het backend-onderzoek noemde vijf regels; de eerste vier
zijn in het prototype tot acht uitgesplitst, de vijfde (een fitnesstest die per tenant een volledig register
afdwingt) is een opstart- en runtime-controle en geen ArchUnit-regel.

## Gevolgen

### Voor de backend

Naast de seam, per module *(onderzoek, gebaseerd op `main` op commit `775f21874`)*:

- **Hoog risico (omvang L):** `common-ground-authentication` (één Keycloak-client en één `jwk-set-uri`; het
  token-endpoint wordt nu afgeleid uit `iss`), `payment-direct` (JVM-globale `InMemorySecretKeyStore.INSTANCE`,
  een tweede `keyId` overschrijft stil de eerste, de postsale-webhook heeft geen tenantrouting) en
  `documenten-api` (de keuze van de bron-API gebeurt met een `contains`-match op de URL; vervangen door een
  prefixmatch binnen de tenant).
- **Hoog risico (omvang M):** `objectenapi` (`getObjectByUrl` stuurt de token naar elke binnenkomende URL;
  beperken tot het hostdomein van de tenant) en `haalcentraal2` (`allowedCitiesForBewoning` en `brpFields`
  zijn per gemeente doelgebonden; een gedeelde waarde kan een tenant meer velden geven dan zijn grondslag
  toestaat).
- **Overig:** de overige modules krijgen een registeropzoeking in plaats van een geïnjecteerde client. De
  core-modules `featureflag` en `frontend` halen de tenant uit de `TenantContext`; `/api/public/frontend` is
  anoniem en neemt de tenant dus alleen uit het hostdomein. `@ConditionalOnProperty` betekent voortaan
  "geïnstalleerd in de runtime" (de unie over tenants).
- **Anonieme en server-to-server paden:** `/api/public/frontend`, de postsale-webhook en `/graphql` (op
  filterniveau `permitAll`) krijgen de tenant uit de host, vóór de resolvers.
- **Gebruikerssleutel:** met meerdere realms is `sub` alleen niet uniek; de gebruikerssleutel is (`iss`, `sub`) of
  (OIN, BSN/KVK) (ASVS 10.3.3).
- Er zijn in productiecode geen `ThreadLocal`, `GlobalScope`, `runBlocking`, `@Scheduled`, `@Cacheable` of
  `ConcurrentHashMap` gevonden *(onderzoek)*; er zijn dus nu geen caches die tenantdata kunnen mengen. Een
  toekomstige cache moet de tenant in de sleutel hebben (ASVS 14.2.2).
- Geheugen- en poolgebruik van circa 600 `WebClient`-instanties (50 tenants × ongeveer 12 API's) is niet
  gemeten *(niet geverifieerd)*.

### Voor de frontend

Zie besluit 7: same-origin API's, afgeleide redirect-URI's, per tenant gegenereerde bestanden, nginx-allowlist,
exacte redirect-URI's per tenant in Keycloak (geen wildcards, ASVS 10.4.1). De frontend gebruikt
`oidc-client-ts`/`react-oidc-context`, geen keycloak-js *(onderzoek)*. Branding vóór login (foutpagina,
landingspagina) komt uit de nginx-bestanden, niet uit `/api/public/frontend` (dat laadt na inloggen).

Vrije thema-CSS is een vertrouwensgrens (CSS kan via attribuutselectors data lekken): alleen door Pink
beheerde en gereviewde thema's, geen self-service zonder review *(onderzoek)*.

### Voor beheer en bewijs

- Elke release raakt alle tenants van een cel; BIO 8.32.01 vraagt een risicoafweging en een rollback-plan per
  wijziging, goedkeuring door een tweede persoon, en Git met de gitops-uitrol is de administratie.
- Een secret-rotatie of een tenantwijziging herstart alleen de eigen cel (het backend-onderzoek ging nog uit
  van een vlootbrede herstart; de celindeling beperkt dat).
- Pink levert per tenant een overzicht van sleutels en certificaten (BIO 8.24.02) en één assurancerapport
  voor de gedeelde runtime; een audit door één gemeente mag geen gegevens van een andere ontsluiten (5.20.04).

## BIO 2.x en ASVS 5.0 L3

Beknopt; het volledige overzicht met alle controls staat in het onderzoek `multitenancy-bio-asvs-tenantscheiding.md`
(branch `research/multitenancy-bio-asvs`).

| Eis *(onderzoek)* | Hoe dit voorstel het afdekt | Bewijs ("werking") |
|---|---|---|
| BIO 8.03.01, ASVS 8.4.1 (L2): logisch isoleren, cross-tenant controls | Tenant verplicht in elke uitgaande client via het register; host/issuer-check | Architectuurchecks, cross-tenant-tests per release |
| BIO 8.22.01, 5.12.01: gescheiden groep heeft een vastgelegd niveau | Cel als beveiligingsgroep | Vastgelegd niveau per cel |
| ASVS 11.1.1 (L2): sleutels niet overdelen | Eigen secrets, clients en realmsleutels per tenant | ESO-paden per tenant, realm per tenant |
| ASVS 13.3.1 (L2, L3 extra), 13.3.3 (L3): HSM-backed store, crypto in securitymodule | Open; zie voorstel hieronder | Te bevestigen door devops |
| ASVS 13.3.2 (L2): least privilege op secrets | Cel mount alleen eigen secrets; restrisico gedocumenteerd | Cel- en ESO-inrichting |
| ASVS 9.2.3, 9.2.4, 10.3.1 (L2): `iss`/`aud` per tenant | Issuer-allowlist, unieke `aud`, issuer als binding | Tests (a) tot (d) bij besluit 2 |
| ASVS 8.3.1 (L1), 4.1.3 (L2): tenant niet uit door gebruiker zetbare bron | Host na allowlist, ingress overschrijft headers, NetworkPolicy | Zie lezing bij besluit 2 |
| BIO 8.15.01, ASVS 16.2.1 (L2): tenant in elke logregel | OIN in MDC/Reactor Context | Nog uit te werken (open punt) |
| ASVS 16.3.2 (L2, L3 extra): alle autorisatiebeslissingen loggen | Niet besloten | Open punt |
| BIO 8.32.01: wijzigingsbeheer met rollback-plan | Git, PR-review, cel-voor-cel gitops-uitrol | PR-historie, uitrolhistorie van de gitops-tool |
| BIO 8.08.05, 8.31, 8.29.01: pentest per release, geen test in productie | Voorstel: acceptatie vóór de canarycel | Voorstel, ter toetsing |
| ASVS 13.4.5, 13.4.2 (L2): geen open beheer-endpoints | Actuator op managementpoort, `refresh` en `restart` weg | Vóór livegang |

**Pentest per release (voorstel, ter toetsing door security en compliance).** Pentest en DAST draaien op
acceptatie, vóór de canarycel; hoge risico's blokkeren daar. De canary telt daarna als eerste productie-uitrol
na acceptatie (BIO 8.31), niet als test in productie. Of dit aan BIO 8.08.05 voldoet, is een
uitleg die security en compliance moeten bevestigen.

**HSM-backed secret store (open, voorstel nog niet beoordeeld).** ASVS 13.3.1 eist bij L3 een
hardware-backed secret store en 13.3.3 crypto in een geïsoleerde securitymodule. Of de hosting dat biedt,
moet devops bevestigen. Voorstel: een HSM-backed store als eis vóór livegang; voor 13.3.3 (in-process HMAC
voor het pseudonieme gebruikers-id, `private_key_jwt`) een gedocumenteerde afwijking met een plan na
livegang.

## Helm-, Kubernetes- en ESO-inrichting

Doelbeeld; niets hiervan is ingericht.

- **Helm en gitops (tool-agnostisch).** Een cel is een Helm-release met eigen values (cel, replica's,
  tenantlijst); de realm-Jobs zijn gewone Kubernetes Jobs (of Helm-hooks) zonder tool-annotaties. Eisen aan
  de gitops-tooling: uitrol in volgorde canarycel naar hoofdcel met wachttijd en health-controle (besluit
  1.4), eerst de `ExternalSecret`'s en dan de Jobs, en een nieuwe run van de Jobs bij wijziging.
- **Context: ArgoCD** *(redactie, uit de Argo CD-documentatie bij het schrijven van deze ADR gelezen)*. Argo
  is de beoogde tool; de chart blijft generiek en krijgt geen Argo-configuratie. Relevant uit de docs:
  ApplicationSet-*progressive syncs* (`RollingSync`) is beta sinds Argo CD v3.3.0 ("generally considered
  stable, but there may be unhandled edge cases"), zet autosync uit voor de gegenereerde Applications, laat
  voortgang alleen van de health afhangen en kent geen vaste wachttijd; hooks draaien niet bij een
  selectieve sync. De wachttijd in de canary moet dus uit een health- of hook-controle komen. Dit hoort in
  de tooling-inrichting (buiten de chart) en bij de verificatiestap onder
  [Vóór livegang](#vóór-livegang-en-na-livegang).
- **Kubernetes.** Per cel: een deployment van backend en frontend (zelfde image en chart), minimaal 2
  replica's, PodDisruptionBudget, anti-affinity. Een `NetworkPolicy` laat de runtime alleen via de ingress
  bereiken en blokkeert egress naar de private broker. De actuator draait op een aparte managementpoort
  (bijvoorbeeld `:8090`), niet via de ingress.
- **Gateway API.** Een `HTTPRoute` per domein routeert naar de cel; de host-allowlist en het certificaat per
  tenant horen op de edge. De `HTTPRoute`'s worden gegenereerd uit de tenantbestanden, zodat er één bron
  van waarheid is voor domeinen *(redactie)*.
- **ESO.** Per tenant een eigen pad en een eigen `ExternalSecret`; een cel mount alleen de secrets van zijn
  tenants. De achterliggende store moet HSM-backed zijn (zie hierboven). Secrets staan niet in Git,
  ConfigMaps of een flag-systeem. De kcc-remote-state krijgt een encryptiesleutel via ESO.

## Vóór livegang en na livegang

Dit is het overgangspad voor de eerste tenants; het scherpt aan zodra de implementatie-epic is ingedeeld.

**Vóór livegang (van de eerste tenants):**

1. De core-seam, het register en de issuercontrole, met de hoog-risico-modulewijzigingen (zie Gevolgen) en
   de zes regels met architectuurchecks, inclusief de ratchet.
2. Het tenantschema met CI-validatie over alle tenantbestanden (domeinen en `keyId` globaal uniek, gequote
   OIN's, flag-zonder-configuratie), fail-fast bij opstarten.
3. Het Configuration Panel en de open `/actuator/refresh` en `/actuator/restart` weg; de managementpoort.
4. Realm per tenant via kcc met bootstrap-identiteit en service-account per realm, drift-CronJob, versleutelde
   remote state, Keycloak-minor vastgezet; de kleine proef met de DigiD-plugin en Signicat.
5. ESO-paden per tenant, `NetworkPolicy`'s (ingress-only, egress naar private broker geblokkeerd),
   Gateway API-routes per domein.
6. De cellen (canary en hoofdcel) als Helm-releases, HA, en gitops-uitrol in volgorde met wachttijd en
   automatische controles; frontend-nginx met allowlist en `default_server` 444.
7. **Verificatiestap ArgoCD:** controleer dat de generieke charts niet botsen met Argo-best practices:
   geen Helm-hooks die Argo anders behandelt dan Helm, geen `lookup` of random-waarden in templates die
   diff-ruis geven, en checksum-annotaties voor de rolling restart werken ook onder Argo. Dit wijzigt de
   chart niet naar Argo toe; het is een toets op de generieke chart.
8. OIN in elke logregel; cross-tenant-tests in de pipeline met een rapport.
9. Pentest en DAST op acceptatie met cross-tenant-scenario's (voorstel).
10. De HSM-backed secret store, of een gedocumenteerde, geaccepteerde afwijking (te bevestigen door devops).
11. Antwoorden op de open feiten die het ontwerp raken: Logius (mTLS), de Pink-plugin (sleutel per
    aansluiting), PSP-`keyId`, Cyberbeveiligingswet.

**Na livegang:**

1. Het volledige observability- en auditmodel per tenant (alle autorisatiebeslissingen loggen, loginzage per
   tenant afschermen, bewaartermijnen).
2. Rate limiting, fair use, connectiepools en circuit breakers per tenant en bron.
3. Het plan voor ASVS 13.3.3 (crypto in vault of HSM) en sterkere clientauthenticatie voor `nl-portal-m2m`
   (`private_key_jwt`; ASVS 10.4.16).
4. Een tenant-onboardingproces van OIN tot draaiend portaal, en een beheer-UI (apart onderzoek).
5. Heroverweging van Crossplane voor realm-as-code, extra cellen per SLA of risicobereidheid, het aantal
   tenants per cel op basis van belastingtests.
6. De losse ASVS-bevindingen buiten tenancy, apart te beleggen: CSP mist `object-src`, `base-uri`,
   `frame-ancestors` en `report-to`; `localhost:*` staat in `connect-src`; twee ongeldige
   `Access-Control-Allow-Origin`-headers.
7. Een gezamenlijke audit en één assurancerapport voor de gedeelde runtime (BIO 5.20.03, 5.20.04).

*(redactie)* De indeling "vóór" en "na" is een voorstel van deze ADR; de kaart bevat het overgangspad
nog als onbepaald punt.

## Open punten en voorbehouden

Open feiten, te bevestigen bij de genoemde partij:

- **HSM-backed secret store** (ASVS 13.3.1 en 13.3.3): devops bevestigt of de hosting dit biedt.
- **Pentest per release versus canary-releases**: voorstel (acceptatie vóór canary) ter toetsing door
  security en compliance.
- **Pink DigiD-plugin**: tekent hij SAML per aansluiting of met de realmsleutel? Uit de plugincode of door
  het team *(niet geverifieerd)*.
- **Logius (mTLS-clientcertificaat)**: het back-channel-certificaat (`client-keystore`) is serverbreed in
  Keycloak. Mag dat bij meerdere DigiD-aansluitingen (de overeenkomst meervoudige aansluiting, LMA)? Op wiens
  naam moeten de certificaten staan? Alternatieven: een SAML-proxy per tenant of een makelaar *(niet
  geverifieerd)*.
- **Cyberbeveiligingswet**: of Pink zelf eronder valt (bijvoorbeeld als aanbieder van beheerde of
  clouddiensten) is niet onderzocht *(niet geverifieerd)*; het bepaalt Pinks eigen zorgplicht en meldplicht.
- **PSP `keyId`**: kunnen PSP-profielen van verschillende gemeenten dezelfde `keyId` hebben (één
  Pink-merchant-account versus één per gemeente)? Navragen. Of de `com.onlinepayments`-SDK een eigen
  `SecretKeyStore` naast `InMemorySecretKeyStore` toestaat, is afgeleid uit de constructor, niet uit de
  SDK-docs *(niet geverifieerd)*.
- **De BIO2-PDF-markering** (welke overheidsmaatregelen buiten de Cbw vallen) en ISO/IEC 27002 en 27017 zijn
  niet gelezen *(niet geverifieerd)*; welke gemeenten essentiële entiteit zijn is niet uitgezocht.

Voorbehouden uit het prototype, in de implementatie-epic op te pakken *(niet getest)*: de issuercheck tegen de
echte `OauthSecurityAutoConfiguration` en JWT-decoder, de filtervolgorde in de volledige app, token exchange,
payment-direct postsale, `documenten-api`, MDC en observability, ingress/NetworkPolicy/ESO, de prestaties
van circa 600 clients, en mutatietests (de CI-gate van 80% op de echte implementatie).

Overige openstaande punten:

- **Locatie van de tenantbestanden** *(redactie)*: in deze open-source repository of in een aparte
  config-repository? Tenantbestanden bevatten OIN's, domeinen en API-URL's van gemeenten; ADR 0016 spreekt
  van een "config-repository". Dit is niet besloten.
- **Gitops-uitrol in volgorde**: met welk mechanisme van de tooling de volgorde en de vaste wachttijd in de
  canary worden afgedwongen (eis aan de tooling, niet aan de chart).
- **Het aantal tenants per cel**, de grootte van de cellen en de belastingtests.
- **Rate limiting en fair use per tenant** (noisy neighbour), **observability en audit per tenant** en het
  **tenant-onboardingproces**: nog niet gespecificeerd op de kaart.
- **Afhankelijkheid van ADR 0002**: de seam landt in `zgw-client`, dat nog niet is gebouwd; de
  `WebClient`-regel verwijst ernaar. Zolang ADR 0002 niet is uitgevoerd, moet het register op een tijdelijke
  plek landen of eerst ADR 0002.
- **Wijzigingen in ADR 0016**: bij acceptatie van deze ADR worden de punten 2 en 6 en het alternatief
  "Tenant-resolutie per request" aangepast.

## Buiten scope

Facturatie en metering per tenant; migratie van productiedata (er is nog geen PRD; herindeling is een nieuwe
tenant); een beheer-UI voor tenantconfiguratie of flags; TLS-certificaten voor gemeentedomeinen (bestaand
proces); de implementatie zelf (die loopt via een epic).

## Bronnen

Onderzoek (branches, nooit gemerged; bestanden onder `site/development/research/`):

- `research/multitenancy-bio-asvs`: `multitenancy-bio-asvs-tenantscheiding.md` (#198).
- `research/multitenancy-backend-impact`: `multitenancy-backend-impact.md` (#146).
- `research/multitenancy-keycloak`: `multitenancy-keycloak-realms.md` (#199).
- `research/multitenancy-frontend`: `multitenancy-frontend-impact.md` (#200).
- `research/multitenancy-realm-as-code`: `multitenancy-realm-as-code.md` (#208).

Prototype (bron, niet overnemen): branch `prototype/tenantcontext-seam`, commit `1f15569b9` (#204).

Besluiten op de kaart: #201 (tenantresolutie), #202 (configuratiemodel), #203 (runtime-indeling), #204
(prototype), de ticketcomments op #205. De Argo CD-documentatie over *Progressive Syncs* is bij het schrijven
van deze ADR gelezen, alleen voor de alinea "Context: ArgoCD".
