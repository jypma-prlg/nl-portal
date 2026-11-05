# Keycloak configuratie

NL Portal gebruikt Keycloak voor de authenticatie van gebruikers. Deze pagina beschrijft alle vereisten om een Keycloak in te richten voor gebruik met de NL Portal: de clients, de token exchange, gebruikersattributen en claims.

Deze documentatie beschrijft de **burger flow** (gebruiker met BSN, ingelogd via DigiD) en de **generieke gebruikersflow** (Keycloak gebruiker zonder BSN of KVK). Voor overige flows, zoals bedrijven (eHerkenning) en machtigingen, verwijzen we naar de broncode of [support](../development/productmanagement/community-en-support.md). Zie [Overige flows](#overige-flows).

## Token exchange

Als je een api call doet naar de backend wordt de token onderschept en wordt er een call naar Keycloak gedaan, waarmee een nieuwe token wordt opgehaald waar de bsn/kvk wél in zit. Deze wordt daarna in de rest van de applicatie gebruikt. De token van de browser bevat nooit een BSN of KVK.

NL Portal gebruikt de [standard token exchange](./keycloak-token-exchange.md). Die vereist Keycloak 26.2 of hoger, geen server features en twee clients. De legacy token exchange (v1) wordt niet meer ondersteund.

Zie de [officiële Keycloak documentatie](https://www.keycloak.org/securing-apps/token-exchange) voor achtergrond over de token exchange.

## Clients en backend configuratie

Je hebt twee clients nodig:

| Client (voorbeeldnaam) | Type | Doel |
| ---------------------- | ---- | ---- |
| `nl-portal` | public, standard flow | Login van de frontend (SPA). Draagt de `middel` mapper. |
| `nl-portal-m2m` | confidential, service accounts, met secret | Voert de token exchange uit namens de backend ("m2m" = machine-to-machine). |

De volledige inrichting staat op de [token exchange pagina](./keycloak-token-exchange.md#clients).

De koppeling met de backend verloopt via de volgende environment variabelen van de app image (zie ook de [Deployment guide](./deployment-guide.md)):

| Variabele | Betekenis |
| --------- | --------- |
| `JWKS_URI` | Wijst de backend naar het realm waarmee hij tokens valideert: `<keycloak-url>/realms/<realm>/protocol/openid-connect/certs`. Draait je Keycloak onder een pad (zoals de demo-omgeving, met `KC_HTTP_RELATIVE_PATH=/auth`), neem dat pad dan mee: `<keycloak-url>/auth/realms/<realm>/protocol/openid-connect/certs`. |
| `KEYCLOAK_CLIENT_ID` | Client id van de m2m (backend) client |
| `KEYCLOAK_CLIENT_SECRET` | Secret van de m2m (backend) client |
| `KEYCLOAK_TOKEN_EXCHANGE_AUDIENCE` | Optioneel. Filter op de `aud` van de uitgewisselde token. Laat leeg, tenzij de m2m client deze audience mag uitgeven. |

De redirect-URI van de `nl-portal` (frontend) client moet overeenkomen met de frontend-variabele `OIDC_REDIRECT_URI`. De SPA plaatst zijn OIDC-callback automatisch op dat pad, dus elk pad op je eigen origin werkt; registreer die redirect-URI op de client.

## Gebruikerstypen

De backend bepaalt het type gebruiker op basis van claims in de **geëxchangede** token.:

1. Claim `aanvrager.bsn` aanwezig → de gebruiker is een **burger**.
2. Claim `aanvrager.kvk` aanwezig → de gebruiker is een **bedrijf** (zie [Overige flows](#overige-flows)).
3. Geen van beide → de gebruiker is een **generieke gebruiker** (zie [Generieke gebruikers](#generieke-gebruikers-sub-flow)).

## Gebruikersattributen: burger flow

Voor de burger flow betekent dit:

* De gebruiker in het portal realm heeft een **user attribute** `bsn` (bijvoorbeeld `999993847`).
* Er staat een protocol mapper (type *User Attribute*) die het user attribute `bsn` mapt naar de claim `aanvrager.bsn` in het access token. Die mapper hoort op een [client scope van de m2m client](./keycloak-token-exchange.md#stap-1-client-scope-met-de-aanvrager-mappers).
* Daarnaast heeft de gebruiker het user attribute `authenticationMethod` met waarde `digid`, dat via de `middel` claim de frontend features bepaalt (zie [De middel claim](#de-middel-claim)).

Een burger heeft daarmee toegang tot onder andere de Mijn Gegevens pagina (BRP gegevens via Haal Centraal) en ziet zaken waarop hij of zij als initiator met dat BSN geregistreerd staat.

## Generieke gebruikers (sub flow)

Een gebruiker zonder `aanvrager.bsn` of `aanvrager.kvk` claim wordt behandeld als generieke Keycloak gebruiker. De backend identificeert deze gebruiker met de eerste 13 karakters van de `sub` claim, met identificatietype `uid`.

Voor generieke gebruikers werkt een deel van de portal functionaliteit:

| Functionaliteit | Werkt voor generieke gebruiker? | Toelichting |
| --------------- | ------------------------------- | ----------- |
| Zaken | Ja | Zaken moeten een rol hebben met `betrokkeneIdentificatie.natuurlijkPersoon.anpIdentificatie` gelijk aan de uid (eerste 13 karakters van `sub`). |
| Taken | Ja | Taakobjecten met `identificatie.type` = `uid` en `identificatie.value` = de uid. |
| Berichten | Ja | Berichtobjecten met dezelfde uid-identificatie. |
| Mijn Gegevens (BRP) | Nee | Vereist een burger (BSN); de pagina toont geen gegevens. |
| OpenKlant 2 (partijen) | Nee | Vereist een burger of bedrijf; queries geven een foutmelding. |

## De middel claim

De frontend leest de claim `middel` uit het access token van de **frontend client** en bepaalt daarmee welke features actief zijn. De standaard app image is geconfigureerd met de volgende authenticatiemethoden (aanpasbaar in `frontend/packages/app/src/App.tsx` in de monorepo):

| Categorie | Waarden van `middel` | Gedrag |
| --------- | -------------------- | ------ |
| person | `digid`, `machtigen` | Persoonsweergave: Mijn Gegevens toont BRP gegevens. |
| company | `eherkenning`, `bewindvoering` | Bedrijfsweergave (zie [Overige flows](#overige-flows)). |
| proxy | `machtigen`, `bewindvoering` | Machtigingsflow (zie [Overige flows](#overige-flows)). |

Voor de burger flow stel je dit in met:

* Het user attribute `authenticationMethod` met waarde `digid` op de gebruiker.
* Een protocol mapper (type *User Attribute*) op de **frontend client** die het user attribute `authenticationMethod` mapt naar de claim `middel` in het access token.

**Let op:** als de `middel` claim ontbreekt valt de frontend terug op de persoonsweergave. De Mijn Gegevens pagina toont dan alleen gegevens als de gebruiker ook daadwerkelijk een burger is (BSN attribuut én mapper correct ingesteld). Is dat niet het geval, dan toont de pagina een foutmelding.

## Externe identity providers

De attributen `bsn` en `authenticationMethod` zijn **user attributes op de Keycloak gebruiker** in het portal realm. Wanneer gebruikers via een externe identity provider inloggen (bijvoorbeeld Azure AD of DigiD via identity brokering), bestaan deze attributen niet vanzelf. Configureer in dat geval **identity provider mappers** in Keycloak die de attributen bij het inloggen op de gebruiker zetten. De protocol mappers op de clients (zie hierboven) mappen de attributen vervolgens naar de claims.

## Audience validatie

De app image configureert alleen een `jwk-set-uri` en verder geen validatie op de ontvangen token. Daardoor wordt **elke** token die door hetzelfde realm is ondertekend geaccepteerd, ongeacht welke client die heeft uitgegeven. Een andere applicatie in hetzelfde realm kan een token van een gebruiker dus doorspelen naar de portal API.

Je sluit dat af door de backend alleen tokens te laten accepteren met een bepaalde audience:

```
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_AUDIENCES=nl-portal-m2m
```

of als property:

```yaml
spring:
    security:
        oauth2:
            resourceserver:
                jwt:
                    audiences: nl-portal-m2m
```

Twee aandachtspunten:

* De property accepteert een lijst en de controle slaagt als `aud` **één** van de waarden bevat.
* Zet er geen `account` in: die audience zit op vrijwel elke token in het realm, waardoor de controle niets meer tegenhoudt.

**Voorwaarde: de tokens moeten de audience ook daadwerkelijk dragen.** Dezelfde decoder verwerkt zowel de token van de browser als de geëxchangede token. Draagt één van beide de audience niet, dan mislukt élk verzoek. Richt daarom eerst de audience mappers in, en zet deze property pas daarna aan.

De [audience mappers](./keycloak-token-exchange.md#stap-2-audience-mappers) zetten `nl-portal-m2m` op beide tokens. Het bovenstaande voorbeeld is daarmee direct bruikbaar.

## Referentieconfiguratie

De docker-compose demo-omgeving in de monorepo bevat een volledig werkend voorbeeld van alle bovenstaande configuratie, ingericht op de standard token exchange. Gebruik deze als referentie bij het inrichten van een eigen Keycloak:

* `docker-compose/docker-compose.yaml` — de Keycloak service. Let op dat hier géén `KC_FEATURES` staat: dat is precies wat de standard token exchange oplevert.
* `docker-compose/imports/keycloak/nlportal-realm.json` — een volledig realm met de twee clients, de `aanvrager` client scope, de audience mappers, de `middel` mapper en testgebruikers (`burger` met BSN attribuut en `authenticationMethod` `digid`, `bedrijf` met KVK attribuut en `authenticationMethod` `eherkenning`).

De bestandsnamen van de realm-imports eindigen op `-realm.json`. Dat is geen vormvoorkeur: Keycloak gebruikt die naamgeving om de importvolgorde te bepalen, en zonder die naamgeving start Keycloak 26 niet op.

**Let op bij een bestaande omgeving:** Keycloak importeert met strategie `IGNORE_EXISTING`. Draaide je de stack al vóórdat het realm op de standard token exchange werd omgezet, dan blijft het oude realm in de database van de `nl-portal-keycloak-database` container staan en wordt de nieuwe versie **niet** geïmporteerd. De backend verwacht dan standard token exchange terwijl het realm nog op de legacy variant staat, met als resultaat `Standard token exchange is not enabled for the requested client` bij elke login.

Het oude realm verdwijnt pas als je het volume van de `nl-portal-keycloak-database` container weggooit. Het eenvoudigste commando daarvoor doet meteen iets ruimers:

```
docker compose down -v
```

Draai dit vanuit de map `docker-compose/`. **Let op:** `-v` verwijdert de volumes van de **hele** demo-stack, dus ook de data van Open Zaak, OpenKlant, OpenProduct en Open Object. Alle imports draaien daarna opnieuw en je bent zelf aangemaakte testdata kwijt. Voor een demo-omgeving is dat precies de bedoeling; wil je alleen Keycloak resetten, verwijder dan gericht het volume van `nl-portal-keycloak-database`.

## Overige flows

Naast de burger flow en de generieke gebruikersflow ondersteunt de NL Portal ook bedrijven (KVK / eHerkenning) en machtigingsflows (`machtigen`, `bewindvoering`). Deze flows zijn niet in deze documentatie uitgewerkt. Raadpleeg hiervoor de broncode (de `common-ground-authentication`-module onder `backend/` en de frontend-app onder `frontend/packages/app` in de monorepo) of neem contact op via [Community en support](../development/productmanagement/community-en-support.md).
