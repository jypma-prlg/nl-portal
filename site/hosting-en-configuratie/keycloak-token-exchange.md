# Standard token exchange

NL Portal gebruikt de **standard token exchange** van Keycloak. Keycloak biedt deze sinds versie 26.2 standaard aan: er zijn geen server features nodig, geen fine-grained admin permissions en geen aparte token-exchange client. De legacy token exchange (v1) wordt niet meer ondersteund.

**Zie ook:** deze pagina beschrijft alleen de token exchange. De overige Keycloak-inrichting staat op de algemene [Keycloak configuratie](./keycloak.md) pagina en is net zo goed verplicht: de [gebruikersattributen](./keycloak.md#gebruikersattributen-burger-flow) `bsn` en `authenticationMethod`, de [`middel` claim](./keycloak.md#de-middel-claim) op de frontend client, en de [redirect-URI](./keycloak.md#clients-en-backend-configuratie) van die client.

## Vereisten

* Keycloak **26.2 of hoger**.
* NL Portal **4.0.0 of hoger**.

Expliciet **niet** nodig:

* geen `KC_FEATURES` (standard token exchange staat standaard aan);
* geen `admin-fine-grained-authz`;
* geen permissions of policies op een doelclient;
* geen aparte token-exchange client.

## Clients

Twee clients volstaan:

| Client (voorbeeldnaam) | Type | Doel |
| ---------------------- | ---- | ---- |
| `nl-portal` | public, standard flow | Login van de frontend (SPA). Draagt de `middel` mapper en de audience mapper. |
| `nl-portal-m2m` | confidential, service accounts, met secret | Voert de token exchange uit namens de backend ("m2m" = machine-to-machine). Draagt de client scope met de `aanvrager` mappers en de audience mapper. |

Bij standard token exchange bouwt Keycloak de nieuwe token op vanuit de **aanvragende** client, niet vanuit een doelclient. De `aanvrager.bsn`/`aanvrager.kvk` mappers horen daarom thuis op de m2m client. Een aparte doelclient is daarom niet nodig.

## Stap 0: de twee clients aanmaken

Heb je de clients al, sla deze stap dan over en ga door naar [stap 1](#stap-1-client-scope-met-de-aanvrager-mappers).

Beide clients maak je aan via *Clients* → **Create client** → *Client type*: `OpenID Connect`.

**`nl-portal` (frontend):**

| Scherm | Instelling | Waarde |
| ------ | ---------- | ------ |
| General settings | *Client ID* | `nl-portal` |
| Capability config | *Client authentication* | **Off** (dit maakt de client public) |
| Capability config | *Standard flow* | aan |
| Capability config | *Service accounts roles* | uit |
| Login settings | *Valid redirect URIs* | de URL van je frontend, bijvoorbeeld `https://portal.example.nl/*` |
| Login settings | *Web origins* | de origin van je frontend, bijvoorbeeld `https://portal.example.nl` |

De redirect-URI moet overeenkomen met de frontend-variabele `OIDC_REDIRECT_URI`; zie [Clients en backend configuratie](./keycloak.md#clients-en-backend-configuratie).

**`nl-portal-m2m` (backend):**

| Scherm | Instelling | Waarde |
| ------ | ---------- | ------ |
| General settings | *Client ID* | `nl-portal-m2m` |
| Capability config | *Client authentication* | **On** (dit maakt de client confidential) |
| Capability config | *Standard flow* | uit |
| Capability config | *Service accounts roles* | **aan** |

Na het opslaan verschijnt op deze client een extra tab *Credentials*. Daar staat de *Client secret* die je in [stap 4](#stap-4-backend-configuratie) bij de backend invult. Klik niet op *Regenerate*: de oude secret is daarna niet meer terug te halen.

## Stap 1: client scope met de aanvrager mappers

Maak via *Client scopes* → **Create client scope** een scope aan (bijvoorbeeld `aanvrager`):

| Instelling | Waarde |
| ---------- | ------ |
| *Name* | `aanvrager` |
| *Type* | `None` (hiermee krijgt niet elke nieuwe client de scope automatisch; je koppelt hem hieronder handmatig aan alleen de m2m client) |
| *Protocol* | `openid-connect` |
| *Include in token scope* | aan |

Voeg daarna op de tab *Mappers* van die scope twee mappers toe via **Configure a new mapper** → *User Attribute*:

| Mapper | User Attribute | Token Claim Name | Claim JSON Type | Add to access token |
| ------ | -------------- | ---------------- | --------------- | ------------------- |
| `bsn` | `bsn` | `aanvrager.bsn` | `String` | aan |
| `kvk` | `kvk` | `aanvrager.kvk` | `String` | aan |

De punt in `aanvrager.bsn` is functioneel: Keycloak nest de claim daardoor als `{"aanvrager":{"bsn":"..."}}`, en dat is precies wat de backend uitleest.

Koppel de scope daarna als **default client scope** aan `nl-portal-m2m`: *Clients* → `nl-portal-m2m` → tab *Client scopes* → **Add client scope** → vink `aanvrager` aan → **Add** → kies **Default**. Kies hier niet *Optional*: dan moet de aanvrager de scope expliciet opvragen, wat de backend niet doet.

Koppel deze scope **niet** aan de frontend client. Dat is wat ervoor zorgt dat de token van de browser nooit een BSN of KVK bevat.

## Stap 2: audience mappers

Een mapper die bij één specifieke client hoort, zet je in Keycloak 26 niet op de client zelf — clients hebben geen *Mappers* tab meer. Je zet hem op de *dedicated* client scope die Keycloak automatisch per client aanmaakt:

*Clients* → `nl-portal` → tab *Client scopes* → klik op **`nl-portal-dedicated`** (de eerste regel in de lijst) → **Add mapper** → **By configuration** → **Audience**.

Vul in:

| Instelling | Waarde |
| ---------- | ------ |
| *Name* | `m2m-audience` (vrij te kiezen) |
| *Included Client Audience* | `nl-portal-m2m` |
| *Add to access token* | aan |
| *Add to ID token* | uit |

Dit is **verplicht**: standard token exchange accepteert een subject token alleen als de aanvragende client al in de `aud` van die token staat. Zonder deze mapper mislukt elke exchange, met de melding `Client is not within the token audience` (zie [Foutmeldingen](#foutmeldingen)).

Voeg via dezelfde route — *Clients* → `nl-portal-m2m` → *Client scopes* → **`nl-portal-m2m-dedicated`** → **Add mapper** → **By configuration** → **Audience** — dezelfde mapper ook toe op `nl-portal-m2m`. Voor de exchange zelf is dat niet nodig, maar hierdoor draagt ook de geëxchangede token de audience — de voorwaarde om [audience validatie](./keycloak.md#audience-validatie) te kunnen aanzetten.

## Stap 3: standard token exchange aanzetten

Zet op `nl-portal-m2m` onder *Settings* → *Capability config* de optie **Standard token exchange** aan.

In een realm export is dit het client attribuut:

```json
"attributes" : {
  "standard.token.exchange.enabled" : "true"
}
```

## Stap 4: backend configuratie

```yaml
nl-portal:
    authentication:
        keycloak:
            resource: nl-portal-m2m
            credentials:
                secret: <secret van de m2m client>
```

Of via de environment variabelen van de app image:

```
KEYCLOAK_CLIENT_ID=nl-portal-m2m
KEYCLOAK_CLIENT_SECRET=<secret>
```

Laat `KEYCLOAK_TOKEN_EXCHANGE_AUDIENCE` **leeg**, tenzij je de uitgewisselde token bewust wilt beperken tot een audience die de m2m client mag uitgeven. De waarde werkt als filter op de `aud` van de uitgewisselde token. Een audience die de m2m client niet mag uitgeven laat de exchange mislukken (zie [Foutmeldingen](#foutmeldingen)).

## Verifiëren

Controleer na het inrichten:

1. Staat **Standard token exchange** aan op de m2m client?
2. Werkt inloggen, en toont Mijn Gegevens nog steeds BRP-gegevens? Dat tweede is de echte controle: het bewijst dat `aanvrager.bsn` via de client scope binnenkomt.

## Foutmeldingen

Mislukt de token exchange, dan logt de backend de melding van Keycloak op niveau `ERROR`, De gebruiker in de browser krijgt die details **niet** te zien: de portal geeft een algemene foutmelding terug. Zoek bij een storing dus altijd in de log van de backend; onderstaande meldingen vind je daar, niet in de frontend.

De regel in de log ziet er zo uit — de melding van Keycloak is het JSON-fragment aan het eind:

```
Token exchange failed with status 403 FORBIDDEN: {"error":"access_denied","error_description":"Client is not within the token audience"}
```

| Melding van Keycloak | Oorzaak |
| -------------------- | ------- |
| `Client is not within the token audience` (`access_denied`, HTTP 403) | De audience mapper op de **frontend** client uit [stap 2](#stap-2-audience-mappers) ontbreekt, waardoor `nl-portal-m2m` niet in de `aud` van de token van de browser staat. Dit is de meest gemaakte fout bij het inrichten. |
| `Standard token exchange is not enabled for the requested client` | [Stap 3](#stap-3-standard-token-exchange-aanzetten) is niet uitgevoerd op de m2m client. |
| `Audience not found` (`invalid_client`) | `KEYCLOAK_TOKEN_EXCHANGE_AUDIENCE` verwijst naar een client die niet bestaat. |
| `Requested audience not available: <client>` | `KEYCLOAK_TOKEN_EXCHANGE_AUDIENCE` verwijst naar een bestaande client die de m2m client niet mag uitgeven. |

## Audience validatie

Zie [Audience validatie](./keycloak.md#audience-validatie) op de algemene Keycloak-pagina voor wat dit is en waarom het verstandig is.

De audience mappers uit [stap 2](#stap-2-audience-mappers) zijn precies de voorwaarde om het aan te kunnen zetten:

```
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_AUDIENCES=nl-portal-m2m
```

**Let op:** dezelfde decoder verwerkt zowel de token van de browser als de geëxchangede token. Ontbreekt de mapper op één van beide clients, dan mislukt élk verzoek. Zet dit dus pas aan nadat stap 2 op beide clients is uitgevoerd.
