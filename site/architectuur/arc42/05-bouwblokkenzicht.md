# 5. Bouwblokkenzicht

De diagrammen staan in [`site/likec4`](../c4-model.md) (`mise run likec4:start` voor een interactieve
versie). Dit hoofdstuk is de tekstuele laag eromheen: waaróm de blokken zo liggen, niet alleen wélke
er zijn.

## 5.1 Niveau 1 — Containers (zie likec4-view `containers`)

| Container | Verantwoordelijkheid | Technologie |
|---|---|---|
| Frontend | Presentatie en interactie voor de klant | React, TypeScript, Vite |
| Backend | GraphQL-contract, orkestratie van ZGW-aanroepen, autorisatie | Kotlin, Spring Boot |
| NL Portal database | Portal-eigen state (niet de brondata) | PostgreSQL |

## 5.2 Niveau 2 — Backend-componenten (zie likec4-view `backendComponents`)

De backend is Gradle-modules per verantwoordelijkheid (`backend/settings.gradle.kts`):

- **Core** — gedeelde domeinbegrippen, configuratie, security-basis. Elke andere module hangt hierop.
- **GraphQL** — het externe contract; roept de domeinmodules aan.
- **Case** — zaakdossier-orkestratie (leunt op de ZGW-integraties).
- **Form** — formulierinzendingen en -definities.
- **ZGW-integraties** — één module per ZGW-bron: `zaken`, `zaken-api`, `besluiten`, `documenten-api`,
  `openklant`, `taak`, `berichten`, `verificatie`, `objectenapi`, `catalogi-api`,
  `common-ground-authentication`. Dit zijn de modules met de hoogste wijzigingsfrequentie (zie
  hoofdstuk 11) en het onderwerp van de sterkste bevindingen uit de architectuurreview.
- **Haal Centraal-integraties** — `haalcentraal2` (BRP), `haalcentraal-hr` (bewoning/volmachten).
- **Payment** — `payment-direct`, betaalprovider-integratie.
- **Product** — productinformatie, gevoed door OpenProduct.
- **Portal authentication** — Keycloak/OIDC-integratie en autorisatieregels.

**Observatie uit de architectuurreview** (zie het meegeleverde HTML-rapport): de ZGW-
integratiemodules delen een vrijwel identieke vorm (eigen HTTP-client, eigen auth-token-injectie,
eigen statuscode-mapping, eigen paginatiewrapper) zonder een gedeelde onderliggende adapter. Dat is
op dit moment een expliciete keuze geweest (zie hoofdstuk 4.3) maar is tegelijk de grootste
gesignaleerde verdiepingskans — zie hoofdstuk 11 en het architectuurreview-rapport, kandidaten 1 en 2.

## 5.3 Niveau 2 — Frontend-componenten (zie likec4-view `frontendComponents`)

pnpm-workspacepakketten (`frontend/pnpm-workspace.yaml`):

- **`@nl-portal/nl-portal-user-interface`** — gedeelde React-componenten en pagina's; verreweg het
  grootste en meest gewijzigde pakket.
- **`@nl-portal/nl-portal-api`** — gegenereerde GraphQL-client + queries/fragments/hooks.
- **`@nl-portal/nl-portal-authentication`** — OIDC-login/sessiebeheer aan SPA-zijde.
- **`@nl-portal/nl-portal-localization`** — vertalingen/locale-afhandeling.

Beide apps (`app`, de shippable productievorm, en `app-dev`, de ontwikkel-sandbox) importeren deze
vier pakketten als `workspace:*`-dependency; zij bevatten zelf geen gedeelde logica.

**Observatie uit de architectuurreview**: binnen `user-interface` ontbreekt een data-fetching-module
tussen pagina's en Apollo's generieke `useQuery` — pagina's praten rechtstreeks met Apollo en
herhalen hun eigen loading/error-afhandeling. Zie het rapport, kandidaten 3-6, voor concrete
bestanden en een voorgestelde interface.

## 5.4 Waarom dit geen los arc42-detail is

Bouwblokken die "vrijwel identiek" zijn maar geen gedeelde abstractie hebben, zijn precies waar
nieuwe functionaliteit het duurst wordt om toe te voegen: een nieuwe ZGW-bron of een nieuw
resource-overzicht betekent vandaag een 20e of 9e kopie van hetzelfde patroon. Dat is de reden om dit
hier te benoemen in plaats van alleen in een los review-document: het raakt direct de kosten van het
werk waarvoor dit arc42-document is geschreven (nieuwe functionaliteit toevoegen).

<!-- BEGIN GENERATED: capabilities -->
_Gegenereerd uit [`docs/capabilities.yaml`](https://github.com/jypma-prlg/nl-portal/blob/main/docs/capabilities.yaml) via `mise run docs:capabilities`. Wijzig de YAML, niet dit blok; CI faalt als dit blok niet actueel is._

## 5.5 Capabilities per module (inventaris 2026-11-04)

Functioneel overzicht van de backend: wat het portaal doet, in welke Gradle-modules dat leeft en via welke
ingangen (GraphQL-operaties, REST-endpoints, event-listeners) het bereikbaar is. Statussen:
`in-ontwikkeling`, `actief`, `twijfel`, `kandidaat-verwijderen`, `deprecated` (met ADR), `verwijderd`.

| Capability | Status | Modules | Ingangen |
|---|---|---|---|
| [Portaalconfiguratie en health](#capability-portaal-configuratie) | `actief` | `backend/core` | 2 |
| [Database-initialisatie (Liquibase)](#capability-database-migratie) | `actief` | `backend/data` | 1 |
| [Zaken inzien](#capability-zaken-inzien) | `actief` | `backend/zgw/zaken`, `backend/zgw/zaken-api`, `backend/zgw/catalogi-api` | 3 |
| [Besluiten inzien](#capability-besluiten-inzien) | `twijfel` | `backend/zgw/besluiten` | 1 |
| [Berichten ontvangen](#capability-berichten-ontvangen) | `actief` | `backend/zgw/berichten`, `backend/zgw/documenten-api`, `backend/zgw/objectenapi` | 4 |
| [Taken afhandelen](#capability-taken-afhandelen) | `actief` | `backend/zgw/taak`, `backend/zgw/documenten-api`, `backend/zgw/objectenapi`, `backend/form` | 5 |
| [Eigen zaken (case-definities)](#capability-eigen-zaken-case) | `verwijderd` | `backend/case` | 0 |
| [Betalen (Ogone direct)](#capability-betalen-ogone-direct) | `actief` | `backend/payment-direct` | 3 |
| [Persoonsgegevens (BRP)](#capability-persoonsgegevens-brp) | `actief` | `backend/haalcentraal2` | 2 |
| [Machtigingen (gemachtigde)](#capability-machtigingen) | `actief` | `backend/haalcentraal2` | 1 |
| [Bedrijfsgegevens (Handelsregister)](#capability-bedrijfsgegevens-hr) | `actief` | `backend/haalcentraal-hr` | 1 |
| [Contactgegevens beheren](#capability-contactgegevens-beheren) | `actief` | `backend/zgw/openklant`, `backend/zgw/verificatie` | 4 |
| [Klantcontacten inzien](#capability-klantcontacten-inzien) | `actief` | `backend/zgw/openklant` | 2 |
| [Klantprofiel (partij) beheren](#capability-partij-beheren) | `kandidaat-verwijderen` | `backend/zgw/openklant` | 4 |
| [Verificatie contactgegevens (los)](#capability-verificatie) | `kandidaat-verwijderen` | `backend/zgw/verificatie` | 3 |
| [Producten inzien](#capability-producten-inzien) | `in-ontwikkeling` | `backend/zgw/openproduct` | 6 |
| [Productthema's](#capability-product-themas) | `in-ontwikkeling` | `backend/zgw/openproduct` | 8 |
| [Productacties en -beslissingen](#capability-productacties) | `in-ontwikkeling` | `backend/zgw/openproduct` | 5 |
| [Productreferentiegegevens](#capability-product-referentiedata) | `twijfel` | `backend/zgw/openproduct` | 12 |

<a id="capability-portaal-configuratie"></a>
### Portaalconfiguratie en health (`portaal-configuratie`)

Levert feature-flags/configuratie aan de frontend en een optionele ping voor monitoring.

- **Status:** `actief`
- **Bewijs:** Frontend haalt /api/public/frontend op bij opstart; ping alleen achter flag nl-portal.support.ping-endpoint.enabled.
- **Modules:** `backend/core`

| Type | Ingang | Frontend gebruikt | Status | Voorwaarde |
|---|---|---|---|---|
| rest | `GET /api/public/frontend` | ja | `actief` |  |
| rest | `GET /api/public/ping` | nee | `actief` |  |

<a id="capability-database-migratie"></a>
### Database-initialisatie (Liquibase) (`database-migratie`)

Voert bij opstart de databasemigraties uit.

- **Status:** `actief`
- **Bewijs:** Infra-listener, nodig voor form-tabellen; geen burgerfunctie.
- **Modules:** `backend/data`

| Type | Ingang | Frontend gebruikt | Status | Voorwaarde |
|---|---|---|---|---|
| event-listener | `LiquibaseInitializationEventListener` | nee | `actief` |  |

<a id="capability-zaken-inzien"></a>
### Zaken inzien (`zaken-inzien`)

Burger/ondernemer ziet zijn lopende en afgeronde zaken met status, documenten en besluiten.

- **Status:** `actief`
- **Bewijs:** getZaken/getZaak en zaakdocument-download gebruikt in CaseDetailsPage; recente commits.
- **Modules:** `backend/zgw/zaken`, `backend/zgw/zaken-api`, `backend/zgw/catalogi-api`

| Type | Ingang | Frontend gebruikt | Status | Voorwaarde |
|---|---|---|---|---|
| graphql-query | `getZaken` | ja | `actief` |  |
| graphql-query | `getZaak` | ja | `actief` |  |
| rest | `GET /api/zakenapi/zaakdocument/{zaakDocumentId}/content` | ja | `actief` |  |

<a id="capability-besluiten-inzien"></a>
### Besluiten inzien (`besluiten-inzien`)

Toont besluiten bij een zaak (via veld Zaak.besluiten) en biedt download van besluitdocumenten.

- **Status:** `twijfel`
- **Bewijs:** Alleen veldresolvers + REST-download; /api/besluiten/... nergens in frontend gevonden, wel actief onderhouden (17 commits).
- **Modules:** `backend/zgw/besluiten`

| Type | Ingang | Frontend gebruikt | Status | Voorwaarde |
|---|---|---|---|---|
| rest | `GET /api/besluiten/{besluitId}/document/{documentId}/content` | nee | `kandidaat-verwijderen` | eerst bevestigen dat besluitdocumenten nergens getoond worden |

<a id="capability-berichten-ontvangen"></a>
### Berichten ontvangen (`berichten-ontvangen`)

Berichtenbox met ongelezen-teller, berichtdetail en downloaden van bijlagen.

- **Status:** `actief`
- **Bewijs:** Alle ingangen gebruikt (MessageDetailsPage, teller in UI).
- **Modules:** `backend/zgw/berichten`, `backend/zgw/documenten-api`, `backend/zgw/objectenapi`

| Type | Ingang | Frontend gebruikt | Status | Voorwaarde |
|---|---|---|---|---|
| graphql-query | `getBerichten` | ja | `actief` |  |
| graphql-query | `getBericht` | ja | `actief` |  |
| graphql-query | `getUnopenedBerichtenCount` | ja | `actief` |  |
| rest | `GET /api/berichten/{berichtId}/document/{documentId}/content` | ja | `actief` |  |

<a id="capability-taken-afhandelen"></a>
### Taken afhandelen (`taken-afhandelen`)

Burger ziet openstaande taken, vult het bijbehorende formulier in (incl. uploads) en dient het in.

- **Status:** `actief`
- **Bewijs:** V2-operaties en upload gebruikt in UI; geen V1-varianten meer aanwezig.
- **Modules:** `backend/zgw/taak`, `backend/zgw/documenten-api`, `backend/zgw/objectenapi`, `backend/form`

| Type | Ingang | Frontend gebruikt | Status | Voorwaarde |
|---|---|---|---|---|
| graphql-query | `getTakenV2` | ja | `actief` |  |
| graphql-query | `getTaakByIdV2` | ja | `actief` |  |
| graphql-query | `getFormDefinitionByTaskId` | ja | `actief` |  |
| graphql-mutation | `submitTaakV2` | ja | `actief` |  |
| rest | `POST /api/taak/{taakId}/document/content` | ja | `actief` |  |

<a id="capability-eigen-zaken-case"></a>
### Eigen zaken (case-definities) (`eigen-zaken-case`)

Portaal-interne zaakafhandeling - case-definities, case-instanties en indienen van formulieren als case.

- **Status:** `verwijderd`
- **Bewijs:** Geen enkele operatie in frontend gebruikt; overlapt met ZGW-zaken; alleen onderhoudscommits (5).
- **Besluit:** site/development/adr/0010-verwijderen-case-module.md (2026-11-04); module verwijderd, zie issue 103 (log-check op externe clients door maintainer).
- **Modules:** `backend/case`

<a id="capability-betalen-ogone-direct"></a>
### Betalen (Ogone direct) (`betalen-ogone-direct`)

Burger betaalt direct via Ogone/Worldline; de PSP meldt de uitkomst via een postsale-webhook.

- **Status:** `actief`
- **Bewijs:** doDirectPayment/getDirectPaymentStatus gebruikt in UI; webhook wordt door PSP aangeroepen (niet door frontend, verwacht).
- **Modules:** `backend/payment-direct`

| Type | Ingang | Frontend gebruikt | Status | Voorwaarde |
|---|---|---|---|---|
| graphql-mutation | `doDirectPayment` | ja | `actief` |  |
| graphql-query | `getDirectPaymentStatus` | ja | `actief` |  |
| rest | `POST /api/public/payment/direct/postsale` | nee | `actief` |  |

<a id="capability-persoonsgegevens-brp"></a>
### Persoonsgegevens (BRP) (`persoonsgegevens-brp`)

Toont de persoonsgegevens van de ingelogde burger uit Haal Centraal BRP.

- **Status:** `actief`
- **Bewijs:** getPersoonV2 gebruikt; getBewonersAantalV2 niet (wel als veld bewonersAantal), 24 commits.
- **Modules:** `backend/haalcentraal2`

| Type | Ingang | Frontend gebruikt | Status | Voorwaarde |
|---|---|---|---|---|
| graphql-query | `getPersoonV2` | ja | `actief` |  |
| graphql-query | `getBewonersAantalV2` | nee | `kandidaat-verwijderen` | dubbel met veld bewonersAantal |

<a id="capability-machtigingen"></a>
### Machtigingen (gemachtigde) (`machtigingen`)

Toont namens wie de gebruiker handelt (gemachtigde persoon of bedrijf).

- **Status:** `actief`
- **Bewijs:** getGemachtigdeV2 gebruikt in UserContext.
- **Modules:** `backend/haalcentraal2`

| Type | Ingang | Frontend gebruikt | Status | Voorwaarde |
|---|---|---|---|---|
| graphql-query | `getGemachtigdeV2` | ja | `actief` |  |

<a id="capability-bedrijfsgegevens-hr"></a>
### Bedrijfsgegevens (Handelsregister) (`bedrijfsgegevens-hr`)

Toont KvK-gegevens van de ingelogde ondernemer.

- **Status:** `actief`
- **Bewijs:** getBedrijf gebruikt; module achter flag haalcentraal.hr.enabled.
- **Modules:** `backend/haalcentraal-hr`

| Type | Ingang | Frontend gebruikt | Status | Voorwaarde |
|---|---|---|---|---|
| graphql-query | `getBedrijf` | ja | `actief` |  |

<a id="capability-contactgegevens-beheren"></a>
### Contactgegevens beheren (`contactgegevens-beheren`)

Burger beheert e-mail/telefoon (digitale adressen) in OpenKlant 2, met verificatiecode.

- **Status:** `actief`
- **Bewijs:** Volledige CRUD gebruikt in EditContactInfoPage; zeer actief (43 commits openklant).
- **Modules:** `backend/zgw/openklant`, `backend/zgw/verificatie`

| Type | Ingang | Frontend gebruikt | Status | Voorwaarde |
|---|---|---|---|---|
| graphql-query | `getUserDigitaleAdressen` | ja | `actief` |  |
| graphql-mutation | `createUserDigitaleAdres` | ja | `actief` |  |
| graphql-mutation | `updateUserDigitaleAdres` | ja | `actief` |  |
| graphql-mutation | `deleteUserDigitaleAdres` | ja | `actief` |  |

<a id="capability-klantcontacten-inzien"></a>
### Klantcontacten inzien (`klantcontacten-inzien`)

Toont eerdere contactmomenten van de burger met de gemeente.

- **Status:** `actief`
- **Bewijs:** getUserKlantContacten gebruikt; detail-operatie getUserKlantContact niet.
- **Modules:** `backend/zgw/openklant`

| Type | Ingang | Frontend gebruikt | Status | Voorwaarde |
|---|---|---|---|---|
| graphql-query | `getUserKlantContacten` | ja | `actief` |  |
| graphql-query | `getUserKlantContact` | nee | `kandidaat-verwijderen` |  |

<a id="capability-partij-beheren"></a>
### Klantprofiel (partij) beheren (`partij-beheren`)

Opvragen en aanmaken/bijwerken van de partij (klantprofiel) van de burger in OpenKlant 2.

- **Status:** `kandidaat-verwijderen`
- **Bewijs:** Geen van de 4 operaties in frontend; publieke GraphQL-ingangen kunnen weg (na check externe clients).
- **Modules:** `backend/zgw/openklant`

| Type | Ingang | Frontend gebruikt | Status | Voorwaarde |
|---|---|---|---|---|
| graphql-query | `findUserPartij` | nee | `kandidaat-verwijderen` |  |
| graphql-query | `getUserPartij` | nee | `kandidaat-verwijderen` |  |
| graphql-mutation | `createUserPartij` | nee | `kandidaat-verwijderen` |  |
| graphql-mutation | `updateUserPartij` | nee | `kandidaat-verwijderen` |  |

<a id="capability-verificatie"></a>
### Verificatie contactgegevens (los) (`verificatie`)

Losse API om een verificatiecode aan te maken/controleren en de verificatieconfig op te vragen.

- **Status:** `kandidaat-verwijderen`
- **Bewijs:** Geen frontend-gebruik; alleen de publieke GraphQL-ingangen gaan weg. De service blijft als interne afhankelijkheid van contactgegevens-beheren (DigitaleAdresMutation).
- **Modules:** `backend/zgw/verificatie`

| Type | Ingang | Frontend gebruikt | Status | Voorwaarde |
|---|---|---|---|---|
| graphql-query | `verificatieConfig` | nee | `kandidaat-verwijderen` |  |
| graphql-mutation | `createVerificatie` | nee | `kandidaat-verwijderen` |  |
| graphql-mutation | `verifyVerificatie` | nee | `kandidaat-verwijderen` |  |

<a id="capability-producten-inzien"></a>
### Producten inzien (`producten-inzien`)

Burger ziet zijn afgenomen producten (OpenProduct) met gekoppelde zaken, taken en documenten.

- **Status:** `in-ontwikkeling`
- **Bewijs:** getOpenProducten/getOpenProduct gebruikt, rest nog niet; 48 commits in 12m.
- **Modules:** `backend/zgw/openproduct`

| Type | Ingang | Frontend gebruikt | Status | Voorwaarde |
|---|---|---|---|---|
| graphql-query | `getOpenProducten` | ja | `in-ontwikkeling` |  |
| graphql-query | `getOpenProduct` | ja | `in-ontwikkeling` |  |
| graphql-query | `getOpenProductenByThema` | nee | `in-ontwikkeling` |  |
| graphql-query | `getOpenProductTypes` | nee | `in-ontwikkeling` |  |
| graphql-query | `getOpenProductType` | nee | `in-ontwikkeling` |  |
| rest | `GET /api/product/{productId}/document/{documentId}/content` | nee | `kandidaat-verwijderen` |  |

<a id="capability-product-themas"></a>
### Productthema's (`product-themas`)

Navigatie over producten per (hoofd)thema met gekoppelde zaken en taken.

- **Status:** `in-ontwikkeling`
- **Bewijs:** 3 van 8 operaties gebruikt (ThemeOverviewPage/AppContext); actieve ontwikkeling.
- **Modules:** `backend/zgw/openproduct`

| Type | Ingang | Frontend gebruikt | Status | Voorwaarde |
|---|---|---|---|---|
| graphql-query | `getOpenProductThemas` | nee | `in-ontwikkeling` |  |
| graphql-query | `getOpenProductHoofdThemas` | nee | `in-ontwikkeling` |  |
| graphql-query | `getOpenProductHoofdThemasByProducten` | ja | `in-ontwikkeling` |  |
| graphql-query | `getOpenProductThemasHierarchy` | nee | `in-ontwikkeling` |  |
| graphql-query | `getOpenProductThemaHierarchy` | nee | `in-ontwikkeling` |  |
| graphql-query | `getOpenProductThema` | nee | `in-ontwikkeling` |  |
| graphql-query | `getOpenProductThemaZaken` | ja | `in-ontwikkeling` |  |
| graphql-query | `getOpenProductThemaTaken` | ja | `in-ontwikkeling` |  |

<a id="capability-productacties"></a>
### Productacties en -beslissingen (`productacties`)

Acties op een product (bijv. verlengen) met DMN-beslissing en prefill van formulieren.

- **Status:** `in-ontwikkeling`
- **Bewijs:** Alleen getOpenProductActiePrefill gebruikt (ProductDecisionList); overige acties nog niet aangesloten.
- **Modules:** `backend/zgw/openproduct`

| Type | Ingang | Frontend gebruikt | Status | Voorwaarde |
|---|---|---|---|---|
| graphql-query | `getOpenProductActies` | nee | `in-ontwikkeling` |  |
| graphql-query | `getOpenProductActie` | nee | `in-ontwikkeling` |  |
| graphql-query | `getOpenProductActieDecision` | nee | `in-ontwikkeling` |  |
| graphql-query | `getOpenProductActiePrefill` | ja | `in-ontwikkeling` |  |
| graphql-mutation | `updateProduct` | nee | `in-ontwikkeling` |  |

<a id="capability-product-referentiedata"></a>
### Productreferentiegegevens (`product-referentiedata`)

Opvragen van bestanden, contacten, links, locaties, organisaties en prijzen uit OpenProduct.

- **Status:** `twijfel`
- **Bewijs:** 12 lijst/detail-operaties, geen enkele in frontend gebruikt; lijkt 1-op-1 API-doorgifte zonder consument.
- **Actie:** Herbeoordelen zodra de OpenProduct-frontend (producten-inzien, product-themas, productacties) af is.
- **Modules:** `backend/zgw/openproduct`

| Type | Ingang | Frontend gebruikt | Status | Voorwaarde |
|---|---|---|---|---|
| graphql-query | `getOpenProductBestanden` | nee | `twijfel` |  |
| graphql-query | `getOpenProductBestand` | nee | `twijfel` |  |
| graphql-query | `getOpenProductContacten` | nee | `twijfel` |  |
| graphql-query | `getOpenProductContact` | nee | `twijfel` |  |
| graphql-query | `getOpenProductLinks` | nee | `twijfel` |  |
| graphql-query | `getOpenProductLink` | nee | `twijfel` |  |
| graphql-query | `getOpenProductLocaties` | nee | `twijfel` |  |
| graphql-query | `getOpenProductLocatie` | nee | `twijfel` |  |
| graphql-query | `getOpenProductOrganisaties` | nee | `twijfel` |  |
| graphql-query | `getOpenProductOrganisatie` | nee | `twijfel` |  |
| graphql-query | `getOpenProductPrijzen` | nee | `twijfel` |  |
| graphql-query | `getOpenProductPrijs` | nee | `twijfel` |  |

### Modules zonder eigen ingang

| Module | Rol |
|---|---|
| `backend/graphql` | library (GraphQL-config, scalars, exceptionhandler) |
| `backend/portal-authentication` | infra (authenticatie-basis) |
| `backend/zgw/common-ground-authentication` | infra (Keycloak/OIDC) |
| `backend/zgw/common-ground-authentication-test` | test-library |
| `backend/zgw/idtoken-authentication` | library (ZGW JWT naar API's) |
| `backend/zgw/objectenapi` | library (client Objecten API) |
| `backend/zgw/catalogi-api` | library (client Catalogi API) |
| `backend/zgw/zaken-api` | library (client Zaken API) |
| `backend/zgw/documenten-api` | library (client Documenten API + ClamAV; deprecated virusscan-API) |
| `backend/app` | assembly (runnable) |
| `backend/app-dev` | assembly (dev-runnable) |
| `backend/architecture-test` | test (ArchUnit) |
| `backend/gradle/license-report` | build-tooling |
| `backend/product` | verwijderd 2026-10-22; alleen lokale build/-map over (mag weg) |
<!-- END GENERATED: capabilities -->
