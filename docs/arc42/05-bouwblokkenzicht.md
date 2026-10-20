# 5. Bouwblokkenzicht

De diagrammen staan in [`docs/likec4`](../likec4) (`mise run likec4:start` voor een interactieve
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
