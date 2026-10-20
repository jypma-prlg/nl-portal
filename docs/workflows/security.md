# Workflow: security-aanpak

Geïnspireerd op [nullpointer.studio](https://nullpointer.studio/)'s publieke aanpak: een continue
cyclus — **Learn. Break. Defend. Repeat.** — in plaats van een eenmalige audit. Hieronder is die
cyclus vertaald naar concrete, bestaande én nieuwe tooling voor NL Portal. Dit document beschrijft
*werkwijze en aanpak*; het vervangt `SECURITY.md` niet (dat blijft het officiële
kwetsbaarheid-disclosure-beleid).

## De cyclus

### Learn — methodologie boven losse scans

Een scan vertelt je *dat* iets fout is; een methodologie vertelt je *waar te zoeken* en *wat het
betekent*. Voor NL Portal:

- **Controlekader**: [OWASP ASVS](https://owasp.org/www-project-application-security-verification-standard/)
  als checklist. Gezien de verwerking van BRP-gegevens en overheids-identiteit (DigiD/eHerkenning,
  zie `docs/arc42/08-dwarsdoorsnede-concepten.md`) is **ASVS Level 2** een passend streefniveau —
  niet geverifieerd tegen de volledige checklist in deze review, wel als uitgangspunt vastgelegd.
- **Threat modeling**: STRIDE per trust-boundary uit het C4-model. Zie
  [`docs/workflows/threat-model-template.md`](threat-model-template.md) — vul dit in bij elke
  nieuwe externe integratie (sluit aan op `docs/workflows/spec-driven-development.md`).
- **Domeinkennis**: `GLOSSARY.md` en `docs/arc42` geven de context die een generieke scanner niet
  heeft (bijv. dat een "verzoek" via Open Object loopt, niet via NL Portal zelf — zie
  `docs/arc42/06-runtime-zicht.md`).

### Break — waar vandaag al op getest wordt, en de gaten die deze PR dicht

| Control | Scope | Al aanwezig? |
|---|---|---|
| SAST (codekwaliteit + een deel beveiligingsregels) | Backend + frontend | ✅ SonarCloud (`sonarcloud.yml`) |
| Dependency-kwetsbaarheden | **Alleen backend** | ✅ OWASP Dependency-Check (`dependency-check.yml`), ❌ frontend |
| Dependency-kwetsbaarheden | Frontend | **✅ nieuw**: `frontend-vulnerability-scan.yml` (grype tegen het SBOM uit `docs/workflows/sbom.md`) |
| Secret-scanning | Beide | **✅ nieuw**: `secret-scan.yml` (gitleaks, elke push/PR) |
| SBOM | Beide | ✅ backend (release-only, zie `docs/workflows/sbom.md`), **✅ nieuw**: frontend on-demand |
| Dependency-updates | Beide | ✅ Renovate, zie `docs/workflows/dependency-updates.md` |

**Concrete bevinding tijdens het schrijven van dit document** (`mise run security:frontend-vuln-scan`,
2026-10-20): de frontend-dependency-tree bevatte op dat moment een **Critical**-kwetsbaarheid
(`shell-quote`, GHSA-pqg4-j6r4-53mv, fix in 1.11.0) en meerdere High-severity kwetsbaarheden
(`@fastify/busboy`, `@graphql-tools/utils`, `brace-expansion`, ...) — **geen van alle zichtbaar
vóór deze PR**, omdat `dependency-check.yml` uitsluitend `backend` scant. Dit is precies het gat
dat `frontend-vulnerability-scan.yml` dicht. Behandel dit als een directe actie, los van deze
documentatie-PR: draai `mise run deps:check-frontend` en update de getroffen packages.

**Tweede concrete bevinding** (`mise run security:backend-vuln-scan`, 2026-10-20): de backend
bevatte op dat moment een **Critical**-kwetsbaarheid in `bcprov-jdk18on` (Bouncy Castle,
GHSA-9pwp-9qqc-pr26, fix vanaf 1.85) — een cryptografiebibliotheek, dus hoog risico gezien de
DigiD/eHerkenning-context — plus High-severity kwetsbaarheden in `jackson-databind`/`jackson-core`.
Niet aangetroffen in `backend/cve-exclusions.xml` (dus niet bewust onderdrukt) en kennelijk nog niet
gevangen door de wekelijkse `dependency-check.yml`-run. **Behandel dit als een directe actie**,
separaat van deze documentatie-PR.

Lokaal draaien, zonder op CI te wachten:

```bash
mise run security:secrets-scan          # werkt met .gitleaks.toml als allowlist
mise run security:frontend-vuln-scan    # SBOM + grype, fail-on high
mise run security:backend-vuln-scan     # idem voor backend (vult dependency-check.yml aan met CVE-data van ná de laatste Dependency-Check-database-sync)
```

### Defend — hardening die niet in een scan-rapport staat

Niet (allemaal) nieuw toegevoegd in deze PR, maar het expliciete "Defend"-tegenwicht bij "Break":

- **Positive security boven blocklists** (nullpointer.studio's principe, hier vertaald): valideer
  input tegen een expliciet schema/whitelist (bijv. GraphQL's eigen typesysteem) in plaats van
  bekende-slechte-patronen te blokkeren. Niet apart geaudit in deze review — een kandidaat voor een
  volgende Learn-stap.
- **Autorisatie op de seam**: de backend is de enige ZGW-cliënt (`docs/arc42/04-oplossingsstrategie.md`,
  4.2) — dat is zelf al een Defend-maatregel: er is precies één plek waar een autorisatiecontrole
  kan (en moet) staan, niet één per frontend-aanroep.
- **Virusscan vóór opslag** (ClamAV, zie `docs/arc42/06-runtime-zicht.md`, 6.3) — al aanwezig.
- **`.gitleaks.toml`-allowlist**: de dev-fixtures in `docker-compose/imports/` en
  `backend/*/src/test/` zijn bewust toegankelijk (lokale ontwikkelstack, geen productie-secrets) —
  zie het bestand zelf voor de volledige lijst en motivatie.

### Repeat

- Renovate (wekelijks) en de nieuwe `frontend-vulnerability-scan.yml` (wekelijks, zondag 19:00 UTC,
  een uur na `dependency-check.yml`) herhalen de "Break"-stap automatisch.
- `secret-scan.yml` draait op elke push naar `main` en elke PR — geen wekelijkse cadans nodig, de
  kosten zijn verwaarloosbaar (< 2 seconden lokaal gemeten).
- Een nieuwe externe integratie doorloopt opnieuw "Learn" (het threat-model-werkblad) vóórdat hij
  "Break"/"Defend" ingaat — zie `docs/workflows/spec-driven-development.md`.

## Wat hier bewust niet in staat

- **CodeQL**: aanbevolen als aanvulling op SonarCloud voor beveiligings-specifieke SAST-regels,
  maar **niet als workflow-bestand toegevoegd** — GitHub's eigen "Default setup" voor code scanning
  (Settings → Code security → Code scanning) is een repository-instelling, geen bestand, en botst
  met een handmatig toegevoegde CodeQL-workflow als beide tegelijk actief zijn. Schakel dit in via
  de GitHub-UI in plaats van via deze PR.
- **Penetratietesten**: buiten scope van deze documentatie-PR; `SECURITY.md` beschrijft al hoe een
  extern gevonden kwetsbaarheid gemeld wordt.
