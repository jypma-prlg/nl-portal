# Inventarisatie: aihero.dev skillset en de `improve-codebase-architecture`-naamsmismatch

Onderzoek voor [issue #42](https://github.com/jypma-prlg/nl-portal/issues/42). Onderliggende bron is de
GitHub-repo achter de "Skills For Real Engineers" set van Matt Pocock / aihero.dev:
[github.com/mattpocock/skills](https://github.com/mattpocock/skills). De marketingpagina's op
[aihero.dev](https://www.aihero.dev/) en [aihero.dev/skills](https://www.aihero.dev/skills) verwijzen naar
deze repo maar hosten de skill-catalogus niet zelf; de repo's `README.md` en `CHANGELOG.md` zijn de
primaire bron hieronder (opgehaald via `raw.githubusercontent.com`), aangevuld met de repo's git-tree
voor de volledige bestandslijst.

## 1. Is `improve-codebase-architecture` een omdoping van `codebase-design`, een apart bestaande skill, of een fout in `docs/adr/README.md`?

**Het is een aparte, los van `codebase-design` bestaande skill — geen omdoping en geen fout.** Beide staan
vandaag naast elkaar in de README-tabel van de repo
([raw.githubusercontent.com/mattpocock/skills/main/README.md](https://raw.githubusercontent.com/mattpocock/skills/main/README.md)):

| Skill | Invocation | Doel |
|---|---|---|
| `codebase-design` | Model | Principes voor het ontwerpen van "deep modules" (vocabulaire: module, interface, depth, seam, adapter) |
| `improve-codebase-architecture` | User | Zoekt "deepening opportunities" in een bestaande codebase |

De `CHANGELOG.md` ([raw.githubusercontent.com/mattpocock/skills/main/CHANGELOG.md](https://raw.githubusercontent.com/mattpocock/skills/main/CHANGELOG.md))
bevestigt dat dit een afhankelijkheidsrelatie is, geen rename, en dat beide sinds de eerste
versie-release van de repo naast elkaar bestaan:

- v1.0.0: "New `codebase-design` skill — the deep-module vocabulary (module, interface, depth, seam, adapter)"
- v1.0.0: "`improve-codebase-architecture` now draws its architecture vocabulary from `/codebase-design`"
- v1.2.0: "Add a YAGNI scoping filter to the `improve-codebase-architecture` skill's Explore step"
  (apart vermeld van `codebase-design`)
- v1.2.3: beide expliciet samen genoemd ("… in `code-review`, `codebase-design` …")
- v1.3.0: "… hand off to `improve-codebase-architecture` (also user-invoked) the same way …"

Onafhankelijk bevestigd door third-party mirrors van dezelfde skill:
[llmbase.ai/skills/mattpocock/improve-codebase-architecture](https://llmbase.ai/skills/mattpocock/improve-codebase-architecture/)
en [hackernoon.com/skills/mattpocock-improve-codebase-architecture](https://hackernoon.com/skills/mattpocock-improve-codebase-architecture).

Samengevat: `codebase-design` is een **model-invoked** referentieskill die alleen het vocabulaire/de
principes levert; `improve-codebase-architecture` is een **user-invoked** skill (`/improve-codebase-architecture`)
die een bestaande codebase scant op "deepening opportunities", daar een HTML-rapport van maakt, en
daarbij expliciet het vocabulaire van `codebase-design` hergebruikt. Hij wijzigt zelf geen code.

Conclusie voor `docs/adr/README.md`: de verwijzing naar `improve-codebase-architecture` verwijst naar een
echte, actuele, van `codebase-design` onderscheiden skill — niet naar een verouderde of foute naam voor
`codebase-design`. Als de tabel in `AGENTS.md` bedoeld is als uitputtende lijst, dan ontbreekt hier simpelweg
een (elfde) skill-naam in die tabel; er is geen naamsfout.

Let op een ander, ongerelateerd rename-gebeurtenis in dezelfde changelog die verwarring kan geven als uit
context gelezen: v1.2.0 vermeldt ook `design-an-interface` → `/codebase-design` (een oudere, aparte skill
genaamd `design-an-interface` is tóén opgegaan in `codebase-design`). Dat staat los van
`improve-codebase-architecture`.

## 2. Volledige, actuele lijst van aihero.dev/mattpocock-skillnamen

Bron: volledige recursieve git-tree van de repo
([api.github.com/repos/mattpocock/skills/git/trees/main?recursive=1](https://api.github.com/repos/mattpocock/skills/git/trees/main?recursive=1))
plus de README-tabel. Totaal 38 skill-directories, verdeeld over vier mappen:

**`skills/engineering/` (20):** ask-matt, code-review, codebase-design, diagnosing-bugs, domain-modeling,
grill-with-docs, implement, implement-spec, **improve-codebase-architecture**, pr, prototype, research,
retro, setup-matt-pocock-skills, tdd, to-spec, to-tickets, triage, wayfinder, wizard

**`skills/productivity/` (7):** grill-me, grilling, handoff, teach, to-questionnaire, wait-what,
**writing-for-agents**

**`skills/misc/` (4):** git-guardrails-claude-code, migrate-to-shoehorn, scaffold-exercises,
setup-pre-commit

**`skills/in-progress/` (7, nog niet stabiel):** chief-of-staff, claude-handoff, loop-me,
setup-ts-deep-modules, writing-beats, writing-fragments, writing-shape

**`skills/deprecated/`:** leeg, afgezien van een `README.md` die de map toelicht.

### Vergelijking met de tabel in dit repo's `AGENTS.md`

`AGENTS.md` noemt exact deze tien namen: `grilling`, `domain-modeling`, `tdd`, `codebase-design`,
`diagnosing-bugs`, `code-review`, `simplify`, `pr`, `research`, `prototype`.

- 9 van de 10 komen letterlijk voor in de mattpocock/skills-repo (alle behalve `simplify`).
- **`simplify` komt niet voor in de mattpocock/skills-repo** (gecontroleerd in de volledige recursieve
  tree én in `CHANGELOG.md` — geen enkele match). Deze sessie heeft lokaal wél een skill genaamd
  `simplify` beschikbaar, maar die is dus kennelijk afkomstig uit een andere bron dan aihero.dev/mattpocock,
  niet uit deze skillset.
- **`security-review` komt evenmin voor** in deze repo. Ook deze is lokaal beschikbaar in de huidige
  sessie, maar niet via aihero.dev/mattpocock.
- `writing-for-agents` bestaat wél in de mattpocock-repo (map `productivity/`), en is volgens de
  changelog zelf ooit hernoemd: "Breaking: rename `writing-great-skills` → `writing-for-agents`,
  restructure it …" (v1.2.0).

Bronnen: [github.com/mattpocock/skills](https://github.com/mattpocock/skills),
[README.md](https://raw.githubusercontent.com/mattpocock/skills/main/README.md),
[CHANGELOG.md](https://raw.githubusercontent.com/mattpocock/skills/main/CHANGELOG.md),
[volledige git-tree](https://api.github.com/repos/mattpocock/skills/git/trees/main?recursive=1).

## 3. Gedocumenteerde installatie-/distributiemethode (globaal vs. vendoren in de repo)

De README ([raw.githubusercontent.com/mattpocock/skills/main/README.md](https://raw.githubusercontent.com/mattpocock/skills/main/README.md))
documenteert meerdere installatiepaden, met als onderscheid vooral auto-update-gedrag — niet expliciet
geframed als "~/.claude/skills vs. .claude/skills" (die letterlijke paden komen niet in de repo voor):

- **Claude Code plugin (managed, niet gevendord):**
  `claude plugin install mattpocock-skills@claude-plugins-official` — "Updates itself by default."
  Dit is de meest "globale"/beheerde installatie: de skill-inhoud wordt niet als losse, bewerkbare
  bestanden in het project gekopieerd.
- **Codex plugin:** `codex plugin marketplace add mattpocock/skills` gevolgd door
  `codex plugin add mattpocock-skills@mattpocock` — "Updates itself at startup."
- **GitHub Copilot:** `copilot plugin marketplace add mattpocock/skills` gevolgd door
  `copilot plugin install mattpocock-skills@mattpocock`, plus een handmatige marketplace-entry in
  `~/.copilot/settings.json` voor auto-updates.
- **Gemini CLI:** `gemini skills install --path skills/engineering` (en apart nogmaals voor
  `skills/productivity`) — "manual updates", d.w.z. commando's opnieuw draaien om te updaten.
- **Overige agents / bewerkbare lokale kopie:** `npx skills@latest add mattpocock/skills -a <agent>`.
  De README stelt expliciet: **"skills.sh copies editable files into your project, and you update them by
  hand."** Updaten via `npx skills@latest update`, of `add` opnieuw draaien om nieuwe skills op te halen.
  Dit is de vendored/repo-lokale optie — bestanden komen in het consumerende project terecht, bewerkbaar,
  niet automatisch gesynchroniseerd.

De README waarschuwt expliciet tegen het combineren van methodes: **"Pick one per agent, because
installing both gives you every skill twice."** Na elke installatie wordt geadviseerd eenmalig
`/setup-matt-pocock-skills` per repo te draaien om de issue tracker, triage-labels en docs-layout te
configureren — wat impliceert dat de auteur per-repo configuratie verwacht, ongeacht welke
installatiemethode gekozen is.

Er werden geen tegenstrijdigheden gevonden tussen bronnen op punt 1–3; de enige (niet-doorslaggevende)
variatie zit in ster-aantallen/skill-aantallen die wisselen tussen third-party blogposts.

## Samenvatting van de conclusie

1. `improve-codebase-architecture` is geen omdoping en geen fout: het is een eigen, user-invoked skill
   die het vocabulaire van de model-invoked skill `codebase-design` hergebruikt. De verwijzing in
   `docs/adr/README.md` is dus correct; wel ontbreekt deze naam in de tabel van `AGENTS.md` als die
   tabel uitputtend bedoeld is.
2. De volledige, actuele aihero.dev/mattpocock-skillset bevat 38 skills in vier categorieën (zie boven).
   Van de tien namen in `AGENTS.md` komen er negen letterlijk terug in die set; `simplify` niet.
   `security-review` komt ook niet voor in de mattpocock-repo. `writing-for-agents` bestaat wel.
3. aihero.dev/mattpocock documenteert zowel een beheerde/globale installatie (Claude Code/Codex/Copilot
   plugin-systemen, die zichzelf updaten) als een expliciet vendored alternatief via `npx skills@latest add`
   (bestanden worden editable in het eigen project gekopieerd, handmatig te updaten) — met de instructie om
   niet beide tegelijk te gebruiken.
