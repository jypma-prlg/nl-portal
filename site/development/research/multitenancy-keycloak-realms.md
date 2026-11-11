# Keycloak bij ~50 tenants: realm per tenant, client per tenant of Organizations

Geraadpleegd op: 2026-11-06. Onderzoek voor #199 (kaart #197), gebruikt door #201.

Peilversie: Keycloak **26.8.2** (laatste release, 2026-11-05). De demo-omgeving draait `quay.io/keycloak/keycloak:26.3` (`docker-compose/docker-compose.yaml`).

## Samenvatting

- **Advies: realm per tenant op de publieke ID-broker** (optie 1). Eén realm per tenant (naam bijvoorbeeld de OIN), elk met de eigen DigiD-, eHerkenning- en walletkoppelingen, de clients `nl-portal` en `nl-portal-m2m`, een eigen thema en een eigen *Frontend URL* op het gemeentedomein. De realms komen uit één template in Git.
- **Doorslaggevend is DigiD.** Keycloak ondertekent SAML-berichten naar een externe IdP met de **sleutel van het realm**. Een per-IdP-sleutel bestaat niet. DigiD verwacht in de metadata van elke aansluiting het eigen PKIoverheid-certificaat van die aansluiting. Met één realm (optie 2 of 3) delen alle gemeenten dus één ondertekeningssleutel. Met een realm per tenant heeft elke aansluiting een eigen sleutel.
- **Isolatie zit in Keycloak op realmniveau.** Sleutels, SSO-sessies, gebruikers, brute-force-instellingen, de token-exchange-grens en de issuer zijn allemaal per realm. In één realm deelt een burger één SSO-sessie over alle gemeenteportalen. Is hij via de DigiD-aansluiting van gemeente A ingelogd, dan komt hij zonder nieuwe DigiD-login binnen bij gemeente B. Dat moet je per client met flow-overrides dichtzetten.
- **50 realms is geen schaalprobleem meer.** De Keycloak-maintainers schrijven dat 1k+ realms werkt sinds 26.4, mits je de realmcache ophoogt. Recente regressies speelden pas bij honderden realms, voor tokens van master-admins.
- **Organizations is niet bedoeld voor dit soort tenants.** Het is een B2B-concept *binnen* één realm. Het bepaalt de organisatie van een gebruiker via diens **e-maildomein** (identity-first login) of uitnodiging, en zet het lidmaatschap in een `organization`-claim. Het vervangt geen isolatie op realmniveau. Een burger met DigiD heeft geen organisatiedomein, en de tenant volgt uit het portaal, niet uit de gebruiker.
- **Gevolg voor de backend:** per tenant een eigen issuer en JWKS. Valideer `iss` tegen een **expliciete allowlist** (Spring `JwtIssuerReactiveAuthenticationManagerResolver`) en koppel de issuer aan de tenant van het request. Bouw de token-exchange-URL uit die allowlist, niet uit `jwt.issuer`, zoals nu in `CommonGroundAuthenticationConverter.kt:83` gebeurt.
- **Private ID-broker:** een aparte instantie met een eigen hostnaam en eigen sleutels. De portal-runtime kent die issuer niet, zodat de allowlist hem afwijst. Een architectuur- of configuratietest borgt dat elke vertrouwde issuer onder de publieke broker valt.

## 1. Bronnen

| Bron | Link | Opmerking |
|---|---|---|
| Keycloak Server Admin guide: realms | https://www.keycloak.org/docs/latest/server_admin/index.html | Versie 26.8.2, inleiding over realms gelezen |
| Organizations-hoofdstuk (bron van de docs) | https://github.com/keycloak/keycloak/tree/main/docs/documentation/server_admin/topics/organizations | `intro`, `managing-identity-providers`, `authenticating-members` en `mapping-organization-claims` volledig gelezen |
| Release notes 26.8.0 | https://github.com/keycloak/keycloak/blob/main/docs/documentation/release_notes/topics/26_8_0.adoc | Organizations: gedeelde IdP's, domain routing |
| SAML identity broker | https://github.com/keycloak/keycloak/blob/main/docs/documentation/server_admin/topics/identity-broker/saml.adoc | Ondertekening met de sleutel van het realm |
| Outgoing HTTP | https://github.com/keycloak/keycloak/blob/main/docs/guides/server/outgoinghttp.adoc | `client-keystore` voor mTLS is serverbreed |
| Hostname v2 | https://github.com/keycloak/keycloak/blob/main/docs/guides/server/hostname.adoc | Frontend URL per realm |
| Token exchange | https://www.keycloak.org/securing-apps/token-exchange (bron: `docs/guides/securing-apps/token-exchange.adoc`) | Standard token exchange alleen binnen één realm |
| Client basic settings | `docs/documentation/server_admin/topics/clients/oidc/con-basic-settings.adoc` | *Login theme* per client |
| Operator realm import | `docs/guides/operator/realm-import.adoc` | Alleen aanmaken, niet bijwerken |
| Discussie "Improved scalability over number of realms" | https://github.com/keycloak/keycloak/discussions/11074 | Update van 2025-10-05 door de maintainers |
| Issues over veel realms | keycloak/keycloak #26883 (open), #46605, #51554, #52009 (gesloten) | Regressies bij honderden realms |
| Logius, Koppelvlakspecificatie DigiD SAML Authenticatie v3.7 | https://www.logius.nl/domeinen/toegang/digid/documentatie/koppelvlakspecificatie-digid-saml-authenticatie | Gelezen via een samenvattende fetch-tool |
| Logius, Handleiding aansluiten op DigiD; overeenkomst meervoudige aansluiting (LMA) | logius.nl (pdf) | Alleen titels en zoekfragmenten gezien, tekst niet leesbaar |
| Spring Security, Resource Server multitenancy (reactive) | https://docs.spring.io/spring-security/reference/reactive/oauth2/resource-server/multitenancy.html | `JwtIssuerReactiveAuthenticationManagerResolver` |

## 2. Hoe Organizations zich verhoudt tot realms

**Een realm** is de eenheid van isolatie in Keycloak. De admin guide zegt: "Realms are isolated from one another and can only manage and authenticate the users that they control." Per realm staan onder meer: gebruikers, clients, identity providers, sleutels (*Realm settings → Keys*), SSO-sessies, authenticatieflows, thema's, token-instellingen, brute-force-detectie en de issuer `…/realms/<realm>`.

**Een organization** leeft *binnen* één realm. De docs omschrijven het als een manier om externe partijen (klanten, zakenpartners) binnen een realm te vertegenwoordigen. Organizations is het startpunt voor B2B-use-cases en "enables multi-tenancy within a realm". De gebruiker krijgt dan toegang tot resources van het realm "in a more restricted and controlled context". Wat een organization biedt (bron: `intro.adoc`):

- leden beheren, ook in groepen;
- leden onboarden via een uitnodiging of via identity brokering met een **gekoppelde IdP** (sinds 26.8 kan één IdP aan meerdere organizations hangen);
- **identity-first login**: de gebruiker vult eerst gebruikersnaam of e-mail in, en Keycloak zoekt de organisatie op **e-maildomein** en kan dan automatisch doorsturen naar de IdP van die organisatie;
- de claim `organization` in tokens, via scope `organization`, `organization:<alias>` of `organization:*`, zodat een resource server kan autoriseren op lidmaatschap.

Wat een organization **niet** heeft: eigen sleutels, een eigen issuer, eigen SSO-sessies, een eigen brute-force-configuratie, eigen clients of een eigen thema. In de organizations-docs is niets daarvan te vinden: het blijft allemaal realm-breed. De IdP zelf maak je ook op realmniveau aan en koppel je daarna aan de organization.

De maintainers beschrijven het onderscheid zelf in discussie #11074. In 2023 dachten ze na over organizations als alternatief voor duizenden realms. Een reactie daarop onderscheidt twee gevallen: een SaaS-aanbieder met B2B(2C)-klanten, waar organizations past, en het hosten van Keycloak voor *ongerelateerde* bedrijven, waar aparte realms nodig zijn.

**Toegepast op NL Portal:** een gemeente is geen zakenpartner wiens medewerkers inloggen. Ze is een aparte dienstaanbieder met een eigen DigiD-aansluiting, eigen certificaat, eigen domein en eigen huisstijl. Haar burgers zijn géén leden met een organisatiedomein. De tenant volgt uit het portaal (de client), niet uit het e-mailadres van de gebruiker. Daarmee valt NL Portal in het tweede geval.

## 3. Vergelijking

| Criterium | (1) Realm per tenant | (2) Eén realm, client + IdP per tenant | (3) Eén realm met Organizations |
|---|---|---|---|
| **Sleutels / issuer** | Eigen sleutelset, issuer en JWKS per tenant | Eén sleutelset en één issuer voor alle tenants | Als (2) |
| **DigiD-ondertekening (SAML)** | Eigen realmsleutel per aansluiting, dus eigen certificaat in de metadata | Alle aansluitingen ondertekenen met dezelfde realmsleutel | Als (2) |
| **SSO-sessies** | Gescheiden per realm | Gedeeld: inloggen bij A geeft een sessie die ook voor B geldt (cookie-authenticator) | Als (2) |
| **Gebruikers** | Gescheiden; een BSN staat alleen in het realm van die tenant | Alle burgers van alle gemeenten in één gebruikersbestand; IdP-links per gebruiker | Als (2), plus lidmaatschappen die zich over gemeenten opstapelen |
| **Brute force, wachtwoordbeleid** | Per tenant | Gedeeld | Gedeeld |
| **Login-pagina met de juiste IdP's** | Alleen de IdP's van de tenant staan in het realm | Op de standaard loginpagina staan alle IdP's van het realm; per client een flow-override met *Identity Provider Redirector* nodig | Identity-first vraagt om e-mail, wat niet past bij burgers; anders een eigen flow nodig |
| **Thema per tenant** | Realm *Login theme* | Client *Login theme* (bestaat per client) | Client *Login theme*; geen thema per organization gevonden |
| **Login op het gemeentedomein** | *Frontend URL* per realm | Eén frontend-URL voor alle tenants | Als (2) |
| **Token exchange (`nl-portal-m2m`)** | Per realm een `nl-portal-m2m`; exchange blijft binnen het realm | Eén `nl-portal-m2m`, maar dan kan elke tenant tokens van elke tenant omwisselen; per tenant een m2m-client is beter | Als (2) |
| **Schaal bij 50** | Prima (zie §5) | Prima | Prima |
| **Beheer als code** | 50 realm-exports uit één template | Eén grote realm-export | Eén realm-export plus organizations |
| **Fout in de configuratie raakt** | Eén tenant | Alle tenants | Alle tenants |

## 4. Toelichting per onderwerp

### 4.1 DigiD en eHerkenning

- DigiD gebruikt SAML met de **HTTP-Artifact binding**. De artifact resolution loopt via een back-channel met **mutual TLS**, met een PKIoverheid machine-to-machine-certificaat. Alle berichten van de dienstaanbieder moeten ondertekend zijn, en DigiD gebruikt "alleen het certificaat in de metadata van de dienstaanbieder" (Koppelvlakspecificatie v3.7). De `Issuer` moet gelijk zijn aan de EntityID in de metadata van de aansluiting.
- Keycloak: "When *ON*, {project_name} uses the realm's keypair to sign requests sent to the external SAML IDP" (`identity-broker/saml.adoc`). De SP EntityID is standaard de URL van het realm, maar is per IdP in te stellen. Een **ondertekeningssleutel per IdP is er niet**.
- Gevolg: met één realm ondertekenen alle 50 aansluitingen met dezelfde sleutel. Daarvoor zou één certificaat in de metadata van 50 verschillende dienstaanbieders moeten staan. Met een realm per tenant laad je het PKIoverheid-certificaat van de tenant als sleutel in zijn eigen realm.
- **Open punt, niet geverifieerd: de mTLS-clientcertificaat voor het back-channel.** De optie `client-keystore` voor uitgaande mTLS is **serverbreed** (`outgoinghttp.adoc`). Ook met een realm per tenant gebruikt Keycloak bij de artifact resolution dus één clientcertificaat voor alle aansluitingen. Navragen bij Logius of dat mag binnen een *meervoudige aansluiting* (Logius heeft daar een overeenkomst voor, de LMA). Ook navragen op wiens naam de certificaten moeten staan. Het alternatief is een SAML-proxy per tenant tussen Keycloak en DigiD, of een DigiD-makelaar. Dit geldt voor alle drie de opties en is dus geen argument tegen realm per tenant.
- eHerkenning loopt via een makelaar. Die bundelt doorgaans meerdere dienstverleners en diensten. Niet verder onderzocht.

### 4.2 Sessies en cross-tenant SSO

De standaard browserflow begint met de *Cookie*-authenticator. Wie al een SSO-sessie in het realm heeft, krijgt dus direct een code voor elke client in dat realm. In opties 2 en 3 betekent dat: na een DigiD-login via de aansluiting van gemeente A krijgt de burger ook een sessie bij gemeente B, zonder dat de aansluiting van B is gebruikt. Dichtzetten kan, met een flow-override per client zonder cookie-stap of met `prompt=login`. Maar dan beheer je 50 flows, en elke vergeten override is een lek tussen tenants. In optie 1 bestaat het probleem niet, omdat sessies en cookies (pad `/realms/<realm>`) per realm zijn.

### 4.3 Token exchange

De standard token exchange (V2) wisselt een token uit "for a new token issued to the client that triggered the token exchange request. Both clients must be in the same realm" (`token-exchange.adoc`). De bestaande inrichting uit [Standard token exchange](../../hosting-en-configuratie/keycloak-token-exchange.md) blijft per realm dus gelijk: `nl-portal` + `nl-portal-m2m`, de `aanvrager`-scope en de audience mappers. Alleen vermenigvuldigd met 50.

- **50 m2m-secrets** in ESO is beheerbaar, maar ASVS 10.4.16 (L3) vraagt om sterke clientauthenticatie. Overweeg `private_key_jwt` (*Signed JWT*) met één backend-sleutelpaar. Elk realm vertrouwt dan dezelfde publieke sleutel of JWKS-URL van de backend, en er hoeft geen gedeeld geheim per realm te bestaan. Niet uitgeprobeerd.
- In opties 2 en 3 kan één gedeelde m2m-client tokens van alle tenants omwisselen. Dat is geen scheiding.

### 4.4 Issuer en JWKS per tenant in de backend

Nu staat in de backend één `jwk-set-uri` (`backend/app/src/main/resources/config/application.yml:28`), zonder `iss`-validatie. De token-exchange-URL wordt opgebouwd uit `jwt.issuer` (`CommonGroundAuthenticationConverter.kt:83`). Bij een realm per tenant:

- Gebruik `JwtIssuerReactiveAuthenticationManagerResolver.fromTrustedIssuers(...)` met de issuers uit de tenantconfiguratie. Spring waarschuwt: "It would be unsafe to simply take any issuer and construct an `ReactiveAuthenticationManager` from it." Gebruik dus een allowlist. De resolver laadt de issuers lazy.
- Controleer daarnaast dat de issuer van het token **hoort bij de tenant van het request** (host → OIN → issuer). Anders accepteert portaal B een geldig token van realm A.
- Haal de token-endpoint-URL uit dezelfde allowlist en niet uit het token. Dat sluit ook het punt uit [asvs-5-l3-token-exchange.md](./asvs-5-l3-token-exchange.md) §2 af, waar het client secret naar `jwt.issuer` gaat.
- In opties 2 en 3 is er maar één issuer. De tenantscheiding hangt dan helemaal af van een claim (`aud`/`azp` of `organization`) die de backend per request moet controleren.

### 4.5 Thema per tenant

Een realm heeft een *Login theme*, en een client heeft ook een eigen *Login theme* (`con-basic-settings.adoc`). Huisstijl per tenant kan dus in alle opties. In de organizations-docs is geen thema per organization te vinden. Of één geparametriseerd thema volstaat (bijvoorbeeld via realm-attributen) of dat er per tenant een themamap nodig is, is niet onderzocht.

### 4.6 Login op een gemeentedomein

Hostname v2 kent naast de serverbrede `hostname` ook een **"Realm configuration for frontend URL"** (`hostname.adoc`, bronnen voor de URL). Per realm kun je dus de login laten draaien op bijvoorbeeld `login.<gemeentedomein>`. Daardoor horen cookies, de EntityID en de DigiD-endpoints bij het domein van de dienstaanbieder. Met één realm kan dat niet. Of DigiD een eigen domein per aansluiting eist, staat niet in de gelezen bronnen (zie §4.1).

### 4.7 Schaal en performance bij 50+ realms

- Discussie #11074: in 2022 schaalde Keycloak slecht voorbij 100-200 realms. Update van de maintainers op 2025-10-05: "With Keycloak 26.4, it should be fine to run Keycloak with 1k+ realms as long as you keep increasing the realm cache." Het gaat om de cache-opties `KC_CACHE_EMBEDDED_REALMS_MAX_COUNT` e.d.
- Regressies die vooral bij honderden realms speelden, allemaal gesloten: #46605 (opstarten 26.5.4, 732 realms, O(N²)), #51554 (Admin API in 26.7.1, gemeten 40 ms om 6 ms bij 50 realms en 11 s bij 300), #52009. Nog open: #26883. Een token van een master-admin bevat de rollen van álle realms, wat slecht schaalt.
- **Conclusie:** 50 realms valt ruim binnen het ondersteunde bereik. Let bij upgrades op regressies in de Admin API. Gebruik voor automatisering een **service-account per realm** of een beperkte admin, geen master-`admin`-token (#26883). Dat is ook beter voor least privilege.

### 4.8 Beheer en automatisering (realm as code)

- De Keycloak Operator `KeycloakRealmImport` "only supports creation of new realms and does not update or delete those" (`realm-import.adoc`). Voor continu bijwerken vanuit Git (ArgoCD) volstaat dat niet. Je hebt dan een tool nodig die declaratief bijwerkt, bijvoorbeeld keycloak-config-cli of Terraform; niet onderzocht.
- Werk met één template plus tenantparameters (OIN, domein, DigiD-metadata, sleutel uit ESO, thema). Daarmee leveren 50 realms geen 50 handmatige configuraties op. Onboarding van een tenant (zie de kaart, *Not yet specified*) wordt dan "één realm genereren".
- De realm-import in de demo (`docker-compose/imports/keycloak/nlportal-realm.json`, realm `nlportal`) is een bruikbaar startpunt voor dat template. Let op: `bruteForceProtected` staat daar op `false`.

### 4.9 Private ID-broker nooit accepteren

- Draai de private broker als **aparte Keycloak-instantie** (eigen database, hostnaam en sleutels), niet als realm in de publieke instantie. Dan zijn issuer en JWKS gegarandeerd anders.
- De backend accepteert alleen issuers uit de tenant-allowlist (§4.4). Een token van de private broker faalt dan al op `iss`, nog vóór de handtekening.
- Borg het met een test: elke geconfigureerde issuer moet beginnen met de basis-URL van de publieke broker van die omgeving. Zet ook de audience-validatie aan (`nl-portal-m2m`, zie [Audience validatie](../../hosting-en-configuratie/keycloak.md#audience-validatie)).
- Netwerk: de portal-runtime hoeft de private broker niet te bereiken. Een egress-NetworkPolicy maakt het onmogelijk dat de backend er per ongeluk JWKS of tokens ophaalt.
- Federeer de publieke broker niet naar de private, en ook niet omgekeerd voor portaalrealms.

## 5. ASVS 5.0 L3

| Eis | Relevantie | Realm per tenant |
|---|---|---|
| 9.1.x / 9.2.3, 10.3.1 (issuer, audience) | Token alleen accepteren van de juiste issuer en voor de juiste audience | Issuer per tenant maakt de controle eenduidig (§4.4) |
| 10.4.16 (sterke clientauthenticatie) | m2m-client richting Keycloak | `private_key_jwt` per realm (§4.3) |
| 8.x (autorisatie, least privilege) | Geen toegang tot data van een andere tenant | Scheiding in het IdP-model in plaats van alleen in claims |
| 13.x (configuratie, secrets) | Sleutels en secrets per tenant | Eigen realmsleutels; geen gedeeld m2m-secret |

De eisnummers komen uit [asvs-5-l3-token-exchange.md](./asvs-5-l3-token-exchange.md). Voor L3 is de grens per realm het makkelijkst te verantwoorden: die dwingt Keycloak zelf af. In opties 2 en 3 hangt de scheiding af van een correcte flow-override per client en van claimcontroles in de backend. Elke vergeten instelling lekt dan tussen tenants.

## 6. Aanbeveling

1. **Realm per tenant** op de publieke ID-broker, gegenereerd uit één template in Git. Per realm: DigiD-, eHerkenning- en wallet-IdP's van de tenant, `nl-portal` + `nl-portal-m2m`, het thema en een Frontend URL op het gemeentedomein.
2. **Geen Organizations** voor tenants. Het is gemaakt voor B2B-lidmaatschap binnen één realm en leunt op e-maildomeinen. Het lost de scheiding van sleutels, sessies en DigiD-ondertekening niet op. Het kan later nuttig zijn *binnen* een tenant, bijvoorbeeld voor medewerkers van ketenpartners, maar dat is buiten de scope.
3. **Backend:** issuer-allowlist per tenant, issuer gekoppeld aan de tenant van het request, token-endpoint uit config in plaats van uit `jwt.issuer`, audience-validatie aan. Een architectuur- of configuratietest borgt dat alleen issuers van de publieke broker vertrouwd worden.
4. **Vóór livegang navragen bij Logius:** certificaten en mTLS-clientcertificaat bij een meervoudige aansluiting via een leverancier (§4.1).
5. Draai Keycloak ≥ 26.4 met een ruim ingestelde realmcache. Automatiseer met een beperkt service-account, niet met master-admin.

## 7. Niet geverifieerd

- De naam op de PKIoverheid-certificaten en de regels voor een meervoudige aansluiting: de Logius-pdf's waren niet als tekst leesbaar.
- Hoe Keycloak de artifact resolution precies doet (mTLS met de serverbrede `client-keystore`) en of dat aan DigiD voldoet: afgeleid uit de docs, niet getest.
- Of er een native beperking van IdP's per client bestaat: niet gevonden. De workaround met een flow-override is bekend maar niet uitgeprobeerd.
- `private_key_jwt` voor `nl-portal-m2m` in combinatie met de standard token exchange: niet uitgeprobeerd.
- Geheugen en opstarttijd bij 50 realms in onze eigen inrichting: niet gemeten.
