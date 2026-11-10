# Diftar-API voor afvalverbruik: aanbod en authenticatie

Geraadpleegd op: 2026-11-10. Onderzoek voor #394 (onderdeel van wayfinder-map #391), gebruikt door #396 (module afval).

## Samenvatting

- Er is een **openbare, door PinkRoccade beheerde OpenAPI-specificatie**: de "Diftar Ledigingen API" in de repo [pinkroccade/diftar-api](https://github.com/pinkroccade/diftar-api) (publiek, laatste gelezen commit `e3c93f7`, 2026-11-10). Spec: OpenAPI 3.0.3, `info.version` 1.1.0, licentie CC0-1.0. Gerenderde documentatie: <https://pinkroccade.github.io/diftar-api/> (HTTP 200 bevestigd).
- De spec is een **herziening van een door de leverancier NewWaste aangeleverde eerste versie**, bedoeld voor de "mijnomgeving" van NL Portal (README). Er is geen openbare leveranciersdocumentatie van NewWaste gevonden (zie sectie 6).
- Endpoints: `GET /ledigingen` (minicontainer-ledigingen), `GET /stortingen` (stortingen in verzamelcontainers) en `GET /openapi.json` (zonder authenticatie). Alleen lezen.
- Authenticatie: OAuth2 Client Credentials volgens het NL GOV Assurance profile for OAuth 2.0 v1.1.0 (Direct Access Client): `private_key_jwt`, JWT-access token met `iss`, `azp`, `exp`, `aud`, `scope`, tokenendpoint bij een aparte autorisatieserver (ADR 0001 en 0002 in dezelfde repo).
- De API kent **geen burgers en geen BSN**, alleen adressen. NL Portal moet zelf het adres van de ingelogde burger bepalen (Haal Centraal BRP) en afdwingen dat een burger alleen zijn eigen adres opvraagt.
- De `servers`-URL's, tokenendpoint, issuer-URL en beheerdercontact in de spec zijn **placeholders** (`*.example`, `TODO`). Er is geen publieke sandbox. Er is wel een mock-service in ontwerp en bouw (diftar-api issue #6, #32 e.v.).
- Open bij leverancier of klant: echte omgevings-URL's, autorisatieserver en `aud`-waarde, sleutelregistratie, codelijst `fractieCode`, gewicht of volume per storting, testdata. Zie sectie 7.

## 1. Bronnen

| Bron | Soort | Wijze van raadplegen |
|---|---|---|
| [diftar-openapi.yaml](https://github.com/pinkroccade/diftar-api/blob/main/diftar-openapi.yaml) | Primair: de specificatie zelf | Volledig gelezen (534 regels) |
| [README.md](https://github.com/pinkroccade/diftar-api/blob/main/README.md), [GLOSSARY.md](https://github.com/pinkroccade/diftar-api/blob/main/GLOSSARY.md), [STANDARDS.md](https://github.com/pinkroccade/diftar-api/blob/main/STANDARDS.md) | Primair, beheerder van de spec | Volledig gelezen |
| [ADR 0001](https://github.com/pinkroccade/diftar-api/blob/main/docs/adr/0001-client-credentials-security.md), [ADR 0002](https://github.com/pinkroccade/diftar-api/blob/main/docs/adr/0002-security-conform-nl-gov-oauth-profiel.md) | Primair, ontwerpbesluiten | Volledig gelezen |
| [diftar-api issue #11](https://github.com/pinkroccade/diftar-api/issues/11) (antwoord NewWaste), [#6](https://github.com/pinkroccade/diftar-api/issues/6), [#32](https://github.com/pinkroccade/diftar-api/issues/32) | Primair, projectadministratie | Gelezen via `gh` |
| [NL GOV Assurance profile for OAuth 2.0 v1.1.0](https://gitdocumentatie.logius.nl/publicatie/api/oauth/v1.1.0/) | Standaard waar de spec naar verwijst | Alleen via de verwijzing in de spec; niet zelf opnieuw gelezen |

De spec is een ontwerp, geen bewijs van een draaiende productie-API. Wat NewWaste daadwerkelijk heeft geïmplementeerd is niet aan een openbare bron te toetsen. Wijzigingen na 2026-11-10 zijn niet meegenomen; de repo is op die datum nog in beweging (meerdere open bouwtickets).

## 2. Endpoints

Basispad per omgeving `https://<omgeving>.diftar-api.example/v1` (placeholders).

| Methode en pad | Doel | Scope | Bron |
|---|---|---|---|
| `GET /ledigingen` | Ledigingen van minicontainers voor een adres in een periode | `diftar.ledigingen.lezen` | spec, `paths./ledigingen` |
| `GET /stortingen` | Stortingen in verzamelcontainers voor een adres in een periode; alleen transacties van type 101, gekoppeld via de pas van het adres | `diftar.stortingen.lezen` | spec, `paths./stortingen` |
| `GET /openapi.json` | De specificatie als JSON; zonder security, met CORS `*` | geen | spec, `paths./openapi.json` |

Queryparameters van `/ledigingen` en `/stortingen`:

- Adres **of** BAG-id, niet beide en niet geen van beide (anders `400`): `postcode` (patroon `^[1-9][0-9]{3}[A-Z]{2}$`), `huisnummer` (integer, minimaal 1), optioneel `huisletter` en `huisnummertoevoeging`; alternatief `nummeraanduidingIdentificatie` (16 cijfers).
- `vanaf` en `totEnMet`: verplicht, datum (RFC 3339), beide inclusief.
- `page` (1-gebaseerd, standaard 1) en `pageSize` (1 tot 200, standaard 50).

Responsheader `API-Version` op alle succesresponses.

## 3. Datamodel

`LedigingenCollectie`: `_links` (`self`, `first`, `last`, `next`, `prev`; `self` verplicht), `totaalAantalLedigingen`, `ledigingen[]`.

`Lediging`: `datumTijd` (date-time, verplicht), `fractieCode` (string, verplicht), `fractieOmschrijving`, `gewichtInKg` (double).

`StortingenCollectie`: `_links`, `totaalAantalStortingen`, `stortingen[]`.

`Storting`: `datumTijd` (verplicht), `fractieCode` (verplicht), `fractieOmschrijving`, `volumeInLiters` (integer), `VerzamelcontainerTitel`, `VerzamelcontainerId`. De laatste twee veldnamen beginnen in de spec met een hoofdletter, afwijkend van de rest (camelCase); mogelijk een spec-foutje, te bevestigen.

Foutantwoorden zijn `application/problem+json` (RFC 7807) met `type`, `title`, `status`, optioneel `detail`, `instance` en `invalidParams[]` (`naam`, `reden`).

Gedrag bij adressen, volgens de spec en het antwoord van NewWaste (diftar-api issue #11):

- Bekend adres of BAG-id zonder data in de periode: `200` met lege lijst.
- Onbekend adres of BAG-id: `404` met Problem Details.
- BAG-id dat niet bij de gemeente hoort: `404` of `403`; de spec legt dit nog niet vast (alleen `404` is voor onbekend gedocumenteerd, `403` staat in de spec voor onvoldoende scope).

Wat de API niet bevat: geen burger- of BSN-gegevens, geen pasnummers, geen tarieven of kosten, geen ophaalkalender, geen containerinventaris per adres. De spec modelleert alleen gebeurtenissen (ledigingen, stortingen). Een module "afval" in NL Portal die meer toont dan verbruikshistorie heeft een andere bron nodig.

Domeintermen (GLOSSARY.md van de spec): Lediging (vervangt leveranciersterm "mc_dump"/"dump"), Fractie, Nummeraanduiding (BAG), NewWaste (leverancier die de API host).

## 4. Authenticatie en autorisatie

Volgens het securityschema `OAuth2ClientCredentials` in de spec en ADR 0002:

- Flow: Client Credentials, profiel "Direct Access Client" van het NL GOV Assurance profile for OAuth 2.0 v1.1.0. Geen `client_secret`; clientauthenticatie uitsluitend met `private_key_jwt`.
- De publieke sleutel van NL Portal wordt vooraf geregistreerd bij de autorisatieserver, bij voorkeur via `jwks_uri`.
- Het tokenendpoint staat niet bij de Diftar-API maar bij een autorisatieserver; `tokenUrl` en de discovery-URL zijn in de spec nog `TODO-autorisatieserver.example.nl`.
- Access token: ondertekende JWT met minimaal `iss`, `azp`, `exp`, `aud`, `scope` en een `kid`-header. De `aud` is een vaste identifier van de Diftar-API; de waarde is nog niet vastgelegd (ADR 0002).
- De API controleert handtekening, `iss`, `aud`, `exp` en scope. `401` en `403` geven `WWW-Authenticate: Bearer` (RFC 6750).
- Netwerk: ADR 0001 noemt een besloten verbinding (Pink Private Cloud naar NewWaste); PKIoverheid/OIN en mTLS zijn vooralsnog niet verplicht, TLS wel.
- Autorisatie per burger ligt volledig bij NL Portal (ADR 0001): de backend haalt het adres uit Haal Centraal en vraagt alleen dat adres op. Een compromittatie van de NL Portal-backend geeft toegang tot alle adressen; ADR 0001 benoemt dit als bewust geaccepteerd risico.
- Gevolg voor de module: een eigen OAuth-client met `private_key_jwt` (geen gedeeld geheim) en sleutelbeheer per koppeling. Of de bestaande HTTP-clientconfiguratie in NL Portal dit al ondersteunt is hier niet onderzocht.

## 5. Sandbox, testomgeving en mock

- De `servers` in de spec (ontwikkel, acceptatie, productie) zijn placeholders; README en STANDARDS noemen dit als open punt: invullen zodra de omgevingen bekend zijn. Er is dus **geen publiek bereikbare sandbox**.
- In de repo is een **mock-service** in ontwerp en bouw (kaart diftar-api issue #6): MockServer in Docker die `diftar-openapi.yaml` importeert, dataset in JSON, scenario-adressen, authenticatiemodi `none`, `builtin` en `keycloak` (de mock mag zelf tokens uitgeven, dat is geen deel van het contract, ADR 0002). Bouwtickets #32 tot en met #36 stonden op 2026-11-10 nog open; er is dus nog geen opgeleverd image. Dit past bij de aanname in #391 dat NL Portal een mockservice niet van een echte API onderscheidt.

## 6. Wat is publiek te vinden buiten de spec

Een zoekopdracht (2026-11-10) naar openbare API-documentatie van NewWaste of Diftar voor gemeentekoppelingen leverde **niets primairs** op: geen openbare leveranciersdocumentatie en geen OpenAPI van NewWaste zelf. Gemeentelijke aanbestedingsstukken noemen hooguit een voorkeur voor een API-koppeling met het afvalpassysteem, zonder technische details. Diftar zelf is een heffingsmodel (betalen op basis van aanbiedingen of ledigingen), geen API-standaard. Onofficiële scrapers van afvalwijzer-sites zijn geen primaire bron en niet gebruikt.

## 7. Open vragen: op te vragen bij leverancier (NewWaste) of klant

Bij NewWaste:

1. Welke omgevingen bestaan er (ontwikkel, acceptatie, productie), wat zijn de basis-URL's, en is er een sandbox met testdata voor NL Portal?
2. Welke autorisatieserver geeft tokens uit (issuer, discovery-URL, tokenendpoint), wat is de `aud`-waarde, en hoe registreren we de publieke sleutel (`jwks_uri` of anders)? Worden de scopes `diftar.ledigingen.lezen` en `diftar.stortingen.lezen` zo geïmplementeerd?
3. Is spec-versie 1.1.0 daadwerkelijk geïmplementeerd door NewWaste, en met welke planning? Welk versiebeleid geldt?
4. Codelijst voor `fractieCode` (nu vrije string).
5. Betekenis van "type 101" bij stortingen; waarom geen gewicht bij stortingen en geen volume bij ledigingen; zijn `VerzamelcontainerTitel` en `VerzamelcontainerId` met hoofdletter bedoeld?
6. Gedrag bij een BAG-id buiten de gemeente (`403` of `404`), rate limits, maximale periode, en vertraging tussen een lediging en de zichtbaarheid ervan.
7. Hoe gaat de API om met verhuizingen en meerdere bewoners of passen op één adres (gegevens van een vorige bewoner op hetzelfde adres)?

Bij de klant (gemeente):

1. Welke gemeente en welke Diftar-installatie; is toegang tot de acceptatieomgeving geregeld?
2. Privacy: is het tonen van ledigingen per adres aan de bewoner toegestaan (DPIA, verwerkingsregister)?
3. Welke afvalinformatie moet de module tonen buiten verbruik (tarieven, kosten, ophaalkalender)? Die staat niet in deze API.
4. Hoe verloopt de private netwerkverbinding (Pink Private Cloud) richting NewWaste vanuit de omgeving waar NL Portal draait?

## 8. Gevolgen voor de module afval (voor #396)

- De backend haalt het adres (postcode, huisnummer, toevoeging of BAG-id) van de ingelogde burger via Haal Centraal op en geeft alleen dat door; geen BSN naar de Diftar-API.
- Verwacht een HTTP-client met OAuth2 Client Credentials en `private_key_jwt`, tokencache en `aud`/scope per koppeling.
- Paginering via `page`/`pageSize` en `_links` en een verplichte periode (`vanaf`, `totEnMet`) moeten in de GraphQL-laag een plek krijgen.
- Leeg (`200`) en onbekend adres (`404`) zijn verschillende uitkomsten die de UI moet onderscheiden.
