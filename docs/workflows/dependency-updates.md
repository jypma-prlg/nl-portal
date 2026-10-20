# Workflow: dependencies up-to-date houden

Dit document beschrijft de bestaande, geautomatiseerde aanpak (Renovate) en de aanvullende,
handmatige stappen eromheen. Het vervangt geen tooling — het legt vast wat er al draait en hoe je
er als ontwikkelaar mee werkt.

## 1. Geautomatiseerd: Renovate

`.github/renovate.json` is al geconfigureerd en draait wekelijks (`schedule: "* 18-23 * * 0"`,
zondagavond):

- **Minor/patch/digest-updates** worden gebatcht tot **één PR per week** (`groupName: "all
  non-major dependencies"`) — minimaliseert PR-ruis.
- **Major-updates** zijn uitgeschakeld voor automatische PR's (`matchUpdateTypes: ["major"],
  enabled: false`) — ze verschijnen wel op de **Dependency Dashboard**-issue
  (`dependencyDashboard: true`) zodat je weet dat ze er zijn, maar Renovate opent er geen PR voor.
- **`minimumReleaseAge: "1 day"`** — een nieuw gepubliceerde versie moet minimaal 1 dag oud zijn
  voordat Renovate hem oppakt (beperkt het risico op een kwaadaardige "day-0"-publicatie); eigen
  scopes (`@gemeente-denhaag/*`, `@utrecht/*`) zijn hiervan uitgezonderd omdat dat eigen/vertrouwde
  packages zijn.
- **Lockfile-maintenance** draait wekelijks mee, zodat transitieve dependencies ook zonder
  directe versiewijziging actueel blijven.

Je hoeft hier niets voor te doen — een Renovate-PR volgt de normale reviewflow uit
`CONTRIBUTING.md` (dependency-bumps zijn expliciet uitgezonderd van de issue-first-eis).

## 2. Handmatig: major-updates

Omdat Renovate geen major-PR's opent, is er een periodieke, handmatige check nodig. Gebruik:

```bash
mise run deps:check
```

Dit draait zowel de backend- als de frontend-check (zie hieronder) en print wat er beschikbaar is.
Beoordeel per major-update:

1. Lees de changelog/release notes van de afhankelijkheid.
2. Schat de impact in (breaking API's? Kotlin/Spring Boot-compatibiliteit? React/TypeScript-versie-
   eisen van `@gemeente-denhaag/*`/`@utrecht/*`?).
3. Open zelf een issue + branch (`feature/*`, zie `CONTRIBUTING.md`) als de upgrade de moeite waard
   is; dit volgt de normale issue-first-flow omdat een major-upgrade impact kan hebben die een
   geautomatiseerde PR niet kan beoordelen.
4. Regenereer het SBOM (zie [`docs/workflows/sbom.md`](sbom.md)) als onderdeel van die PR, zodat de
   licentie-/kwetsbaarheidsstatus van de nieuwe versie zichtbaar is vóór merge.

### Backend (`./gradlew dependencyUpdates`)

De `com.github.ben-manes.versions`-plugin is al toegepast (`backend/build.gradle.kts`). Draai:

```bash
cd backend && ./gradlew dependencyUpdates -Drevision=release
```

Rapport komt in `build/dependencyUpdates/report.txt` (en html). Dit toont alleen wát er beschikbaar
is — het werkt de versies niet bij.

### Frontend (`pnpm outdated`)

```bash
cd frontend && pnpm outdated -r
```

`-r` doorzoekt alle workspace-packages. `frontend/pnpm-workspace.yaml` heeft al
`minimumReleaseAgeExclude` voor de eigen scopes, consistent met Renovate's uitzondering hierboven —
houd deze twee lijsten synchroon als er een nieuwe vertrouwde scope bijkomt.

## 3. Licenties

`backend` past al de `com.github.jk1.dependency-license-report`-plugin toe. Draai
`./gradlew generateLicenseReport` (output: `build/reports/dependency-license/index.html`) om te
controleren dat een nieuwe afhankelijkheid geen EUPL-incompatibele licentie meebrengt (zie
`docs/arc42/02-randvoorwaarden.md`, sectie 2.2). Dit draait automatisch mee in de release-CI
(`_stage-be.yml`), maar is niet verplicht bij elke PR — overweeg het expliciet te checken bij een
nieuwe (niet alleen geüpdatete) afhankelijkheid.

## 4. Wat hier bewust niet in staat

Kwetsbaarheden in een specifieke dependency-versie horen bij het beveiligingsproces
([`docs/workflows/security.md`](security.md)), niet hier — dit document gaat over *bijblijven*,
niet over *reageren op een kwetsbaarheid*.
