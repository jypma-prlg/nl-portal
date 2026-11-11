# NL-Portal 4.0.0

NL-Portal is samengevoegd tot een monorepo met een enkele, gedeelde versie voor de libraries en de app. Deze release-notes bundelen alle wijzigingen sinds 3.0.x.

## Nieuwe Functionaliteit

* **Monorepo met uniforme versionering.** Backend-libraries, frontend-libraries en de app zijn samengevoegd in één repository (`nl-portal/nl-portal`) met een gedeeld versienummer. App-images worden vanuit de monorepo gepubliceerd op hetzelfde versienummer als de libraries. De docker-compose-stack schakelt met `RUN_MODE` tussen `remote`, `local` en `sources`; zie `GETTING_STARTED.md`.
* **Frontend-features vanuit de backend.** De frontend haalt feature-toggles en -eigenschappen op via `GET /api/public/frontend` (`nl-portal.frontend.features.*`, env `NLPORTAL_FRONTED_FEATURES_*`) in plaats van via `window.*`. Hiermee zijn onder andere de onderhoudsmelding op de overzichtspagina, de contactmomenten op de zaakdetailpagina en het aantal zaken in het overzichtsvoorbeeld configureerbaar. Zie de [Deployment guide](../../../../hosting-en-configuratie/deployment-guide.md#frontend-features).
* **Snelle links in de mobiele weergave.** Frontend-`MenuItems` kunnen nu ook worden getoond in de nieuwe sectie **Quick Links** op de overzichtspagina. Zo kunnen gebruikers in de mobiele weergave sneller naar belangrijke onderdelen navigeren.
* **Uniforme documentafhandeling.** Besluiten-, openproduct- en productdocumenten worden nu op dezelfde manier getoond en gedownload als zaak- en berichtdocumenten. Zie [Document filtering](../../../../hosting-en-configuratie/document-filtering.md).
* Den Haag-componenten bijgewerkt naar de laatste versie.
* Er is een optie toegevoegd om het zoeken naar klantcontact(en) te beperken op basis van de referentie van het gekoppelde object. Hiermee kunnen implementaties het zoeken naar contacten beperken tot contacten die zijn gekoppeld aan een OpenZaak-zaak, een OpenProduct-product of beide.
* Voor monitoring tools is er een ping endpoint beschikbaar gekomen `GET /api/public/ping`, response is platte tekst. Dit endpoint is standaard uit gezet. Via property kan deze aangezet worden via `nl-portal.support.ping-endpoint.enabled=true` of via .env `NLPORTAL_SUPPORT_PINGENDPOINT_ENABLED=false` 
* Samenvoeging van Objecttype API en Objecten API in Open Object in docker compose en fixtures samengevoegd
* **Ondersteuning voor Keycloak standard token exchange.** Twee clients volstaan; de `KC_FEATURES` vlaggen, de fine-grained token-exchange permission en de aparte token-exchange client zijn niet nodig. De docker-compose demo-omgeving draait hierop. Zie [Standard token exchange](../../../../hosting-en-configuratie/keycloak-token-exchange.md). *De V1/V2-keuze (`token-exchange-version`) is inmiddels verwijderd: alleen standard token exchange wordt ondersteund.* **Draaide je de demo-omgeving al eerder?** Zie [Referentieconfiguratie](../../../../hosting-en-configuratie/keycloak.md#referentieconfiguratie) voor hoe je een oud realm herstelt.

## Bugfixes

* De applicatiebeveiliging is verbeterd.
* Fixed an issue where the toegestane status (in_aanvraag) of a product would break at de-/serialization

## Breaking changes

**Voor iedereen (ook gebruikers van de Docker-images):**
* OpenProductFrequentie enum has changed and support also blank value

* **Images verhuisd naar de monorepo.** De backend- en frontend-images worden nu vanuit `nl-portal/nl-portal` gepubliceerd op een gedeeld versienummer (`ghcr.io/nl-portal/nl-portal-backend` / `-frontend`). Wijs je deployment naar de nieuwe images. Zie migratie-instructies.
* **Frontend feature-configuratie via `window.*` is verwijderd** (`config.js` / `config.template.js`). `window.*` bevat nu alleen nog OIDC- en API-URL-configuratie; alle feature-toggles en -eigenschappen worden nu op de backend gezet. Zie migratie-instructies.

**Alleen voor broncode-ontwikkelaars en maatwerkimplementaties:**

* **Upgrade naar Spring Boot 4.1.0** (Spring Framework 7 / Spring Security 7), Kotlin 2.4.10. Bouwen en uitbreiden vereist Java 21; controleer eigen code, configuratie en dependencies op Spring Boot 4- / Spring Security 7-wijzigingen.
* **Frontend-platform bijgewerkt**: React 19, Vite 8, TypeScript 5.9. Bouwen vereist Node 24 en pnpm 11.
* **Haalcentraal HR: In de graphql response is hoofdvestiging gewijzigd naar vestiging. Dit zorgt ook voor ondersteuning voor als je ingelogd bent voor vestiging en je dus het correct adres krijgt 
* **Modules hernoemd en opgeschoond (#399).** `haalcentraal2` heet nu `haalcentraal-brp` (pakket `nl.nlportal.haalcentraalbrp`, property `nl-portal.config.haalcentraal-brp`, env `NLPORTAL_CONFIG_HAALCENTRAALBRP_*`). `haalcentraal-hr` heet nu `kvk` (pakket `nl.nlportal.kvk`, `HandelsregisterClient` is `KvkClient`, property `nl-portal.config.kvk`, env `NLPORTAL_CONFIG_KVK_*`); de oude sleutels `haalcentraal.hr` en `haalcentraalhr` vervallen. `haalcentraal-brp` hangt niet meer af van `kvk`.


## Deprecations

* **Keycloak legacy token exchange (v1).** Inmiddels verwijderd in dit project: alleen standard token exchange wordt nog ondersteund.

## Removals

* (Ogone) payment has been removed, use instead the Direct payment.
* De query `getGemachtigdeV2` (type `GemachtigdeV2`) is verwijderd, samen met het gemachtigde-deel van `UserContext` in de frontend.
* De publieke GraphQL-ingangen voor partij (`findUserPartij`, `getUserPartij`, `createUserPartij`, `updateUserPartij`) en verificatie (`verificatieConfig`, `createVerificatie`, `verifyVerificatie`) zijn verwijderd. De verificatieservice blijft bestaan voor `createUserDigitaleAdres` en `updateUserDigitaleAdres`.

## Migratie-instructies


**Frontend-features naar de backend.** Verplaats de voorheen via `window.*` gezette features naar `nl-portal.config.features.*` (of de env-vorm `NLPORTAL_CONFIG_FEATURES_*` met relaxed binding). Mapping oude `window.*` var → nieuwe backend-property:

| Oude `window.*` var | Nieuwe backend-property |
|---|---|
| `USE_THEME_API` | `toggles.theme-api-enabled` |
| `MESSAGE_COUNT_ENABLE` | `toggles.message-count-enabled` |
| `MESSAGE_COUNT_POLLING_INTERVAL` | `properties.message-count-polling-interval` |
| `CASES_PARTIAL_SEARCH` | `toggles.cases-partial-search-enabled` |
| `SHOW_INHABITANT_AMOUNT` | `toggles.my-inhabitant-count-enabled` |
| `OPEN_PRODUCTEN` | `toggles.open-product-enabled` |
| `SHOW_CASE_RESULT_EXPLANATION` | `toggles.cases-result-explanation-enabled` |
| `USE_LEGACY_OGONE_PAYMENT` | `toggles.legacy-payment-enabled` |
| `OVERVIEW_INTRO_ENABLED` | `toggles.overview-intro-enabled` |
| `OVERVIEW_MAINTENANCE_ALERT_ENABLED` | `toggles.overview-maintenance-alert-enabled` |
| `OVERVIEW_MAINTENANCE_ALERT_TITLE_NL/EN` | `properties.overview-maintenance-alert-title-nl/en` |
| `OVERVIEW_MAINTENANCE_ALERT_TEXT_NL/EN` | `properties.overview-maintenance-alert-text-nl/en` |
| `OVERVIEW_CURRENT_CASES_PREVIEW_LENGTH` | `properties.overview-current-cases-preview-length` |
| `THEME_CLASS` | `properties.theme-class` |
| BRP/adres-URL's (`ADDRESS_RESEARCH_URL`, etc.) | `properties.my-address-research-url`, etc. |

Verwijderde variabelen zonder vervanging (werden niet meer gebruikt): `OPEN_KLANT_VERSION`, `ACCOUNT_SHOW_NOTIFICATION_SUBSECTION`, `OVERVIEW_SHOW_CURRENT_CASES_PREVIEW`.

**Images bijwerken.** De `-app`-infix in de imagenaam vervalt. Werk de image-referenties in je deployment (docker-compose, Helm-values, Kubernetes-manifests) bij:

| Oud | Nieuw |
|---|---|
| `ghcr.io/nl-portal/nl-portal-app-backend:3.x.x` | `ghcr.io/nl-portal/nl-portal-backend:4.0.0` |
| `ghcr.io/nl-portal/nl-portal-app-frontend:3.x.x` | `ghcr.io/nl-portal/nl-portal-frontend:4.0.0` |

Tags zijn beschikbaar per volledige versie (`4.0.0`), minor (`4.0`) en major (`4`).

**Broncode / maatwerk.** Bouw en draai met Java 21; bouw de frontend met Node 24 en pnpm 11. Controleer eigen uitbreidingen op Spring Boot 4- / Spring Security 7-wijzigingen.

Remove the "payment" dependency from the pom.xml or build.gradle file.

## Bekende problemen

Er zijn geen bekende problemen.
