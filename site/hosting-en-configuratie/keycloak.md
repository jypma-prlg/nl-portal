# Keycloak configuratie

NL Portal gebruikt Keycloak voor de authenticatie van gebruikers. Deze pagina beschrijft alle vereisten om een Keycloak in te richten voor gebruik met de NL Portal: de clients, de token exchange, gebruikersattributen en claims.

Deze documentatie beschrijft de **burger flow** (gebruiker met BSN, ingelogd via DigiD) en de **generieke gebruikersflow** (Keycloak gebruiker zonder BSN of KVK). Voor overige flows, zoals bedrijven (eHerkenning) en machtigingen, verwijzen we naar de broncode of [support](../development/productmanagement/community-en-support.md). Zie [Overige flows](#overige-flows).

## Token exchange

Als je een api call doet naar de backend wordt de token onderschept en wordt er een call naar Keycloak gedaan, waarmee een nieuwe token wordt opgehaald waar de bsn/kvk wél in zit. Deze wordt daarna in de rest van de applicatie gebruikt. De token van de browser bevat nooit een BSN of KVK.

### Twee varianten

Keycloak kent twee varianten van de token exchange. Welke je gebruikt stel je in met één property:

| Variant | `token-exchange-version` | Wanneer |
| ------- | ------------------------ | ------- |
| [Standard token exchange (v2)](./keycloak-token-exchange-v2.md) | `v2` | **Aanbevolen.** Vereist Keycloak 26.2 of hoger. Geen server features nodig, twee clients. |
| [Legacy token exchange (v1)](./keycloak-token-exchange-v1.md) | `v1` (standaard) | Verouderd. Voor bestaande omgevingen en voor Keycloak-versies ouder dan 26.2. Vereist twee verouderde server features en drie clients. |

`v1` blijft in de hele 4.x-lijn de standaardwaarde, zodat bestaande omgevingen bij een upgrade niets hoeven te wijzigen. In **5.0** wordt `v2` de standaard en in een latere release verdwijnt `v1`. Draai je op `v1`, dan meldt de applicatie dat bij het opstarten met een waarschuwing.

De [v2-pagina](./keycloak-token-exchange-v2.md#migratie-van-v1-naar-v2) bevat een migratiepad zonder downtime, inclusief terugrolstap.

Zie de [officiële Keycloak documentatie](https://www.keycloak.org/securing-apps/token-exchange) voor de achterliggende verschillen tussen beide varianten.

## Clients en backend configuratie

Welke clients je nodig hebt verschilt per variant:

| Client (voorbeeldnaam) | Type | v1 | v2 | Doel |
| ---------------------- | ---- | -- | -- | ---- |
| `nl-portal` | public, standard flow | ✓ | ✓ | Login van de frontend (SPA). Draagt de `middel` mapper. |
| `nl-portal-m2m` | confidential, service accounts, met secret | ✓ | ✓ | Voert de token exchange uit namens de backend ("m2m" = machine-to-machine). |
| `nl-portal-token-exchange` | public, geen flows | ✓ | — | Doelclient (audience) van de token exchange. Alleen bij v1 nodig. |

De volledige inrichting per variant staat op de [v2-pagina](./keycloak-token-exchange-v2.md#clients) en de [v1-pagina](./keycloak-token-exchange-v1.md#clients).

De koppeling met de backend verloopt in beide gevallen via dezelfde environment variabelen van de app image (zie ook de [Deployment guide](./deployment-guide.md)):

| Variabele | Betekenis |
| --------- | --------- |
| `JWKS_URI` | Wijst de backend naar het realm waarmee hij tokens valideert: `<keycloak-url>/realms/<realm>/protocol/openid-connect/certs`. Draait je Keycloak onder een pad (zoals de demo-omgeving, met `KC_HTTP_RELATIVE_PATH=/auth`), neem dat pad dan mee: `<keycloak-url>/auth/realms/<realm>/protocol/openid-connect/certs`. |
| `KEYCLOAK_TOKEN_EXCHANGE_VERSION` | `v1` of `v2`. Standaard `v1`. |
| `KEYCLOAK_CLIENT_ID` | Client id van de m2m (backend) client |
| `KEYCLOAK_CLIENT_SECRET` | Secret van de m2m (backend) client |
| `KEYCLOAK_TOKEN_EXCHANGE_AUDIENCE` | Client id van de token-exchange client. Alleen voor `v1`, waar de waarde verplicht is. Laat leeg bij `v2`. |

De redirect-URI van de `nl-portal` (frontend) client moet overeenkomen met de frontend-variabele `OIDC_REDIRECT_URI`. De SPA plaatst zijn OIDC-callback automatisch op dat pad, dus elk pad op je eigen origin werkt; registreer die redirect-URI op de client.

## Gebruikerstypen

De backend bepaalt het type gebruiker op basis van claims in de **geëxchangede** token. Dit geldt voor beide varianten:

1. Claim `aanvrager.bsn` aanwezig → de gebruiker is een **burger**.
2. Claim `aanvrager.kvk` aanwezig → de gebruiker is een **bedrijf** (zie [Overige flows](#overige-flows)).
3. Geen van beide → de gebruiker is een **generieke gebruiker** (zie [Generieke gebruikers](#generieke-gebruikers-sub-flow)).

## Gebruikersattributen: burger flow

Voor de burger flow betekent dit:

* De gebruiker in het portal realm heeft een **user attribute** `bsn` (bijvoorbeeld `999993847`).
* Er staat een protocol mapper (type *User Attribute*) die het user attribute `bsn` mapt naar de claim `aanvrager.bsn` in het access token. Op welke client die mapper hoort verschilt per variant: bij [v2](./keycloak-token-exchange-v2.md#stap-1-client-scope-met-de-aanvrager-mappers) op een client scope van de m2m client, bij [v1](./keycloak-token-exchange-v1.md#clients) op de token-exchange client.
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

Welke waarde je invult verschilt per variant:

* **[v2](./keycloak-token-exchange-v2.md):** de audience mappers uit [stap 2](./keycloak-token-exchange-v2.md#stap-2-audience-mappers) zetten `nl-portal-m2m` op beide tokens. Het bovenstaande voorbeeld is daarmee direct bruikbaar.
* **[v1](./keycloak-token-exchange-v1.md):** de geëxchangede token wordt opgebouwd vanuit de doelclient en draagt daardoor een ándere audience dan de token van de browser. De waarde hierboven klopt dan niet. Controleer eerst de `aud` van beide tokens en voeg waar nodig audience mappers toe, voordat je dit aanzet.

## Referentieconfiguratie

De docker-compose demo-omgeving in de monorepo bevat een volledig werkend voorbeeld van alle bovenstaande configuratie, ingericht op **v2**. Gebruik deze als referentie bij het inrichten van een eigen Keycloak:

* `docker-compose/docker-compose.yaml` — de Keycloak service. Let op dat hier géén `KC_FEATURES` staat: dat is precies wat de v2-variant oplevert.
* `docker-compose/imports/keycloak/nlportal-realm.json` — een volledig realm met de twee clients, de `aanvrager` client scope, de audience mappers, de `middel` mapper en testgebruikers (`burger` met BSN attribuut en `authenticationMethod` `digid`, `bedrijf` met KVK attribuut en `authenticationMethod` `eherkenning`).

De bestandsnamen van de realm-imports eindigen op `-realm.json`. Dat is geen vormvoorkeur: Keycloak gebruikt die naamgeving om de importvolgorde te bepalen, en zonder die naamgeving start Keycloak 26 niet op.

**Let op bij een bestaande omgeving:** Keycloak importeert met strategie `IGNORE_EXISTING`. Draaide je de stack al vóórdat het realm op v2 werd omgezet, dan blijft het oude realm in de database van de `nl-portal-keycloak-database` container staan en wordt de nieuwe versie **niet** geïmporteerd. De backend staat dan op v2 terwijl het realm nog op v1 staat, met als resultaat `Standard token exchange is not enabled for the requested client` bij elke login.

Het oude realm verdwijnt pas als je het volume van de `nl-portal-keycloak-database` container weggooit. Het eenvoudigste commando daarvoor doet meteen iets ruimers:

```
docker compose down -v
```

Draai dit vanuit de map `docker-compose/`. **Let op:** `-v` verwijdert de volumes van de **hele** demo-stack, dus ook de data van Open Zaak, OpenKlant, OpenProduct en Open Object. Alle imports draaien daarna opnieuw en je bent zelf aangemaakte testdata kwijt. Voor een demo-omgeving is dat precies de bedoeling; wil je alleen Keycloak resetten, verwijder dan gericht het volume van `nl-portal-keycloak-database`.

## Overige flows

Naast de burger flow en de generieke gebruikersflow ondersteunt de NL Portal ook bedrijven (KVK / eHerkenning) en machtigingsflows (`machtigen`, `bewindvoering`). Deze flows zijn niet in deze documentatie uitgewerkt. Raadpleeg hiervoor de broncode (de `common-ground-authentication`-module onder `backend/` en de frontend-app onder `frontend/packages/app` in de monorepo) of neem contact op via [Community en support](../development/productmanagement/community-en-support.md).
