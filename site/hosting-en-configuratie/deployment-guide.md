---
description: >-
  Met deze stappen moet het mogelijk worden om de NL Portal app images te
  deployen naar je gewenste omgeving, mocht dat Azure, AWS of een Kubernetes
  cluster zijn.
---

# Deployment guide

De NL Portal images (`ghcr.io/pinkroccade/nl-portal-backend` en `ghcr.io/pinkroccade/nl-portal-frontend`) worden volledig geconfigureerd via environment variabelen. De complete, versie-specifieke referentie met inline documentatie vind je in de monorepo, in de bestanden `docker-compose/imports/backend.env` en `docker-compose/imports/frontend.env`. Raadpleeg altijd het bestand dat hoort bij de release die je deployt.

De backend variabelen volgen de Spring Boot relaxed binding naamconventie: de property `nl-portal.config.zakenapi.properties.url` wordt de environment variabele `NLPORTAL_CONFIG_ZAKENAPI_PROPERTIES_URL`.

**Let op bij properties met koppeltekens in meerdere delen.** Relaxed binding accepteert twee schrijfwijzen, maar je moet er consequent één aanhouden: vervang *elk* koppelteken door een underscore, óf laat *elk* koppelteken weg. Een mengvorm wordt **stilzwijgend genegeerd** — geen foutmelding, geen waarschuwing, de property houdt gewoon zijn standaardwaarde. Voor `nl-portal.support.ping-endpoint.enabled`:

| Schrijfwijze | Environment variabele | Werkt |
| ------------ | --------------------- | ----- |
| elk koppelteken wordt `_` | `NL_PORTAL_SUPPORT_PING_ENDPOINT_ENABLED` | ja |
| elk koppelteken weggelaten | `NLPORTAL_SUPPORT_PINGENDPOINT_ENABLED` | ja |
| mengvorm | `NLPORTAL_SUPPORT_PING_ENDPOINT_ENABLED` | **nee** |

Bij properties waarvan alleen het voorste deel een koppelteken heeft — zoals `nl-portal.authentication.keycloak.resource` — vallen beide schrijfwijzen samen en speelt dit niet. Gebruik bij voorkeur de variabelen van de app image (zoals `KEYCLOAK_CLIENT_ID`), dan loop je hier niet tegenaan.

### Deployen met Helm (Kubernetes)

Dit verwees eerder naar officiële Helm charts in Ritense's `helm-charts` repository, met charts voor
de NL Portal backend, frontend en het Configuratiepaneel (backend en frontend). Pinkroccade's eigen
equivalent is nog niet ingericht; zie [epic #191](https://github.com/pinkroccade/nl-portal/issues/191).

### Backend: module-configuratie

De functionele modules van de backend (onder andere Zaken API, Catalogi API, Documenten APIs, Open Object, OpenKlant 2, HaalCentraal BRP (`haalcentraal-brp`) en Handelsregister (`kvk`, als omgevingsvariabele `NLPORTAL_CONFIG_KVK_ENABLED`), Berichten, Taak, Prefill, DMN, OpenProduct, betalingen en virusscan) worden per module aangezet met een enable-vlag. Alle modules staan standaard uit.

Elke module heeft daarnaast eigen properties die met hetzelfde patroon worden gezet: `NLPORTAL_CONFIG_<MODULE>_PROPERTIES_<PROPERTY>`. Bijvoorbeeld voor de Zaken API:

```shell
NLPORTAL_CONFIG_ZAKENAPI_ENABLED=true
NLPORTAL_CONFIG_ZAKENAPI_PROPERTIES_URL=https://openzaak.example.com
NLPORTAL_CONFIG_ZAKENAPI_PROPERTIES_CLIENTID=nl-portal
NLPORTAL_CONFIG_ZAKENAPI_PROPERTIES_SECRET=<secret>
```

**Let op:** de zaken-functionaliteit vereist dat de Zaken API, Catalogi API én Open Object modules alle drie enabled zijn.

**Ontbrekende configuratie:** een module controleert zijn verplichte instellingen (URL, `clientId`, `secret` of token) pas bij het eerste gebruik, niet bij het opstarten. Zet je een module aan zonder die instellingen, dan start de backend gewoon op en faalt de eerste aanroep van die module met een foutmelding als `Module 'zakenapi' is not configured: setting 'url' is missing`. Controleer na het aanzetten van een module dus altijd dat een aanroep slaagt; een goede opstart bewijst nog niet dat de configuratie klopt.

Alle module properties, inclusief de optionele, zijn met inline documentatie te vinden in `docker-compose/imports/backend.env` in de monorepo.

### Backend: CORS

De backend staat standaard geen cross-origin verzoeken toe. Zonder configuratie stuurt hij geen CORS-headers (`Access-Control-Allow-Origin`) mee, waardoor de browser cross-origin antwoorden blokkeert. Dat is voldoende als de frontend en de backend via dezelfde host worden bediend, bijvoorbeeld achter een reverse proxy die beide onder één origin publiceert.

Draait de frontend op een andere origin, dan sta je die expliciet toe per omgeving via `nl-portal.security.cors`:

```properties
NLPORTAL_SECURITY_CORS_0_PATH=/**
NLPORTAL_SECURITY_CORS_0_CONFIG_ALLOWEDORIGINS=https://portal.example.nl
NLPORTAL_SECURITY_CORS_0_CONFIG_ALLOWEDMETHODS=GET,POST,OPTIONS
NLPORTAL_SECURITY_CORS_0_CONFIG_ALLOWEDHEADERS=*
```

Meerdere origins zet je komma-gescheiden in `ALLOWEDORIGINS`. Gebruik `*` voor origins alleen voor lokale ontwikkeling.

**ASVS:** de aanpak volgt het streefniveau uit [security](../development/workflows/security.md) (ASVS Level 2): een deployment vertrouwt standaard geen enkele vreemde origin en origins staan alleen aan als je ze expliciet instelt. Een toetsing per ASVS-eis is nog niet uitgevoerd.

### Frontend features

De frontend haalt zijn feature-toggles en -eigenschappen op via `GET /api/public/frontend`, gevuld vanuit `nl-portal.frontend.properties` en de feature flags op de backend. Eigenschappen volgen de relaxed-binding naamconventie.

- Toggles: alle toggles (`messageCountEnabled`, `overviewIntroEnabled`, ...) komen uit feature flags `ops.frontend.*` (zie [Feature flags](#feature-flags) en de [feature flags-referentie](./feature-flags.md)); `nl-portal.frontend.toggles.*` bestaat niet meer.
- `nl-portal.frontend.properties.*` — waarden, zoals `messageCountPollingInterval`, `overviewCurrentCasesPreviewLength`, `overviewCurrentTasksPreviewLength`, `themeClass`, de `overviewMaintenanceAlert{Title,Text}{Nl,En}` teksten en de BRP/adres-URL's.

De onderhoudsmelding op de overzichtspagina zet je aan met de flag `ops.frontend.overview-maintenance-alert` en de teksten:

```shell
NLPORTAL_FRONTEND_PROPERTIES_OVERVIEWMAINTENANCEALERTTITLENL=Onderhoud
NLPORTAL_FRONTEND_PROPERTIES_OVERVIEWMAINTENANCEALERTTEXTNL=Deze omgeving is tijdelijk in onderhoud.
```

### Feature flags

NL Portal stuurt gedrag aan met feature flags, geëvalueerd via de open standaard
[OpenFeature](https://openfeature.dev). **Je hoeft niets in te richten om te starten**: zonder aparte
instelling gebruikt NL Portal een meegeleverd flagbestand, en de standaardwaarden daarin zijn bedoeld
als veilige startpositie. Je wijzigt flags door een eigen flagbestand aan te wijzen.

**Eigen flagbestand gebruiken**

| Property | Omgevingsvariabele | Betekenis |
|---|---|---|
| `nl-portal.feature-flags.file` | `NLPORTAL_FEATUREFLAGS_FILE` | Pad naar je eigen flagbestand. Zonder deze instelling wordt het meegeleverde bestand gebruikt. |
| `nl-portal.feature-flags.poll-interval-ms` | `NLPORTAL_FEATUREFLAGS_POLLINTERVALMS` | Hoe vaak het bestand op wijzigingen wordt gecontroleerd (standaard 5000). Reken hier niet op: een flagwijziging rol je uit met een rolling update. |

Het bestand heeft het [flagd-formaat](https://flagd.dev/reference/flag-definitions/). Het meegeleverde
bestand is het uitgangspunt: kopieer het uit de broncode van NL Portal
(`backend/core/src/main/resources/feature-flags/default.flagd.json`) en pas het aan. Een flag aanzetten:

```json
{
  "$schema": "https://flagd.dev/schema/v0/flags.json",
  "flags": {
    "ops.frontend.message-count": {
      "state": "ENABLED",
      "variants": { "on": true, "off": false },
      "defaultVariant": "on"
    }
  }
}
```

Op Kubernetes mount je het bestand uit een ConfigMap en zet je `NLPORTAL_FEATUREFLAGS_FILE` op het
gemounte pad. Een flag die ontbreekt in je bestand valt terug op de standaardwaarde in de code, en een
flag voor toegang (permission) valt bij twijfel altijd terug op uit. Een wijziging van het flagbestand rol je uit met een
rolling update (zet een checksum van de ConfigMap als annotatie op de Deployment, zodat pods herstarten);
NL Portal belooft geen live herladen.

**Evaluatiecontext en targeting**

Elke flag wordt per request geëvalueerd met een evaluatiecontext. Daarmee kun je in je flagbestand
[targetingregels](https://flagd.dev/reference/flag-definitions/#targeting-rules) schrijven op deze
attributen:

| Attribuut | Inhoud |
|---|---|
| `tenant` | De tenant van deze installatie (`nl-portal.feature-flags.context.tenant`). |
| `environment` | De omgeving (`nl-portal.feature-flags.context.environment`); ontbreekt als die niet is ingesteld. |
| `userType` | Soort ingelogde gebruiker: `bsn`, `kvk` of `uid`; ontbreekt bij een anoniem request. |
| `targetingKey` | Opaque gebruikers-id: een HMAC-SHA256 van het subject van de identity provider met de geheime sleutel van de tenant. Ontbreekt zonder sleutel of zonder ingelogde gebruiker. |

De context bevat nooit persoonsgegevens zoals BSN, KVK-nummer of naam.

| Property | Omgevingsvariabele | Betekenis |
|---|---|---|
| `nl-portal.feature-flags.context.tenant` | `NLPORTAL_FEATUREFLAGS_CONTEXT_TENANT` | Naam van de tenant (standaard `default`). |
| `nl-portal.feature-flags.context.environment` | `NLPORTAL_FEATUREFLAGS_CONTEXT_ENVIRONMENT` | Naam van de omgeving, bijvoorbeeld `test` of `productie`. |
| `nl-portal.feature-flags.context.user-id-secret` | `NLPORTAL_FEATUREFLAGS_CONTEXT_USERIDSECRET` | Geheime sleutel voor het opaque gebruikers-id. Zet deze alleen via een secret, nooit in versiebeheer of in een flag-systeem. |

Een flag die alleen voor één tenant aan staat:

```json
"ops.frontend.message-count": {
  "state": "ENABLED",
  "variants": { "on": true, "off": false },
  "defaultVariant": "off",
  "targeting": { "if": [{ "==": [{ "var": "tenant" }, "gemeente-a"] }, "on", "off"] }
}
```

**Beschikbare flags**

Per flag naam, type, standaard en effect staan in de [feature flags-referentie](./feature-flags.md), 

Een andere OpenFeature-provider (bijvoorbeeld een flag-server) sluit je aan door in een eigen
Spring-configuratie een bean van het type `FeatureProvider` te registreren; die vervangt dan het
flagbestand.

Zie [Eigen vormgeving](../gebruik-en-inrichting/eigen-vormgeving.md) voor `themeClass` en theming.

Zie de [Keycloak configuratie](./keycloak.md) pagina voor het instellen van de bijbehorende clients in Keycloak. Zie [Standard token exchange](./keycloak-token-exchange.md) voor de inrichting van de token exchange.
