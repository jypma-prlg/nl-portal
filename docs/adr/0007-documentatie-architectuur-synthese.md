# 7. Documentatie-architectuur: synthese en "waar vind ik wat"

## Status

Geaccepteerd. Zie wayfinder-ticket [#44](https://github.com/jypma-prlg/nl-portal/issues/44) op map
[#37](https://github.com/jypma-prlg/nl-portal/issues/37) — de afsluitende synthese van die hele map.
Bouwt voort op [ADR 0005](0005-build-test-gates.md) en [ADR 0006](0006-unified-docs-site.md).

## Context

Map #37 heeft, verspreid over meerdere wayfinder-tickets, een reeks losse beslissingen genomen over
waar documentatie en interne kennis in dit project wonen:

- ADR 0005: de canonieke bron voor build/test/"done"-commando's — vier bewuste tiers, met één nog te
  schrijven gedeeld document (`docs/workflows/build-and-test-gates.md`) als toekomstige canonieke
  bron.
- ADR 0006: één VitePress-site volgens Diataxis vervangt `docs/` + `documentation/`, met vijf
  secties (Architectuur, Gebruik & inrichting, Hosting & configuratie, Development, en een
  footer-only Compliance-sectie); root-conventiebestanden verhuizen niet mee.
- [Ticket #40](https://github.com/jypma-prlg/nl-portal/issues/40): verouderde Ritense-eigenaarschapsclaims
  bijgewerkt naar pinkroccade.
- [Ticket #41](https://github.com/jypma-prlg/nl-portal/issues/41): de interne compliance-index wordt
  een uitbreiding van `docs/workflows/security.md`'s per-control tabel — en landt daarmee, via de
  content-mapping hieronder, in de footer-only Compliance-sectie, niet als losse Development-pagina.
- [Ticket #43](https://github.com/jypma-prlg/nl-portal/issues/43): de aihero.dev-skillset (27 skills,
  niet de eerder aangenomen 38) wordt volledig gevendoord onder `.claude/skills/`; het
  `improve-codebase-architecture`-gat in AGENTS.md is gedicht.
- [Ticket #54](https://github.com/jypma-prlg/nl-portal/issues/54): de volledige content-mapping
  (bron → sectie → Diataxis-type) voor alle bestaande documentatiebronnen.

Wat ontbrak was een bevestiging dat deze losse beslissingen samen nog steeds kloppen, én een
concrete afspraak over hoe de root-conventiebestanden (die zelf niet verhuizen, ADR 0006 punt 4)
voortaan naar de nieuwe site verwijzen in plaats van content te dupliceren.

## Beslissing

1. **De site-IA van ADR 0006 blijft ongewijzigd.** De content-mapping (ticket #54) is uitgevoerd over
   alle bestaande bronnen (~80 bestanden in `docs/` en `documentation/`) en paste zonder wringen in de
   vijf secties — geen zesde sectie, hernoeming of andere knip is nodig gebleken.
2. **`docs/workflows/build-and-test-gates.md`** (ADR 0005's nog te schrijven gedeelde document) landt
   in **Development › Workflows**, naast de rest van `docs/workflows/*` — zelfde sectie, zelfde
   doelgroep (engineers/agents).
3. **"Waar vind ik wat"-tabel**, bewust abstract (de site is nog niet gescaffold; een concrete URL nu
   vastleggen zou een belofte zijn die de uitvoeringsfase per ongeluk kan breken):

   | Onderwerp | Sectie op de toekomstige site |
   |---|---|
   | Architectuur (arc42, C4/LikeC4, patronen, integraties) | Architectuur |
   | Functionele features, eigen vormgeving | Gebruik & inrichting |
   | Deployment, Keycloak, module-afhankelijkheden, connectiviteit | Hosting & configuratie |
   | SDLC: ADR's, workflows (incl. build/test-gates), GLOSSARY, spec-driven development | Development |
   | Productmanagement-proces, governance, roadmap, contributors-geschiedenis | Development › Productmanagement |
   | Release-notes-archief (1.x.x–4.x.x) | Development › release-notes-archief |
   | SBOM, security-scans, lifecycle-checks | Compliance (footer-only) |

   Tot de site bestaat, verwijzen root-bestanden naar deze tabel (dit ADR) in plaats van naar een
   concrete URL; een latere PR die de site daadwerkelijk scaffoldt vult de URL's in.
4. **Alle zes root-conventiebestanden** (`AGENTS.md`, `CONTRIBUTING.md`, `README.md`, `SECURITY.md`,
   `CODE_OF_CONDUCT.md`, `GETTING_STARTED.md`) krijgen een verwijzing naar dit ADR (en daarmee naar de
   toekomstige site) voor diepgaande content, in plaats van zelf content te dupliceren of zelf te
   verzinnen waar iets staat — conform ADR 0006 punt 4 ("linken ernaar, niet andersom"), nu voor het
   eerst expliciet voor alle zes tegelijk vastgelegd in plaats van per bestand opnieuw te beslissen.

## Consequenties

- Hiermee is de bestemming van wayfinder-map #37 bereikt: er is geen architectuurbeslissing meer te
  nemen over waar documentatie hoort te wonen. Wat overblijft is uitvoering, buiten deze map: de
  VitePress-site daadwerkelijk scaffolden, de content migreren volgens ticket #54's mapping-tabel, en
  `docs/`/`documentation/` daarna verwijderen.
- GLOSSARY.md's links naar `documentation/fundamentals/architectuur/patronen/{verzoek,externe-taak}.md`
  moeten worden bijgewerkt zodra die content daadwerkelijk migreert (padwijziging, geen
  termwijziging — onderdeel van de uitvoeringsfase, niet van dit ADR).
- `documentation/CONTRIBUTORS.MD` verhuist naar Development › Productmanagement in plaats van te
  vervallen (correctie op een aanname in ticket #54's oorspronkelijke ticketbeschrijving: er bestaat
  geen root-duplicaat van dit bestand).
