# 9. Architectuurbeslissingen

Individuele beslissingen staan als Architecture Decision Records (ADR's) in
[`docs/adr`](../adr), niet in dit arc42-document — zo kan een beslissing worden toegevoegd zonder dit
hele hoofdstuk te herschrijven, en kan naar één beslissing specifiek gelinkt worden.

De eerste ADR, [`0001-record-architecture-decisions.md`](../adr/0001-record-architecture-decisions.md),
legt vast *dat* en *hoe* beslissingen worden vastgelegd. Op het moment van schrijven zijn er nog geen
inhoudelijke ADR's — de bestaande architecturale keuzes (hoofdstuk 4) zijn impliciet in de code
aanwezig, maar nooit als expliciete, afgewogen beslissing gedocumenteerd.

## Kandidaten voor een eerste inhoudelijke ADR

Deze volgen direct uit de architectuurreview (deliverable 1) en uit de grilling-stap die daarop kan
volgen (zie de `improve-codebase-architecture`-skill):

1. **"Eén Gradle-module per ZGW-bron, zonder gedeelde HTTP-adapter"** — vastleggen als bewuste
   keuze, óf vervangen door de in de review voorgestelde gedeelde adapter (kandidaat 1 in het
   review-rapport). Beide uitkomsten verdienen een ADR: vasthouden aan de huidige vorm is dan een
   expliciete, beargumenteerde keuze in plaats van "het is nu eenmaal zo gegroeid".
2. **Keuze van test-dubbel-strategie voor ZGW-clients** (in-memory fake vs. tegen een lokale
   docker-compose-instantie testen) — relevant zodra kandidaat 1 wordt opgepakt.
