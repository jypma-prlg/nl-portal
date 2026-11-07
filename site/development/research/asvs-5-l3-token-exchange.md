# ASVS 5.0.0 level 3 voor Keycloak token-exchange (V2) in NL Portal

Geraadpleegd op: 2026-11-05. Onderzoek voor #131, gebruikt door #132.

> **Historisch:** dit onderzoek beschrijft de toestand vóór #132. V1 en de `token-exchange-version`-schakelaar bestaan niet meer; de toets na #132 staat in [asvs-5-l3-toetsing-token-exchange.md](./asvs-5-l3-toetsing-token-exchange.md).

> **Kader:** er bestaat geen ASVS versie 6. De maintainer bedoelde **hoofdstuk V6 (Authentication)** van **ASVS 5.0.0 level 3** (besloten 2026-11-05). Dit document behandelt V6 in sectie 10 en de aanpalende hoofdstukken V8, V9 en V10 in de rest. Verwijs in code, PR's en documentatie naar `v5.0.0-<hoofdstuk>.<sectie>.<eis>`.

## Samenvatting

- **ASVS v6 bestaat niet.** De nieuwste stabiele versie is **5.0.0** (tag `v5.0.0_release`, gepubliceerd 2025-05-30). De repo `OWASP/ASVS` heeft op `master` alleen de mappen 1.0, 2.0, 3.0, 3.0.1, 4.0, 5.0. Er is geen 6.x-tag, -branch of -release. Wel is er een rolling "bleeding edge" build (`latest`, 2026-10-26); die is geen versie.
- Alle nummers hieronder komen daarom uit **v5.0.0**. In formele notatie is dat `v5.0.0-10.3.1`, niet `v6.x.y.z`. Citeer in rapporten dus `v5.0.0-<hoofdstuk>.<sectie>.<eis>`. Een nummer zonder versie wordt door ASVS zelf als "de laatste versie" gelezen.
- De OAuth/OIDC-eisen staan in **V10** (nieuw in v5; in v4.0.3 bestond geen OAuth-hoofdstuk). Tokenvalidatie staat in **V9** (nieuw; in v4 onderdeel van sessiebeheer). Delegatie en least privilege staan in **V8**.
- Relevantste L3-eisen: **10.3.5** (sender-constrained tokens), **10.4.14** (AS geeft alleen sender-constrained tokens uit), **10.4.16** (sterke clientauthenticatie), **10.2.3** (alleen benodigde scopes vragen), **8.3.2** (autorisatiewijzigingen direct toepassen), **8.3.3** (rechten van de oorspronkelijke subject, niet van de tussenpersoon).
- Belangrijkste bevinding in de code: Spring valideert met alleen `jwk-set-uri` (`application.yml`, regel 28) standaard handtekening en `exp`/`nbf`, maar **niet `iss` en niet `aud`**. Dat raakt 9.2.3 en 10.3.1 (L2, dus ook L3). Dit staat nergens in de gelezen code of config en moet in de volledige configuratie nog bevestigd worden.
- Standard token exchange V2 in Keycloak ondersteunt **geen impersonation** en alleen via een preview-feature delegation (`actor_token`/`may_act`). De NL Portal-"machtiging" (gemachtigde, `urn:etoegang:core:ServiceUUID`) is dus een eigen claimmodel, geen RFC 8693-delegatie.
- 28 eisen opgenomen (zie checklist). Niet geverifieerd: zie sectie 9.

## 1. Bronnen en versie

Het register [normatieve bronnen](../../compliance/normatieve-bronnen.md) (`N-ASVS-5.0`, `N-RFC-8693`, `N-KEYCLOAK-TOKEN-EXCHANGE`) is leidend voor ID, versie en datum; onderstaande tabel geeft de herkomst per hoofdstuk. Bronnen die niet zelf zijn gelezen zijn als zodanig gemarkeerd.

| Bron | Link | Geraadpleegd op 2026-11-05; wijze van raadplegen |
|---|---|---|
| ASVS 5.0.0 V10 | https://github.com/OWASP/ASVS/blob/v5.0.0_release/5.0/en/0x19-V10-OAuth-and-OIDC.md | Primaire bron, volledig gelezen |
| ASVS 5.0.0 V9 | https://github.com/OWASP/ASVS/blob/v5.0.0_release/5.0/en/0x18-V9-Self-contained-Tokens.md | Volledig gelezen |
| ASVS 5.0.0 V8 | https://github.com/OWASP/ASVS/blob/v5.0.0_release/5.0/en/0x17-V8-Authorization.md | Volledig gelezen |
| ASVS 5.0.0 V12 en V13 | .../0x21-V12-Secure-Communication.md, .../0x22-V13-Configuration.md | Alleen eisentabellen gelezen |
| Wijzigingen t.o.v. 4.x | .../0x05-For-Users-Of-4.0.md | Over nummering en nieuwe hoofdstukken |
| Releases | https://github.com/OWASP/ASVS/releases (via `gh api repos/OWASP/ASVS/releases`) | 5.0.0 = 2025-05-30, 4.0.3 = 2021-10-28, `latest` = 2026-10-26 |
| Keycloak | https://www.keycloak.org/securing-apps/token-exchange | Gelezen via samenvattende fetch-tool |
| RFC 8693, 9068, 8707 | rfc-editor.org | Gelezen via samenvattende fetch-tool |

Notatie: de nummers in kolom "Eis" zijn de ID's uit de bron (`10.3.1` = `v5.0.0-10.3.1`). Levels zijn exact overgenomen uit de eisentabellen. In v5 geldt: een eis op level 1 geldt ook voor L2 en L3; een L2-eis geldt ook voor L3. Voor een L3-verificatie zijn dus **alle** hieronder genoemde eisen van L1, L2 en L3 van toepassing.

### Verschillen die tot verwarring kunnen leiden

1. **Er is geen v6.** Documenten of tools die "ASVS v6" noemen verwijzen waarschijnlijk naar 5.0.x of naar de bleeding-edge `master`. README noemt 5.0.1 als eerstvolgende geplande release. Nummers uit `master` kunnen nog veranderen.
2. **Nummering is niet stabiel tussen v4 en v5.** Van de 286 eisen in 4.0.3 zijn er 11 ongewijzigd, maar ook die hebben nieuwe ID's. Een v4-nummer (bijv. `3.5.x` voor tokens, `13.x` voor API) betekent in v5 iets anders. Voorbeeld: v4 `V3` was Session Management, v5 `V3` is Web Frontend Security.
3. **Hoofdstukindeling:** v4 had V1 Architecture (verwijderd in v5) en V4 Access Control (v5: V8 Authorization). OAuth/OIDC (V10) en Self-contained Tokens (V9) zijn nieuw in v5.
4. **Levels zijn ook gewijzigd:** v4 gebruikte vinkjes-kolommen per level; v5 gebruikt een enkel getal 1/2/3 per eis.
5. **Doelen vs. mechanismen:** v5 formuleert eisen als doel (bijv. "sender-constrained"), v4 vaak als mechanisme.
6. **Sectie 10.3 zegt zelf** dat zijn eisen uitgevoerd worden na tokenvalidatie en voor autorisatie op basis van het token; V9 en V10.3 vullen elkaar dus aan.

## 2. Context: wat NL Portal doet (uit de code)

NL Portal is **resource server** (valideert Bearer JWT via `ReactiveJwtDecoder`) en tegelijk **OAuth-client** richting Keycloak (confidential client `nl-portal.authentication.keycloak.resource` + secret).

- `CommonGroundAuthenticationConverter.convert(jwt)` wisselt het binnenkomende access token **elke request** om via `POST {jwt.issuer}/protocol/openid-connect/token` met `grant_type=urn:ietf:params:oauth:grant-type:token-exchange`, `subject_token`, `requested_token_type=access_token`, en `client_id`/`client_secret` in de formbody.
- V1: `audience` is verplicht. V2: `subject_token_type` wordt toegevoegd en `audience` is optioneel (`KeycloakConfig.audience`). Default is nog `V1` (`KEYCLOAK_TOKEN_EXCHANGE_VERSION:v1`).
- Het uitgewisselde token wordt met dezelfde decoder gedecodeerd. Op basis van claim `aanvrager` (bsn/kvk/sub) wordt een `BurgerAuthentication`, `BedrijfAuthentication` of `KeycloakUserAuthentication` opgebouwd. Claims `gemachtigde` en `urn:etoegang:core:ServiceUUID` (machtigingen) worden in `CommonGroundAuthentication` gelezen.
- Autoriteiten komen uit `JwtGrantedAuthoritiesConverter` (standaard `scope`/`scp`, prefix `SCOPE_`).
- Config: alleen `spring.security.oauth2.resourceserver.jwt.jwk-set-uri: ${JWKS_URI}` is gevonden in `backend/app/src/main/resources/config/application.yml`. Er is geen `issuer-uri`, geen custom `OAuth2TokenValidator`, geen audience-check gevonden in `backend/**/src/main` (zoekopdracht op `issuer|audience|JwtValidators|OAuth2TokenValidator`: alleen de token-exchange-code, `IdTokenGenerator` en de yml-bestanden).

Opmerkingen bij de code (relevantie, geen wijziging doorgevoerd):

- **Issuer uit het token als doel van het client secret.** `tokenExchange()` bouwt de token-URL uit `jwt.issuer` en stuurt daar `client_secret` naartoe. Zonder een `iss`-validatie in de decoder kan een token met een andere `iss` (maar toevallig geldige handtekening, bijv. bij een gedeelde JWKS) het secret naar een ander endpoint sturen. Raakt 9.1.3, 10.3.1, 13.2.4. Alleen echt exploiteerbaar als de handtekening geldig is; dat is afhankelijk van de `JWKS_URI`-inrichting en niet geverifieerd.
- **Fallback in de converter (regel ~64):** `jwt.claims[SUB_KEY] != null` bouwt een `KeycloakUserAuthentication` uit het **oorspronkelijke** token (`jwt`), niet het uitgewisselde token. Dat omzeilt de effecten van de exchange (audience-/scope-beperking). Relevant voor 10.3.1/10.3.2 en 8.3.3.
- **Geen `typ`-controle.** Er is geen `at+jwt`-check gevonden (RFC 9068 §4); Keycloak zet standaard `typ: Bearer`, dus die check zou Keycloak-specifiek moeten zijn. Raakt 9.2.2.
- **Foutlogging** schrijft de volledige response body van Keycloak naar de log (`logger.error ... $body`). Geen ASVS-eis in de scope hier; wel noemenswaardig onder logging (V16, niet onderzocht).

## 3. Keycloak standard token exchange V2: relevante feiten

Bron: https://www.keycloak.org/securing-apps/token-exchange (indruk via samenvattende fetch; zie sectie 9).

- Standard token exchange V2 is de ondersteunde variant; feature `token-exchange-standard:v2` is standaard aan en moet per client aangezet worden (schakelaar "Standard token exchange"). Legacy V1 is *Preview* en *Deprecated*, standaard uit, en wordt later verwijderd.
- De aanvragende client moet **confidential** zijn.
- `subject_token` is verplicht en moet een access token zijn; de aanvragende client moet in de `aud` ervan staan (tenzij het token aan die client zelf is uitgegeven).
- `audience`: optioneel; werkt als **filter** op de `aud`-claim (kan alleen versmallen, niet uitbreiden). Een audience waarvoor de gebruiker geen rollen heeft leidt tot een fout.
- `scope`: optioneel; zonder scope alleen standaard client scopes; met scope worden optionele client scopes **toegevoegd** (kan dus verbreden). Least privilege vereist expliciet beheer.
- Refresh tokens: standaard niet uitgegeven; alleen met client-instelling.
- Impersonation (RFC 8693) is in V2 "not implemented yet". Delegation via `actor_token` en `may_act` is een aparte **Preview**-feature (`token-exchange-delegation`) en vereist FGAP V2.
- Levensduur: het document geeft alleen een voorbeeld met `expires_in` 300 s; er is geen algemeen beleid beschreven. Levensduur stel je dus in via realm/client-instellingen.
- Sender-constrained subject tokens vereisen overeenkomend bewijs van bezit.

RFC-achtergrond:

- **RFC 8693** §1.1 definieert impersonation (actor niet te onderscheiden van subject) versus delegation (actor blijft identificeerbaar). §2.1 `audience`/`resource`/`scope` in het verzoek. §4.1 `act`, §4.4 `may_act`. §5 raadt korte levensduur en scope-beperking aan bij delegatie.
- **RFC 9068** §4 (validatie door RS): `typ`, `iss` exact, `aud` bevat eigen identifier, handtekening/`alg` (geen `none`), `exp`. §5: iedere resource een eigen `aud`.
- **RFC 8707** §2: `resource`-parameter; AS moet tokens beperken tot de gevraagde resources (`aud`). §3: voorkeur voor één resource per token; bij meerdere audiences moet er hoog onderling vertrouwen zijn.

## 4. Eisen per onderwerp

Legenda: **Eis** = ID in v5.0.0 (formeel `v5.0.0-<ID>`). Parafrases zijn eigen formuleringen.

### 4.1 Validatie van access tokens (V9, V10.3)

| Eis | Level | Parafrase | Betekenis voor NL Portal |
|---|---|---|---|
| 9.1.1 | L1 | Token valideren via handtekening/MAC vóór de inhoud wordt vertrouwd | Gedekt door `jwk-set-uri`-decoder. Ook voor het **uitgewisselde** token; dat gebeurt (decode van `it.accessToken`). |
| 9.1.2 | L1 | Alleen toegestane algoritmen (allowlist), nooit `none` | Spring Security's `NimbusReactiveJwtDecoder` accepteert standaard alleen RS256 tenzij `jwsAlgorithm` is gezet. Controleren of Keycloak-realm niet op een ander `alg` staat. |
| 9.1.3 | L1 | Sleutelmateriaal alleen uit vooraf geconfigureerde bron; `jku`/`x5u`/`jwk`-headers via allowlist | `JWKS_URI` is vast geconfigureerd (goed). Spring/Nimbus negeert `jku`-headers. |
| 9.2.1 | L1 | `nbf`/`exp` controleren | Standaard aanwezig (`JwtTimestampValidator`) in Spring. Let op clock skew (standaard 60 s). |
| 9.2.2 | L2 | Tokentype controleren (access vs. ID-token) | Niet gevonden. Keycloak ID-tokens zijn met dezelfde sleutel ondertekend; zonder `typ`/`azp`-check kan een ID-token als Bearer worden geaccepteerd. |
| 9.2.3 | L2 | Alleen tokens voor deze service (`aud` op een allowlist) | Niet gevonden: alleen `jwk-set-uri`. Standaard geen `aud`-validatie. Moet toegevoegd worden voor het **binnenkomende** token en voor het **uitgewisselde** token (`KEYCLOAK_TOKEN_EXCHANGE_AUDIENCE`). |
| 9.2.4 | L2 | Gedeelde sleutel over audiences vereist unieke audience-beperking door de uitgever | Keycloak ondertekent per realm met dezelfde sleutel voor alle clients. Dus `aud` moet per client uniek zijn en door NL Portal worden gecontroleerd; anders kan een token voor client A bij NL Portal worden gebruikt. |
| 10.3.1 | L2 | RS accepteert alleen access tokens bedoeld voor deze service (audience), via `aud` of introspectie | Idem 9.2.3. Kernpunt voor token-exchange V2: de V2-`audience`-parameter filtert alleen, dus zonder `aud`-check bij NL Portal is er geen harde grens. |
| 10.3.3 | L2 | Identificeer gebruikers via niet-herbruikbare claims (`iss` + `sub`) | NL Portal gebruikt `bsn`/`kvk`/`aanvrager.sub`. Controleer dat `sub` of `iss+sub` gebruikt wordt voor de interne gebruikersidentiteit waar een unieke gebruiker nodig is (in `userId`-opbouw in `PortalAuthentication`, niet gelezen). |
| 10.3.4 | L2 | Als authenticatiesterkte/-actualiteit vereist is: `acr`/`amr`/`auth_time` toetsen | Relevant voor eHerkenning/DigiD-niveaus (`acr`). Niet gevonden in de gelezen code; toets of er functies zijn die een minimaal betrouwbaarheidsniveau eisen. |

### 4.2 Audience-restricted tokens en tokenexchange zelf (V10, V9)

| Eis | Level | Parafrase | Betekenis voor NL Portal |
|---|---|---|---|
| 10.1.1 | L2 | Tokens alleen naar componenten die ze strikt nodig hebben | Het binnenkomende token gaat als `subject_token` naar Keycloak (nodig). Zorg dat het uitgewisselde token niet verder doorgestuurd wordt naar ZGW-API's anders dan bedoeld, en niet gelogd wordt. |
| 10.4.10 | L2 | Confidential clients authenticeren zich bij back-channel-verzoeken (o.a. tokenverzoeken) | NL Portal doet dit met `client_id` + `client_secret` in de form-body (`client_secret_post`). Voldoet aan 10.4.10 maar niet aan 10.4.16 (L3). |
| 10.4.4 | L1 | Alleen de grants die de client nodig heeft; geen implicit/password | Keycloak-clientconfig: alleen token-exchange (en de grants die de frontend nodig heeft) aan. Voor de NL Portal-client: Direct Access Grants/Implicit uit. |
| 10.4.11 | L2 | AS kent alleen benodigde scopes toe aan de client | Keycloak: default/optional client scopes van de m2m-client beperken. Bij V2 voegt `scope` optionele scopes toe; leg vast welke. |
| 10.4.14 | L3 | AS geeft uitsluitend sender-constrained access tokens uit (mTLS of DPoP) | Keycloak ondersteunt DPoP en mTLS-gebonden tokens (config per client, niet in de hier gelezen docs-pagina geverifieerd). De token-exchange-pagina noemt wel dat sender-constrained subject tokens bewijs van bezit vereisen. Voor L3 betekent dit: ook het uitgewisselde token moet gebonden zijn. |
| 10.3.5 | L3 | RS voorkomt gebruik van gestolen/herhaalde tokens door sender-constrained tokens te eisen (mTLS of DPoP) | NL Portal accepteert nu gewone Bearer-tokens. Voor L3: DPoP/mTLS-validatie in de resource server (Spring Security 6.5+ kent DPoP-ondersteuning; versie in repo niet gecontroleerd). Raakt ook de frontend/BFF-keten. |
| 10.4.16 | L3 | Client is confidential en AS vereist sterke, replay-bestendige clientauthenticatie (mTLS of `private_key_jwt`) | Huidig: `client_secret` in formbody. Voor L3: `private_key_jwt` of mTLS voor de token-exchange-client. |
| 10.4.12 | L3 | AS staat per client alleen het benodigde `response_mode` toe | Relevant voor de frontend-client in Keycloak, niet voor token exchange; vermeld voor volledigheid. |
| 10.4.13 | L3 | Grant `code` altijd samen met PAR | Frontend-client in Keycloak; niet specifiek token exchange. |
| 10.4.15 | L3 | Voor server-side clients: `authorization_details` komt van de client-backend en is niet gemanipuleerd (PAR/JAR) | Alleen relevant als RAR (`authorization_details`) gebruikt wordt; niet gevonden in NL Portal. |

### 4.3 Scopes en least privilege

| Eis | Level | Parafrase | Betekenis voor NL Portal |
|---|---|---|---|
| 10.2.3 | L3 | OAuth-client vraagt alleen de benodigde scopes (of andere autorisatieparameters) | NL Portal stuurt **geen** `scope`-parameter in het exchange-verzoek (alleen audience). Dat betekent: alleen default client scopes van de m2m-client. Controleer dat die default scopes minimaal zijn. Voeg `scope` alleen expliciet toe. |
| 10.4.11 | L2 | zie 4.2 | idem |
| 10.3.2 | L2 | RS beslist op basis van de delegatieclaims (`sub`, `scope`, `authorization_details`) als die aanwezig zijn | `JwtGrantedAuthoritiesConverter` zet `scope` om naar `SCOPE_*`. Controleer dat endpoints daadwerkelijk op scope/rol autoriseren en niet alleen op "geauthenticeerd". Keycloak-rollen (`realm_access`, `resource_access`) worden door de standaardconverter niet gelezen. |
| 8.2.1 | L1 | Functieniveau-toegang beperkt tot consumers met expliciete rechten | Toets de GraphQL/REST-endpoints op autorisatie per functie. |
| 8.2.2 | L1 | Data-specifieke toegang (IDOR/BOLA) | Zaken/documenten per bsn/kvk/machtiging filteren (zie 8.3.3). |
| 8.3.1 | L1 | Autorisatie afdwingen op een vertrouwde servicelaag | Backend is die laag; de frontend mag niet de enige controle zijn. |
| 8.1.1 | L1 | Autorisatieregels gedocumenteerd (functie- en dataniveau) | Documentatie voor scopes/audiences/machtigingen opnemen in arc42. |

### 4.4 Token-levensduur

ASVS 5.0.0 heeft **geen** eis voor een maximale levensduur van access tokens. De verwante eisen:

| Eis | Level | Parafrase | Betekenis voor NL Portal |
|---|---|---|---|
| 9.2.1 | L1 | `nbf`/`exp` controleren | Zie 4.1. |
| 10.4.3 | L1 | Autorisatiecode kortlevend; max 10 min (L1/L2), max 1 min (L3) | AS/Keycloak-instelling voor de frontend-client (`Access Code Lifespan`). Voor L3 maximaal 60 s. |
| 10.4.5 | L1 | Refresh-token-replay bij publieke clients mitigeren (sender-constrained, of rotatie voor L1/L2) | Keycloak, frontend-client. Token exchange V2 geeft standaard geen refresh token uit; laat dat zo. |
| 10.4.8 | L2 | Refresh tokens hebben een absolute vervaldatum | Keycloak `SSO Session Max`/`Client Session Max`. |
| 8.3.2 | L3 | Wijzigingen in autorisatiewaarden direct toepassen; als dat niet kan (zoals bij self-contained tokens), compenserende controles met alarm en terugdraaien | Self-contained tokens bevatten gemachtigden-claims tot `exp`. NL Portal wisselt elke request opnieuw om, wat helpt (Keycloak herberekent rollen), maar een ingetrokken machtiging in de eToegang-/machtigingsdienst is pas zichtbaar als de claim opnieuw wordt opgebouwd. Korte `exp` (RFC 8693 §5 beveelt korte levensduur aan) en een gedocumenteerde compensatie zijn nodig voor L3. |
| 10.4.9 | L2 | Refresh- en reference tokens kunnen door de gebruiker worden ingetrokken | Keycloak account console; niet NL Portal-code. |

### 4.5 Machtigingen: optreden namens een ander (delegatie/impersonatie)

| Eis | Level | Parafrase | Betekenis voor NL Portal |
|---|---|---|---|
| 8.3.3 | L3 | Toegang tot een object gebaseerd op de rechten van de oorspronkelijke subject, niet van de tussenliggende service; bij doorgeven aan een tweede service het token van de gebruiker gebruiken, niet een m2m-token | Dit is de kern van de vraag. NL Portal gebruikt token exchange **met het token van de gebruiker als `subject_token`**, en de uitgewisselde token wordt voor downstream gebruik (ZGW, Objecten) gebruikt; dat past bij 8.3.3. Controleer dat nergens op een m2m-token teruggevallen wordt voor gebruikersdata en dat de fallback met `jwt.claims[SUB_KEY]` (zie sectie 2) niet het oorspronkelijke token naar buiten brengt. |
| 10.3.2 | L2 | zie 4.3 | Claims `aanvrager`, `gemachtigde` en `urn:etoegang:core:ServiceUUID` zijn eigen claims, geen `sub`/`scope`/`authorization_details`; 10.3.2 verlangt ook dat `sub` en `scope` in de beslissing worden meegenomen. |
| 10.3.3 | L2 | Gebruikersidentificatie via `iss`+`sub` | Bij een gemachtigde: onderscheid de ingelogde gebruiker (`sub`) van de gemachtigde (`gemachtigde.sub/bsn/kvk`). De toegangsbeslissing moet op de juiste entiteit plaatsvinden. |
| 10.7.1 – 10.7.3 | L2 | AS vraagt toestemming per autorisatieverzoek, toont duidelijke informatie, gebruiker kan toestemmingen beheren | Alleen relevant als externe clients via Keycloak toegang krijgen namens burgers (consent). Voor first-party NL Portal-frontend meestal niet van toepassing (Keycloak `Consent Required` uit). |
| 8.1.3 / 8.1.4 | L3 | Documenteer contextuele attributen (tijd, locatie, IP, device) en hoe ze meewegen | Alleen nodig als zulke attributen in beslissingen gebruikt worden; nu niet gevonden. |

Let op bij delegatie/impersonatie in token exchange:

- V2 *standard* ondersteunt **geen impersonation**. Een token dat "namens" een burger is uitgegeven blijft dus een token voor de ingelogde Keycloak-gebruiker. Het gemachtigde-model van NL Portal (burger A acteert voor B) hangt af van claims die door Keycloak mappers/eToegang worden gezet.
- Als `act`/`may_act` (RFC 8693) ooit gebruikt wordt: Keycloak levert dat alleen via de preview-feature `token-exchange-delegation`. Een Preview-feature past slecht bij een L3-claim; leg die keuze vast als risico.

### 4.6 Aanvullende eisen die de token-exchange raken

| Eis | Level | Parafrase | Betekenis voor NL Portal |
|---|---|---|---|
| 12.3.1 | L2 | TLS voor alle in- en uitgaande verbindingen, geen terugval | Keycloak-URL komt uit `jwt.issuer`; controleer dat alleen `https` issuers geaccepteerd worden. |
| 12.3.2 | L2 | TLS-clients valideren servercertificaten | `WebClient.create()` gebruikt standaard truststore; geen `insecure`-trustmanager gevonden. |
| 13.2.1 | L2 | Backend-communicatie zonder gebruikerssessie wordt geauthenticeerd met individuele serviceaccounts, korte-termijntokens of certificaten, niet met gedeelde/statische credentials | `client_secret` is een statisch credential; zie ook 10.4.16. |
| 13.2.4 | L2 | Allowlist van externe systemen waarmee de applicatie mag communiceren | Token-endpoint is nu afgeleid uit het token (`jwt.issuer`). Beter: vaste, geconfigureerde issuer. |
| 13.3.1 | L2 | Secrets in een secretsmanagementoplossing | `KEYCLOAK_CLIENT_SECRET` als env-variabele; controleer de hosting. |
| 13.3.4 | L3 | Secrets verlopen en worden geroteerd volgens documentatie | Roteer `client_secret` van de token-exchange-client. |
| 13.1.4 | L3 | Documenteer kritieke secrets en rotatieschema | Opnemen in hosting-documentatie. |

## 5. Wat ASVS 5.0.0 níét eist

- Geen eis over de **vorm** van token exchange (RFC 8693) zelf; ASVS noemt token exchange niet. De eisen werken via audience, scope, sender-constraining en delegatiemodel.
- Geen vaste maximale levensduur voor access tokens.
- Geen eis voor `typ=at+jwt`; wel 9.2.2 (tokentype controleren). RFC 9068 §4 geeft de invulling.
- Geen eis voor `resource`-parameter (RFC 8707), al staat RFC 8707 wel in de ASVS-referenties voor V10. Keycloak ondersteunt voor token exchange de parameter `audience`, niet `resource` (niet verder geverifieerd).

## 6. Hoe te toetsen in code en config: checklist

| Eis | Level | Relevantie voor NL Portal | Hoe te toetsen in code/config |
|---|---|---|---|
| 9.1.1 | L1 | Handtekening binnenkomend en uitgewisseld token | Test met een token met verkeerde handtekening; verwacht 401. Config: `JWKS_URI` aanwezig. |
| 9.1.2 | L1 | Alleen toegestane `alg` | Test met `alg: none` en HS256-token met publieke sleutel; verwacht 401. Zet `jws-algorithms` expliciet. |
| 9.1.3 | L1 | Sleutelbron vast | Controleer dat geen `jku`/`x5u` gevolgd wordt; `JWKS_URI` is https en vast. |
| 9.2.1 | L1 | `exp`/`nbf` | Test met verlopen token. |
| 9.2.2 | L2 | ID-token niet als access token | Test: ID-token als Bearer meesturen; verwacht 401. Overweeg `typ`/`azp`-validator. |
| 9.2.3 | L2 | `aud` binnenkomend token | Custom `OAuth2TokenValidator` of `JwtValidators` + `aud`-check; test met token voor andere client. |
| 9.2.4 | L2 | Unieke `aud` per client | Keycloak: audience-mapper per client; controleer dat NL Portal een eigen audience heeft. |
| 10.3.1 | L2 | Audience-controle RS | Zie 9.2.3; test ook het **uitgewisselde** token tegen `KEYCLOAK_TOKEN_EXCHANGE_AUDIENCE`. |
| 10.3.2 | L2 | Scope/claims in autorisatie | Review `@PreAuthorize`/security-config; test dat een token zonder juiste scope 403 geeft. |
| 10.3.3 | L2 | Identificatie via `iss`+`sub` | Review `PortalAuthentication.userId`-opbouw; geen e-mail/username als sleutel. |
| 10.3.4 | L2 | `acr`/`auth_time` | Review of functies een minimaal betrouwbaarheidsniveau eisen; zo ja, `acr` toetsen. |
| 10.3.5 | L3 | Sender-constrained tokens | Config/code: DPoP- of mTLS-validatie in resource server; test met gestolen token zonder bewijs. |
| 10.1.1 | L2 | Token niet verder dan nodig | Review logging en doorgifte van tokens. |
| 10.2.3 | L3 | Minimale scopes | Controleer `formData` (geen `scope`) en Keycloak default client scopes van de m2m-client. |
| 10.4.4 | L1 | Minimale grants | Keycloak admin: clientinstellingen (Standard Flow, Direct Access, Implicit, Token Exchange). |
| 10.4.10 | L2 | Clientauthenticatie | `client_secret` of beter; zie 10.4.16. |
| 10.4.11 | L2 | Minimale scopes bij AS | Keycloak: client scopes tab. |
| 10.4.14 | L3 | AS geeft alleen sender-constrained tokens | Keycloak: client-instelling DPoP / mTLS-bound tokens (niet geverifieerd in bronnen). |
| 10.4.16 | L3 | Sterke clientauthenticatie | Keycloak: `Client authenticator` = Signed JWT of X509; code: `private_key_jwt` implementeren. |
| 10.4.3 | L1 | Auth-code levensduur (max 1 min voor L3) | Keycloak: Access Code Lifespan. |
| 10.4.5 | L1 | Refresh-token-replay | Keycloak: refresh token rotation (`Revoke Refresh Token`). |
| 10.4.8 | L2 | Absolute vervaldatum refresh token | Keycloak: SSO Session Max, Client Session Max. |
| 8.1.1 | L1 | Autorisatieregels gedocumenteerd | Controleer arc42-documentatie. |
| 8.2.1 / 8.2.2 | L1 | Functie- en data-autorisatie | Review resolvers/services; IDOR-tests op bsn/kvk/machtiging. |
| 8.3.1 | L1 | Autorisatie in vertrouwde laag | Review backend; geen client-only controles. |
| 8.3.2 | L3 | Direct effect van wijzigingen / compensatie | Korte `exp` in Keycloak; documenteer compensatie voor ingetrokken machtigingen; test intrekking tijdens geldig token. |
| 8.3.3 | L3 | Rechten van oorspronkelijke subject | Code-review `CommonGroundAuthenticationConverter` (fallback op regel ~64 gebruikt het oorspronkelijke token); integratietest met twee burgers. |
| 12.3.1 / 12.3.2 | L2 | TLS naar Keycloak | Eis `https` voor issuer; geen insecure truststore. |
| 13.2.1 / 13.3.1 / 13.3.4 | L2/L2/L3 | Credential van de client | Secret in vault; rotatie gedocumenteerd. |
| 13.2.4 | L2 | Allowlist uitgaande verbindingen | Vaste issuer-URL in config i.p.v. `jwt.issuer`; issuer-validatie via `issuer-uri`. |

(Aantal unieke eisen in tabel 4.1–4.6 en checklist: 28 eisen met expliciete rij voor de checklist; 10.4.12, 10.4.13, 10.4.15, 10.7.x, 8.1.3, 8.1.4 zijn alleen in sectie 4 genoemd omdat ze geen directe code/config-toets in NL Portal hebben.)

## 7. Aanbevolen volgorde (prioriteit voor L3)

1. Voeg `iss`- en `aud`-validatie toe aan de `ReactiveJwtDecoder` (9.2.3, 10.3.1, 9.2.4); gebruik een vaste, geconfigureerde issuer voor het token-endpoint.
2. Zet in Keycloak `KEYCLOAK_TOKEN_EXCHANGE_AUDIENCE` en beperk default client scopes; maak `V2` de standaard (nu `V1`).
3. Beslis en documenteer DPoP/mTLS (10.3.5, 10.4.14) en `private_key_jwt` (10.4.16); dit zijn de L3-hoofdpunten.
4. Documenteer en beperk de token-levensduur en compensatie voor ingetrokken machtigingen (8.3.2).
5. Controleer de fallback in de converter (8.3.3).

## 8. Datum en versie

- ASVS-versie: **5.0.0** (stabiel, 2025-05-30). Geen v6 gevonden.
- Datum van raadplegen: **2026-11-05**.
- Methode: `gh api` op `OWASP/ASVS` (bestanden op tag `v5.0.0_release`), plus WebFetch van de Keycloak-pagina en RFC's.

## 9. Niet geverifieerd

- **ASVS v6**: bestaat niet in de bron; alle "v6.x.y.z"-nummers zijn daarom niet te verifiëren. Er is niet gezocht op owasp.org-pagina's zelf, alleen in GitHub (repo, tags, releases, branches); een niet-aangekondigde v6-site is daarmee niet uitgesloten, maar er zijn geen aanwijzingen.
- **Keycloak-pagina en RFC's** zijn gelezen via een samenvattende fetch-tool en niet via de letterlijke tekst. Concrete uitspraken (o.a. "impersonation not implemented yet", `scope` voegt toe, `audience` filtert, subject-token-vereisten, de preview-status van delegation) zijn dus tweedehands tegenover de bron en moeten voor definitief gebruik tegen de pagina zelf gelezen worden. Ook de Keycloak-versie waarin V2 verscheen staat niet op die pagina.
- **Keycloak-ondersteuning voor DPoP/mTLS-gebonden tokens** en `private_key_jwt`: niet uit de Keycloak-bron geverifieerd, alleen genoemd als te onderzoeken.
- **Spring Security-versie en DPoP-ondersteuning** in de repo: niet gecontroleerd.
- **Daadwerkelijke runtime-validatie** (geen `iss`/`aud`-check): afgeleid uit de gevonden config (`jwk-set-uri` zonder `issuer-uri`) en uit het ontbreken van een custom validator bij de zoekopdracht; ik heb niet alle modules gelezen (o.a. `PortalAuthentication`, `PortalAuthenticationConverter`, security-config in `backend/portal`/`app`). Ook productie-configuratie (Helm/env) is niet bekeken.
- **RFC 9068-citaten**: `typ`-check en sectienummers komen uit de samenvattende fetch; de fetch gaf aan dat audience-/confused-deputy-tekst in §5 staat.
- ASVS-eistekst is wel rechtstreeks uit de bronbestanden gelezen; levels en nummers zijn dus exact overgenomen uit de v5.0.0-markdown. Bij 12.x/13.x zijn alleen de tabelregels gelezen (sommige afgekapt op 330 tekens).

## 10. Hoofdstuk V6 Authentication (ASVS 5.0.0)

Bron: https://github.com/OWASP/ASVS/blob/v5.0.0_release/5.0/en/0x15-V6-Authentication.md (volledig gelezen; levels en nummers exact uit de eisentabel). Het hoofdstuk steunt op NIST SP 800-63B en noemt zichzelf niet volledig; het richt zich op veelvoorkomende zwaktes.

**Scope voor NL Portal.** NL Portal verwerkt zelf geen wachtwoorden of tweede factoren: authenticatie loopt via Keycloak met DigiD en eHerkenning als bron (zie `GLOSSARY.md`). De meeste V6-eisen (wachtwoordbeleid, MFA-levenscyclus, herstel, out-of-band) zijn daarom **Keycloak/IdP-inrichting**, niet NL Portal-code. De eisen die de applicatie zelf raken staan in 10.1; de IdP-eisen staan in 10.2 als inrichtingschecklist.

### 10.1 Eisen die NL Portal-code en -documentatie raken

| Eis | Level | Parafrase | Betekenis voor NL Portal | Hoe te toetsen |
|---|---|---|---|---|
| 6.8.2 | L2 | Aanwezigheid en geldigheid van de handtekening op authenticatie-assertions (JWT, SAML) altijd valideren; niet-ondertekende of ongeldige afwijzen | Gedekt door de `jwk-set-uri`-decoder voor het binnenkomende en het uitgewisselde token. Zie ook 9.1.1 en 9.1.2. | Test met ongeldige en `alg: none`-token; verwacht 401. |
| 6.8.1 | L2 | Bij meerdere IdP's mag identiteit niet via een andere IdP te vervalsen zijn; registreer met IdP-id als namespace plus gebruikers-id | NL Portal kent meerdere inlogroutes (burger/DigiD, bedrijf/eHerkenning, medewerker). Identificeer de gebruiker niet op `bsn`/`kvk` alleen, maar met `iss` + `sub` (zie ook 10.3.3). | Review van `userId`-opbouw in `PortalAuthentication`; test met twee tokens met gelijke `sub`, andere `iss`. |
| 6.8.4 | L2 | Wordt specifieke authenticatiesterkte of -recentheid verwacht, controleer die via de gegevens van de IdP (`acr`, `amr`, `auth_time`); levert de IdP die niet, documenteer dan een terugvalaanpak die de laagste sterkte aanneemt | Relevant voor functies die een minimaal DigiD/eHerkenning-betrouwbaarheidsniveau vereisen. Niet gevonden in de gelezen code (zie ook 10.3.4). | Review van functies met niveau-eis; test token met te laag `acr`. |
| 6.1.3 | L2 | Meerdere authenticatieroutes, plus de bijbehorende beveiligingscontroles en sterkte, samen documenteren | Documenteer de routes (DigiD, eHerkenning, medewerker via Keycloak) en de geldende sterkte in arc42 (hoofdstuk 8 of 10). | Controleer de arc42-documentatie. |
| 6.3.4 | L2 | Bij meerdere routes: geen ongedocumenteerde routes, en consistente handhaving van controles en sterkte | Alle routes komen samen in `CommonGroundAuthenticationConverter`; de fallback op `jwt.claims[SUB_KEY]` (sectie 2) is een extra, minder strikt pad en moet worden beoordeeld of verwijderd. | Code-review van de converter; test van elk pad. |
| 6.3.1 | L1 | Controles tegen credential stuffing en brute force uitvoeren volgens de beveiligingsdocumentatie | Ligt bij Keycloak (brute force detection) en DigiD/eHerkenning; NL Portal documenteert de afspraak. | Keycloak-realm: Brute Force Detection aan. |
| 6.1.1 | L1 | Documenteer hoe rate limiting, anti-automation en adaptieve respons zijn ingezet tegen credential stuffing en wachtwoord-brute-force, zonder dat dit tot kwaadwillige accountblokkade leidt | Beschrijf de keuzes in de hosting-documentatie van Keycloak. | Controleer documentatie. |

### 10.2 Eisen die vooral Keycloak/IdP-inrichting zijn (niet in NL Portal-code)

Deze eisen gelden voor de plek waar credentials daadwerkelijk worden afgehandeld; NL Portal toetst ze via de Keycloak-realmconfiguratie, niet via eigen code.

| Groep | Eisen (level) | Opmerking |
|---|---|---|
| Wachtwoorden | 6.2.1 t/m 6.2.8 (L1); 6.2.9 t/m 6.2.12 (L2) | Alleen van toepassing op een eigen Keycloak-gebruikersbestand (bijv. medewerkers). Voor DigiD/eHerkenning bepaalt de aanbieder dit. |
| Algemene beveiliging | 6.3.2 (L1) geen standaardaccounts; 6.3.5 (L3) melding verdachte pogingen; 6.3.6 (L3) e-mail niet als factor; 6.3.7 (L3) melding bij wijziging authenticatiegegevens; 6.3.8 (L3) geen gebruikersdetectie via foutmeldingen of timing | Keycloak-instellingen en -thema's. |
| MFA (eis voor toegang) | **6.3.3 (L2, met L3-clausule)**: MFA of een combinatie van factoren verplicht; voor L3 moet één factor hardware-gebonden en phishingbestendig zijn met gebruikersactie (bijv. FIDO-sleutel). Afwijken vereist een volledig gedocumenteerde rationale en compenserende maatregelen. | Voor burgers via DigiD en bedrijven via eHerkenning bepaalt de aanbieder de factoren en is een afwijking van de L3-clausule onvermijdelijk. Leg dit expliciet vast als gedocumenteerde afwijking met compenserende maatregelen. Voor medewerkers via Keycloak is een FIDO2/WebAuthn-factor haalbaar. |
| Levenscyclus en herstel | 6.4.1 t/m 6.4.2 (L1); 6.4.3 t/m 6.4.4 (L2); 6.4.5 t/m 6.4.6 (L3) | Keycloak-herstelstromen. |
| MFA-algemeen | 6.5.1 t/m 6.5.5 (L2); 6.5.6 t/m 6.5.8 (L3) | Keycloak-OTP/WebAuthn-instellingen (eenmalig gebruik, levensduur, intrekbaar). |
| Out-of-band | 6.6.1 t/m 6.6.3 (L2); 6.6.4 (L3); bij L3 mogen telefoon en SMS geen optie zijn (6.6.1) | Keycloak: SMS-OTP uitzetten voor L3. |
| Cryptografische authenticatie | 6.7.1 t/m 6.7.2 (L3) | Alleen bij gebruik van certificaat- of FIDO-authenticatie in Keycloak. |

### 10.3 Gevolgen voor #132

- De code-eisen uit 10.1 (6.8.1, 6.8.2, 6.8.4, 6.3.4) vallen samen met de bevindingen uit sectie 2 en 7: `iss`/`aud`-validatie, `acr`-toetsing, de fallback in de converter.
- 6.3.3 (L3-clausule) is geen code-eis maar een besluit: **leg de afwijking voor DigiD/eHerkenning vast** (bijvoorbeeld in een ADR of in arc42 hoofdstuk 10), met compenserende maatregelen.
- Dit document wijst de hoofdstuk-V6-eisen aan; of NL Portal ze daadwerkelijk haalt is een toets die bij #132 en een Keycloak-inrichtingsreview hoort.
