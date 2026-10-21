---
description: >-
  Met deze stappen moet het mogelijk worden om de NL Portal app images te
  deployen naar je gewenste omgeving, mocht dat Azure, AWS of een Kubernetes
  cluster zijn.
---

# Deployment guide

De NL Portal images (`ghcr.io/nl-portal/nl-portal-backend` en `ghcr.io/nl-portal/nl-portal-frontend`) worden volledig geconfigureerd via environment variabelen. De complete, versie-specifieke referentie met inline documentatie vind je in de monorepo, in de bestanden `docker-compose/imports/backend.env` en `docker-compose/imports/frontend.env`. Raadpleeg altijd het bestand dat hoort bij de release die je deployt.

De backend variabelen volgen de Spring Boot relaxed binding naamconventie: de property `nl-portal.config.zakenapi.properties.url` wordt de environment variabele `NLPORTAL_CONFIG_ZAKENAPI_PROPERTIES_URL`.

**Let op bij properties met koppeltekens in meerdere delen.** Relaxed binding accepteert twee schrijfwijzen, maar je moet er consequent één aanhouden: vervang *elk* koppelteken door een underscore, óf laat *elk* koppelteken weg. Een mengvorm wordt **stilzwijgend genegeerd** — geen foutmelding, geen waarschuwing, de property houdt gewoon zijn standaardwaarde. Voor `nl-portal.authentication.keycloak.token-exchange-version`:

| Schrijfwijze | Environment variabele | Werkt |
| ------------ | --------------------- | ----- |
| elk koppelteken wordt `_` | `NL_PORTAL_AUTHENTICATION_KEYCLOAK_TOKEN_EXCHANGE_VERSION` | ja |
| elk koppelteken weggelaten | `NLPORTAL_AUTHENTICATION_KEYCLOAK_TOKENEXCHANGEVERSION` | ja |
| mengvorm | `NLPORTAL_AUTHENTICATION_KEYCLOAK_TOKEN_EXCHANGE_VERSION` | **nee** |

Bij properties waarvan alleen het voorste deel een koppelteken heeft — zoals `nl-portal.authentication.keycloak.resource` — vallen beide schrijfwijzen samen en speelt dit niet. Gebruik bij voorkeur de variabelen van de app image (zoals `KEYCLOAK_TOKEN_EXCHANGE_VERSION`), dan loop je hier niet tegenaan.

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

Alle module properties, inclusief de optionele, zijn met inline documentatie te vinden in `docker-compose/imports/backend.env` in de monorepo.

### Frontend features

De frontend haalt zijn feature-toggles en -eigenschappen op via `GET /api/public/frontend`, gevuld vanuit `nl-portal.frontend` op de backend. Deze volgen dezelfde relaxed-binding naamconventie, bijvoorbeeld `NLPORTAL_FRONTEND_TOGGLES_OVERVIEWMAINTENANCEALERTENABLED=true`.

- `nl-portal.frontend.toggles.*` — booleans, zoals `messageCountEnabled`, `casesPartialSearchEnabled`, `openProductEnabled`, `casesResultExplanationEnabled`, `myInhabitantCountEnabled`, `casesContactMomentsEnabled`, `overviewIntroEnabled`, `overviewMaintenanceAlertEnabled`.
- `nl-portal.frontend.properties.*` — waarden, zoals `messageCountPollingInterval`, `overviewCurrentCasesPreviewLength`, `overviewCurrentTasksPreviewLength`, `themeClass`, de `overviewMaintenanceAlert{Title,Text}{Nl,En}` teksten en de BRP/adres-URL's.

De onderhoudsmelding op de overzichtspagina zet je bijvoorbeeld aan met:

```shell
NLPORTAL_FRONTEND_TOGGLES_OVERVIEWMAINTENANCEALERTENABLED=true
NLPORTAL_FRONTEND_PROPERTIES_OVERVIEWMAINTENANCEALERTTITLENL=Onderhoud
NLPORTAL_FRONTEND_PROPERTIES_OVERVIEWMAINTENANCEALERTTEXTNL=Deze omgeving is tijdelijk in onderhoud.
```

Zie [Eigen vormgeving](eigen-vormgeving.md) voor `themeClass` en theming.

Zie de [Keycloak configuratie](keycloak.md) pagina voor het instellen van de bijbehorende clients in Keycloak. Kies daar ook de variant van de token exchange: [standard (v2)](keycloak-token-exchange-v2.md) voor nieuwe omgevingen, of [legacy (v1)](keycloak-token-exchange-v1.md) — de standaardwaarde — voor bestaande.
