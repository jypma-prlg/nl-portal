# 9. Architectuurbeslissingen

Individuele beslissingen staan als Architecture Decision Records (ADR's) in
[`docs/adr`](../adr), niet in dit arc42-document — zo kan een beslissing worden toegevoegd zonder dit
hele hoofdstuk te herschrijven, en kan naar één beslissing specifiek gelinkt worden.

De eerste ADR, [`0001-record-architecture-decisions.md`](../adr/0001-record-architecture-decisions.md),
legt vast *dat* en *hoe* beslissingen worden vastgelegd.

## Vastgelegde beslissingen

- [`0002-zgw-client-consolidation.md`](../adr/0002-zgw-client-consolidation.md) — **in uitvoering**.
  Vervangt "kandidaat-ADR 1" hieronder: in plaats van één Gradle-module per ZGW-bron met een eigen
  HTTP-client, komt er één gedeelde, diepe `AuthenticatedZgwClient` (module `backend/zgw/zgw-client`)
  met een uniform, RFC 9457-gebaseerd foutafhandelingsbeleid. Zie de bijbehorende
  [spec](../adr/0002-zgw-client-consolidation-spec.md) voor de concrete, nog te zetten
  implementatiestappen (nog niet uitgevoerd op het moment van schrijven).

## Overige kandidaten

Nog niet als ADR vastgelegd:

1. **Keuze van test-dubbel-strategie voor de resterende ZGW-clients** (`openklant`, `taak`,
   `berichten`, `verificatie`, `besluiten`, `openproduct` — buiten de scope van ADR 0002):
   `MockWebServer` is het precedent dat ADR 0002 volgt voor de vier modules die het wél meeneemt;
   relevant zodra die overige modules aan de beurt zijn.
