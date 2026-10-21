# 11. Risico's en technische schulden

## 11.1 Uit de architectuurreview (deliverable 1, 2026-10-20)

Zie het bijgeleverde HTML-rapport voor de volledige onderbouwing per kandidaat (bestanden, voor/na,
deletion test). Samengevat, geordend op impact voor **nieuwe functionaliteit toevoegen** (de reden
waarom deze review is uitgevoerd):

| Risico/schuld | Impact op nieuwe functionaliteit | Kandidaat in review |
|---|---|---|
| Geen gedeelde HTTP-adapter voor ZGW-clients (auth-token-injectie en statuscode-mapping 10+ keer gedupliceerd; bij nader onderzoek bleken het zelfs 4 verschillende, deels ontbrekende foutafhandelingen) | Een nieuwe ZGW-integratie kopieert bestaande bugs mee; geen unit-testbaarheid zonder live API | #1 — **in uitvoering, zie [ADR 0002](../adr/0002-zgw-client-consolidation.md)** |
| 4 byte-identieke, los van elkaar gedefinieerde `ResultPage<T>`-klassen (`besluiten`, `objectenapi`, `catalogi-api`, `documenten-api`) | Een paginatiebug wordt los per module gefixt | gevonden tijdens het grillen van #1 — **in uitvoering, zie [ADR 0002](../adr/0002-zgw-client-consolidation.md)** |
| 20 bijna-identieke `*Page`-wrapperklassen (backend paginatie) | Een nieuwe resource-lijst voegt kopie #21 toe tenzij bewust vermeden | #2 |
| Geen gedeeld GraphQL-fragment voor de zaak-samenvatting (7x gedupliceerd) | Een veldwijziging aan "wat een zaak toont" moet 7x worden doorgevoerd | #3 |
| Geen data-fetching-hook-laag tussen pagina's en Apollo | Een nieuwe pagina herhaalt loading/error-boilerplate in plaats van een hook te hergebruiken | #4 |
| 8 bijna-identieke `*List`-componenten | Een nieuw resource-overzicht voegt kopie #9 toe | #5 |
| Parallelle, index-gekoppelde kolom-arrays (met een **al aangetroffen drift-bug**: een kolom mist in één kopie) | Nieuwe producttypes erven het risico op stille inconsistentie | #6 |
| 560-regelige `AccountPage` zonder tests, met zware branching | Een wijziging aan accountweergave is risicovol om te testen/reviewen | #7 |
| `ThemeMutatePage`/`ThemeHistoryPage` identiek op 3 regels na | Laag risico, snelle opruiming | #8 |

## 11.2 Overige, niet uit de codereview afkomstige risico's

- **Twee onderhouden major-releaselijnen tegelijk** (`CONTRIBUTING.md`): een architectuurwijziging
  die moeilijk cherry-pickbaar is naar de vorige major, vertraagt security-/bugfix-propagatie. Geen
  concreet incident gevonden, maar het is een structureel risico dat meeweegt bij elke grote
  herstructurering (zie hoofdstuk 9 en [ADR 0002](../adr/0002-zgw-client-consolidation.md)).
- **SBOM-generatie is backend-only en release-only** (zie hoofdstuk 8.6): een kwetsbaarheid die
  alleen in een snapshot-/dev-afhankelijkheid zit, of alleen in de frontend, is niet zichtbaar in het
  huidige SBOM-proces. Wordt uitgewerkt in deliverable 5.
- **Geen BIO-toetsing aangetroffen** in de repository (zie hoofdstuk 2.3) — open vraag, geen
  vastgesteld risico, maar wel een follow-up waard gezien de verwerking van BRP-gegevens.
- **Geen productie-topologiedocument in déze repository** (zie hoofdstuk 7.4) — bemoeilijkt het
  inschatten van operationele risico's (schaalbaarheid, failover) vanuit dit document alleen.

## 11.3 Wat hier bewust niet in staat

Risico's die specifiek zijn voor een individuele dependency-versie (bijv. "library X heeft CVE Y")
horen in de Dependency-Check-/Renovate-dashboards, niet hier — dit hoofdstuk beschrijft structurele
risico's, geen puntoplossingen die door bestaande tooling al worden gesignaleerd.
