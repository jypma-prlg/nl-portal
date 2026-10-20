# NL Portal — C4-model (LikeC4)

Dit is het C4-applicatiearchitectuurmodel van NL Portal, als [LikeC4](https://likec4.dev)
DSL-bestanden (`.c4`). Het beschrijft hetzelfde systeem als
[`documentation/fundamentals/architectuur`](../../documentation/fundamentals/architectuur) en
[`docs/arc42`](../arc42), maar dan als een navigeerbaar, consistent diagrammodel in plaats van
losse afbeeldingen.

## Bestanden

| Bestand             | Inhoud                                                                 |
| -------------------- | ----------------------------------------------------------------------- |
| `specification.c4`  | Toegestane elementsoorten (`actor`, `system`, `container`, `component`). |
| `model.c4`          | C1/C2: actoren, het NL Portal-systeem met zijn containers (frontend, backend, database), en de externe ZGW/Common Ground-systemen waarmee het samenwerkt. |
| `components.c4`     | C3: de interne compositie van de backend- en frontend-container, als `extend`-blokken op `model.c4`. |
| `views.c4`          | De vier views: `index` (C1), `containers` (C2), `backendComponents` en `frontendComponents` (C3). |

LikeC4 leest alle `.c4`-bestanden in deze map en voegt ze samen tot één model; de indeling in
bestanden is alleen leesbaarheid, geen technische grens.

## Gebruiken

Alle commando's lopen via `mise` (zie `mise.toml` in de repository-root):

```bash
mise run likec4:validate   # syntax + semantische checks (ook te draaien in CI)
mise run likec4:start      # lokale preview-server met interactieve diagrammen
mise run likec4:export     # exporteert de 4 views als PNG naar docs/likec4/export
```

`likec4:start` opent een React-gebaseerde verkenner in de browser; geen extra installatie nodig.
`likec4:export` gebruikt Playwright en installeert bij de eerste run een headless Chromium
(`npx playwright install`) als die nog ontbreekt.

## Scope en relatie tot arc42

- **C1 (systeemcontext, view `index`)** komt overeen met arc42-hoofdstuk 3 (Context en scope).
- **C2 (containers, view `containers`)** komt overeen met arc42-hoofdstuk 5 (Bouwblokkenzicht, niveau 1).
- **C3 (componenten, views `backendComponents`/`frontendComponents`)** is de verdieping van
  arc42-hoofdstuk 5, niveau 2, voor respectievelijk de backend- en de frontend-container.

De C3-componentindeling volgt de Gradle-modules (`backend/settings.gradle.kts`) resp.
pnpm-workspacepakketten (`frontend/pnpm-workspace.yaml`) zoals die nu in de code bestaan; het is een
beschrijving van de huidige structuur, geen streefbeeld. Zie de architectuur-review (deliverable 1)
voor voorstellen om die structuur te verdiepen.

## Onderhoud

Werk dit model bij wanneer:

- er een nieuw ZGW-achtig extern systeem bijkomt (nieuwe `system`-declaratie in `model.c4`);
- er een nieuwe backend Gradle-module of frontend workspace-package bijkomt die een eigen
  verantwoordelijkheid heeft (nieuw `component` in `components.c4`);
- een integratie wijzigt van protocol/richting (relaties in `model.c4`/`components.c4`).

Draai na elke wijziging `mise run likec4:validate` voordat je committeert.
