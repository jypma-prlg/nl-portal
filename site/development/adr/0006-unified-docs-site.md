# 6. Eén gedocumenteerde bron: VitePress-site volgens Diataxis in plaats van docs/ + documentation/

## Status

Geaccepteerd. Zie wayfinder-ticket [#39](https://github.com/jypma-prlg/nl-portal/issues/39) op map
[#37](https://github.com/jypma-prlg/nl-portal/issues/37).

## Context

Dit project heeft vandaag twee losse documentatiebomen: `docs/` (arc42, C4/LikeC4, ADR's,
workflows — doelgroep: engineers/agents) en `documentation/` (een GitBook-export — doelgroep:
klanten/gebruikers/beheerders). Onderzoek voor dit ticket liet zien dat die scheiding niet
houdbaar is:

- `documentation/README.md` noemt "Architectuur" zelf als één van drie hoofdpijlers
  ("Hoe is NL Portal opgebouwd?"), naast Getting Started en Support.
- `documentation/fundamentals/architectuur/` (README, 5-lagenmodel, `integraties/{zaak,
  mijn-gegevens,product}.md`, `patronen/{verzoek,externe-taak}.md`, authentication-en-authorization)
  is stevige, technische architectuurcontent die inhoudelijk overlapt met `docs/arc42`. Twee van
  die `patronen/*.md`-bestanden worden bovendien actief gelinkt vanuit `GLOSSARY.md`.
- `documentation/product-management/governance.md` beschrijft een ouder, afwijkend
  bijdrageproces dat niet meer overeenkomt met het huidige `CONTRIBUTING.md`.
- `documentation/release-notes/` bevat een echt historisch archief (versies 1.x.x–4.x.x) dat
  dateert van vóór ADR 0004's nu-geautomatiseerde `CHANGELOG.md`.
- Compliance-feiten (SBOM, security-scans, lifecycle — zie ticket #41) horen bij weer een ander,
  smaller publiek (CISO/TISO/auditors) dan beide bestaande bomen bedienen.

## Beslissing

1. **Eén geconsolideerde documentatieboom, geen docs/ + documentation/-scheiding meer.** Alle
   content (architectuur, SDLC, compliance, gebruik/inrichting, hosting/configuratie,
   productmanagement-proces) verhuist naar één logisch gestructureerde map, gebouwd en gepubliceerd
   met **VitePress**.
2. **Het [Diataxis](https://diataxis.fr/)-framework** (tutorial / how-to / reference /
   explanation) bepaalt hoe content binnen elke sectie is vormgegeven — niet alleen waar een
   bestand staat, maar welk type pagina het is.
3. **Sectie-indeling (hoofdnavigatie):**
   - **Architectuur** — arc42 is leidend voor de diepgaande content; de homepage bevat een
     vereenvoudigde, platte-taal-uitleg (vergelijkbaar met de huidige
     `documentation/fundamentals/wat-is-nl-portal.md`) die doorlinkt naar de volledige
     arc42-hoofdstukken. Dit is de expliciete maatregel tegen informatieverlies: begrijpelijke taal
     op het eerste niveau, de volledige diepte één klik verder.
   - **Gebruik & inrichting** — klant/gebruiker/functioneel-beheerder-content (bijv. huidige
     `features/*`, `configuratie/eigen-vormgeving.md`).
   - **Hosting & configuratie** — technische installatie/ops-content (bijv. huidige
     `configuratie/{deployment-guide,keycloak,module-dependency-guide,configuration-panel,
     keycloak-token-exchange-*,opzetten-nl-portal}.md`).
   - **Development** — SDLC voor engineers/agents: `docs/workflows/*`, ADR's, GLOSSARY, en een
     **Productmanagement-subsectie** die vooral het *proces* beschrijft (hoe werk wordt
     geprioriteerd/getrackt via GitHub Issues/backlog/kanban — zie ook de nog niet gechartte
     agentic-procesautomatisering-map); de daadwerkelijke roadmap/backlog-*inhoud* blijft in GitHub
     Issues leven, wordt niet gedupliceerd als statische pagina. Het release-notes-archief
     (1.x.x–4.x.x) bevriest hier als historisch materiaal, met één regel die naar het
     geautomatiseerde root-`CHANGELOG.md` (ADR 0004) verwijst voor alles ná de automatisering.
   - **Compliance** — een eigen, aparte sectie (niet een subsectie van Development), maar bewust
     **niet in de hoofdnavigatie**: alleen bereikbaar via het footer-menu, passend bij het smallere
     CISO/TISO/auditor-publiek. Invult ticket #41's canonieke compliance-index.
4. **Root-conventiebestanden verhuizen niet.** `AGENTS.md`, `CONTRIBUTING.md`, `README.md`,
   `SECURITY.md`, `CODE_OF_CONDUCT.md`, `GETTING_STARTED.md` blijven op hun GitHub-conventionele
   plek in de repo-root (GitHub rendert ze daar automatisch; agents/CLI's zoeken ze daar) en linken
   naar de site voor diepgaande content — niet andersom.
5. **`governance.md` wordt herschreven, niet verwijderd**, als een eerlijke "in ontwikkeling"-pagina
   onder Development › Productmanagement: deze fork faciliteert maatwerk voor gemeenten; het
   governance-model staat onder regie van de PinkRoccade product owners, in nauwe afstemming met de
   kwartiermakers/gemeenten die er gebruik van maken. De daadwerkelijke uitwerking van dat
   governance-model zelf is organisatorisch, geen documentatiebeslissing — zie de map's "Not yet
   specified".
6. **De exacte content-mapping** (welk bestaand arc42-hoofdstuk/ADR/configuratie-bestand wordt
   welke Diataxis-pagina in welke sectie) is een eigen, apart ticket — niet hier uitgewerkt.

## Consequenties

- Vervolgwerk, buiten deze map: de VitePress-site daadwerkelijk scaffolden, de content migreren,
  en `docs/`/`documentation/` daarna verwijderen.
- Ticket #41 (interne compliance-index) is nu scherper: het besluit wat er in de
  footer-only Compliance-sectie komt, niet een losse uitbreiding van `docs/workflows/security.md`.
- Ticket #44 (de samenvattende synthese) is nu groter: die bevestigt de uiteindelijke site-IA en
  ADR 0005's "nieuw gedeeld document" (`docs/workflows/build-and-test-gates.md`) landt in de
  Development-sectie van deze site, niet als los bestand.
- Een nieuw ticket (content-mapping) blokkeert #44, net als de overige vijf open beslissingen.
