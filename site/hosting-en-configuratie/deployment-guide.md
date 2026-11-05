---
description: >-
  Met deze stappen moet het mogelijk worden om de NL Portal app images te
  deployen naar je gewenste omgeving, mocht dat Azure, AWS of een Kubernetes
  cluster zijn.
---

# Deployment guide

De NL Portal images (`ghcr.io/nl-portal/nl-portal-backend` en `ghcr.io/nl-portal/nl-portal-frontend`) worden volledig geconfigureerd via environment variabelen. De complete, versie-specifieke referentie met inline documentatie vind je in de monorepo, in de bestanden `docker-compose/imports/backend.env` en `docker-compose/imports/frontend.env`. Raadpleeg altijd het bestand dat hoort bij de release die je deployt.

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
equivalent is nog niet ingericht; zie [issue #18](https://github.com/jypma-prlg/nl-portal/issues/18).

### Backend: module-configuratie

De functionele modules van de backend (onder andere Zaken API, Catalogi API, Documenten APIs, Open Object, OpenKlant 2, HaalCentraal BRP (`haalcentraal2`) en Handelsregister (`haalcentraal_hr`), Berichten, Taak, Prefill, DMN, OpenProduct, betalingen en virusscan) worden per module aangezet met een enable-vlag. Alle modules staan standaard uit.

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

### Frontend features

De frontend haalt zijn feature-toggles en -eigenschappen op via `GET /api/public/frontend`, gevuld vanuit `nl-portal.frontend` op de backend. Deze volgen dezelfde relaxed-binding naamconventie, bijvoorbeeld `NLPORTAL_FRONTEND_TOGGLES_OVERVIEWMAINTENANCEALERTENABLED=true`.

- Feature flags: de toggle `messageCountEnabled` komt uit de feature flag `ops.frontend.message-count` (zie [Feature flags](#feature-flags)) en wordt niet meer via `nl-portal.frontend.toggles` ingesteld.
- `nl-portal.frontend.toggles.*` — booleans, zoals `casesPartialSearchEnabled`, `openProductEnabled`, `casesResultExplanationEnabled`, `myInhabitantCountEnabled`, `casesContactMomentsEnabled`, `overviewIntroEnabled`, `overviewMaintenanceAlertEnabled`.
- `nl-portal.frontend.properties.*` — waarden, zoals `messageCountPollingInterval`, `overviewCurrentCasesPreviewLength`, `overviewCurrentTasksPreviewLength`, `themeClass`, de `overviewMaintenanceAlert{Title,Text}{Nl,En}` teksten en de BRP/adres-URL's.

De onderhoudsmelding op de overzichtspagina zet je bijvoorbeeld aan met:

```shell
NLPORTAL_FRONTEND_TOGGLES_OVERVIEWMAINTENANCEALERTENABLED=true
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
| `nl-portal.feature-flags.poll-interval-ms` | `NLPORTAL_FEATUREFLAGS_POLLINTERVALMS` | Hoe vaak het bestand op wijzigingen wordt gecontroleerd (standaard 5000). Een aangepast bestand werkt zonder herstart door. |

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
flag voor toegang (permission) valt bij twijfel altijd terug op uit. Kubernetes werkt een gemounte
ConfigMap bij met een bestandswissel; of NL Portal die wijziging zonder herstart oppikt is nog niet in
een echte clusteromgeving getest, controleer dat in je eigen omgeving.

**Beschikbare flags**

| Flag | Type | Standaard | Effect |
|---|---|---|---|
| `ops.frontend.message-count` | ops | uit | Toont het aantal ongelezen berichten in de frontend (`messageCountEnabled`). |

De overige frontend-toggles en de module-activatie worden stapsgewijs flags (zie
[ADR 0014](../development/adr/0014-feature-flags-via-openfeature.md)); tot die tijd stel je ze in zoals
hierboven beschreven.

Een andere OpenFeature-provider (bijvoorbeeld een flag-server) sluit je aan door in een eigen
Spring-configuratie een bean van het type `FeatureProvider` te registreren; die vervangt dan het
flagbestand.

Zie [Eigen vormgeving](../gebruik-en-inrichting/eigen-vormgeving.md) voor `themeClass` en theming.

Zie de [Keycloak configuratie](./keycloak.md) pagina voor het instellen van de bijbehorende clients in Keycloak. Zie [Standard token exchange](./keycloak-token-exchange.md) voor de inrichting van de token exchange.
