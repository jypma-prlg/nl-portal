# Toetsing Keycloak token exchange aan ASVS 5.0.0 level 3

Uitgevoerd op: 2026-11-05, bij #132 (V1 verwijderd, standard token exchange is het enige pad). Eisen en context komen uit het onderzoek [ASVS 5.0.0 level 3 voor token-exchange](./asvs-5-l3-token-exchange.md); het kader (hoofdstuk V6 van ASVS 5.0.0, geen "v6") staat daar.

Deze toets kijkt naar de **code en de standaardconfiguratie in deze repository** (`backend/zgw/common-ground-authentication`, `backend/app/src/main/resources/config/application.yml`, `docker-compose/`). De inrichting van een eigen Keycloak-realm of productieomgeving is niet beoordeeld.

## Resultaat in één oogopslag

| Resultaat | Aantal |
|---|---|
| Voldaan (in code/standaardconfiguratie) | 8 |
| Voldaan na inrichting (hangt af van de uitrol, door NL Portal gedocumenteerd) | 4 |
| Niet voldaan | 15 |
| Buiten NL Portal (Keycloak/IdP-inrichting, niet getoetst; 4 rijen, sommige met meerdere eisen) | 4 |
| Besluit nodig (gedocumenteerde afwijking) | 2 |

**Conclusie:** de token exchange voldoet niet aan ASVS 5.0.0 level 3. Alle L3-hoofdpunten (sender-constrained tokens, sterke clientauthenticatie, scope-minimalisatie, compensatie voor ingetrokken machtigingen) ontbreken, en op L2 ontbreken `iss`-validatie en een standaard `aud`-controle. Het verwijderen van V1 verandert dat niet; het haalt wel de enige variant weg waarvoor audience-beperking structureel onmogelijk was (zie 10.3.1).

## Wat #132 aan de V2-code veranderde

- `audience` is niet langer verplicht (was V1-eis). Staat er een waarde, dan stuurt NL Portal die mee als filter op de `aud` van de uitgewisselde token (kan alleen versmallen).
- `subject_token_type` en `requested_token_type` worden altijd verstuurd.
- Het opstartbericht meldt alleen nog een geconfigureerde audience; er is geen moduskeuze of V1-waarschuwing meer.

## Toets per eis

Legenda: **Voldaan** = aantoonbaar in code of standaardconfiguratie. **Na inrichting** = kan voldaan worden met een documenteerde instelling die de standaard niet aanzet. **Niet voldaan** = ontbreekt. **Buiten NL Portal** = Keycloak/IdP-instelling.

### Tokenvalidatie (V9, V10.3)

| Eis | Lvl | Resultaat | Onderbouwing |
|---|---|---|---|
| 9.1.1 | L1 | Voldaan | `jwk-set-uri`-decoder valideert de handtekening van het binnenkomende token; het uitgewisselde token gaat door dezelfde decoder (`CommonGroundAuthenticationConverter.convert`). |
| 9.1.2 | L1 | Voldaan | `NimbusReactiveJwtDecoder` accepteert standaard alleen RS256; `alg: none` wordt geweigerd. Geen expliciete `jws-algorithms` gezet; Keycloak-realm moet RS256 blijven gebruiken. |
| 9.1.3 | L1 | Voldaan | Sleutelbron is vast (`JWKS_URI`); `jku`/`x5u`-headers worden niet gevolgd. |
| 9.2.1 | L1 | Voldaan | `JwtTimestampValidator` is standaard actief (`exp`/`nbf`, 60 s skew). |
| 9.2.2 | L2 | Niet voldaan | Geen controle op tokentype (`typ`/`azp`); een met dezelfde sleutel ondertekend ID-token wordt als Bearer geaccepteerd. |
| 9.2.3 | L2 | Na inrichting | `spring.security.oauth2.resourceserver.jwt.audiences` bestaat en is gedocumenteerd ([Audience validatie](../../hosting-en-configuratie/keycloak.md#audience-validatie)), maar de app image zet het **niet** aan. Zonder die waarde wordt elke token van het realm geaccepteerd. |
| 9.2.4 | L2 | Na inrichting | Vereist een unieke audience per client (audience mappers in de [token exchange pagina](../../hosting-en-configuratie/keycloak-token-exchange.md#stap-2-audience-mappers)) én de property uit 9.2.3. |
| 10.3.1 | L2 | Na inrichting | Zelfde als 9.2.3. Met de standard token exchange werkt `audience` als filter; de harde grens ontstaat pas door de `audiences`-property aan te zetten. (Met V1 kon de uitgewisselde token geen gedeelde audience met de browsertoken dragen; die belemmering is weg.) |
| 10.3.2 | L2 | Niet voldaan | `JwtGrantedAuthoritiesConverter` zet alleen `scope` om naar `SCOPE_*`; de claims `aanvrager`, `gemachtigde` en `urn:etoegang:core:ServiceUUID` zijn eigen claims. Of alle endpoints op scope/rol autoriseren (in plaats van op "geauthenticeerd") is niet onderzocht; zonder dat bewijs geldt de eis als niet voldaan. |
| 10.3.3 | L2 | Niet voldaan | Gebruikers worden op `bsn`/`kvk`/`aanvrager.sub` herkend; `iss` + `sub` wordt niet als sleutel gebruikt. Zie ook 6.8.1. |
| 10.3.4 | L2 | Niet voldaan | Geen toetsing van `acr`/`amr`/`auth_time` gevonden. Zie ook 6.8.4. |

### Tokenexchange, clients en grants (V10)

| Eis | Lvl | Resultaat | Onderbouwing |
|---|---|---|---|
| 10.1.1 | L2 | Voldaan | Het binnenkomende token gaat alleen als `subject_token` naar Keycloak. Het uitgewisselde token wordt niet gelogd. Let op: bij een mislukte exchange logt de backend de volledige response-body van Keycloak (`logger.error`), zonder token, maar met `error_description`; de gebruiker krijgt die niet te zien. |
| 10.4.4 | L1 | Buiten NL Portal | Clientgrants staan in Keycloak. De [token exchange pagina](../../hosting-en-configuratie/keycloak-token-exchange.md#stap-0-de-twee-clients-aanmaken) schrijft voor: m2m zonder standard flow, frontend zonder service accounts. |
| 10.4.10 | L2 | Voldaan | Confidential client; `client_id` + `client_secret` in de form-body (`client_secret_post`). |
| 10.4.11 | L2 | Na inrichting | De `aanvrager`-scope is als *Default* aan de m2m-client gekoppeld en niet aan de frontend. De overige default scopes van de m2m-client zijn niet in de repo vastgelegd. |
| 10.2.3 | L3 | Niet voldaan | Het exchange-verzoek stuurt geen `scope`; dat levert alle default scopes van de m2m-client op. Minimale scopes zijn niet afgedwongen of vastgelegd. |
| 10.3.5 | L3 | Niet voldaan | Gewone Bearer-tokens; geen DPoP- of mTLS-validatie in de resource server. |
| 10.4.14 | L3 | Niet voldaan | De token endpoint geeft niet-sender-constrained tokens uit; de aanvraag stuurt geen DPoP-proof. |
| 10.4.16 | L3 | Niet voldaan | `client_secret_post`; geen `private_key_jwt` of mTLS. |
| 10.4.12, 10.4.13, 10.4.15 | L3 | Buiten NL Portal | Betreffen de frontend-client of RAR in Keycloak; niet van toepassing op de token exchange zelf. |

### Autorisatie, levensduur en delegatie (V8)

| Eis | Lvl | Resultaat | Onderbouwing |
|---|---|---|---|
| 8.3.2 | L3 | Niet voldaan | De exchange gebeurt per request, maar een ingetrokken machtiging blijft zichtbaar tot de claim opnieuw wordt opgebouwd. Er is geen vastgelegde `exp`-limiet of compensatie. |
| 8.3.3 | L3 | Niet voldaan | De exchange gebruikt het token van de gebruiker als `subject_token` (goed). Maar `CommonGroundAuthenticationConverter` bouwt in het terugvalpad `jwt.claims[SUB_KEY]` een `KeycloakUserAuthentication` uit het **oorspronkelijke** token, niet uit het uitgewisselde. Dat omzeilt de beperkingen van de exchange. |

### Transport, secrets en token-endpoint (V12, V13)

| Eis | Lvl | Resultaat | Onderbouwing |
|---|---|---|---|
| 12.3.1 | L2 | Niet voldaan | De token-URL komt uit `jwt.issuer`; er is geen controle dat dit `https` is. |
| 12.3.2 | L2 | Voldaan | `WebClient.create()` gebruikt de standaard truststore; geen insecure trustmanager. |
| 13.2.4 | L2 | Niet voldaan | Het token-endpoint wordt uit het token afgeleid (`jwt.issuer`) in plaats van uit vaste configuratie, en `iss` wordt niet gevalideerd (geen `issuer-uri`). Bij een geldige handtekening met een andere `iss` gaat het `client_secret` naar dat endpoint. |
| 13.2.1 / 13.3.1 / 13.3.4 | L2/L2/L3 | Buiten NL Portal | Secret via `KEYCLOAK_CLIENT_SECRET` (env); opslag in een secretsmanager en rotatie zijn een hostingkeuze. |

### Hoofdstuk V6 (Authenticatie)

| Eis | Lvl | Resultaat | Onderbouwing |
|---|---|---|---|
| 6.8.2 | L2 | Voldaan | Handtekening wordt altijd gevalideerd (zie 9.1.1/9.1.2). |
| 6.8.1 | L2 | Niet voldaan | Zie 10.3.3. |
| 6.8.4 | L2 | Niet voldaan | Zie 10.3.4. |
| 6.3.4 | L2 | Niet voldaan | Alle routes komen samen in één converter, maar het terugvalpad op het oorspronkelijke token (8.3.3) is een extra, minder strikt pad. |
| 6.1.3 | L2 | Besluit nodig | Authenticatieroutes (DigiD, eHerkenning, medewerker) zijn nog niet gedocumenteerd in arc42. |
| 6.3.3 | L2/L3 | Besluit nodig | De L3-clausule (hardware-gebonden, phishingbestendige factor) is voor DigiD/eHerkenning niet haalbaar; leg de afwijking met compenserende maatregelen vast. |
| 6.3.1, 6.1.1 | L1 | Buiten NL Portal | Brute-force-bescherming ligt bij Keycloak, DigiD en eHerkenning. |

## Vervolgwerk

Dit zijn bevindingen, geen onderdeel van #132. Elk punt raakt een eigen ontwerpbesluit:

1. `iss`- en `aud`-validatie in de `ReactiveJwtDecoder` aanzetten en een vaste token-endpoint-URL gebruiken (9.2.3, 10.3.1, 13.2.4, 12.3.1).
2. Het terugvalpad op het oorspronkelijke token in de converter verwijderen of beoordelen (8.3.3, 6.3.4).
3. `typ`/`azp`-validatie (9.2.2) en `iss` + `sub` als gebruikerssleutel (10.3.3, 6.8.1).
4. Besluit over DPoP/mTLS en `private_key_jwt` (10.3.5, 10.4.14, 10.4.16); dat zijn de L3-hoofdpunten.
5. Scopes, levensduur en compensatie voor ingetrokken machtigingen vastleggen (10.2.3, 8.3.2).
6. Afwijking 6.3.3 en de authenticatieroutes (6.1.3) vastleggen in arc42 of een ADR.

## Beperkingen van deze toets

- Eisen zijn getoetst aan de code en standaardconfiguratie in de repo, niet aan een draaiende Keycloak. "Na inrichting" betekent dat de uitkomst per uitrol verschilt.
- De ASVS-teksten en de Keycloak-feiten komen uit het onderzoeksdocument; de beperkingen daar (sectie 9) gelden hier ook.
- Er is geen penetratietest of runtime-toets uitgevoerd (bijvoorbeeld met een ID-token als Bearer).
