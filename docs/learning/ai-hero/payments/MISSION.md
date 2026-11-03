# Mission: Payments in nl-portal

## Why
Je moet PR's en ontwerpen die de payment-module (Ogone/Worldline direct payment) raken kunnen beoordelen: snappen of een wijziging correct is, wat de risico's zijn, en welke aannames een voorstel maakt over de betaalflow.

## Success looks like
- Je kunt uitleggen welk component (frontend, backend-service, webhook) verantwoordelijk is voor welk deel van een betaling, zonder de code erbij te pakken.
- Je herkent in een PR-diff of een wijziging de betaalstatus-afhandeling (succes/pending/mislukt) correct raakt.
- Je kunt beoordelen of een voorgestelde wijziging de webhook-signatuurvalidatie of de koppeling met de Objecten API (taakstatus) kan breken.
- Je kunt de juiste vragen stellen aan een auteur wiens PR de payment-module wijzigt.

## Constraints
- Geleerd via losse sessies, geen aaneengesloten studietijd.
- Focus op reviewen/beoordelen, niet op zelf bouwen — minder nadruk op exacte syntax, meer op architectuur, contracten en faalscenario's.

## Out of scope
- Diepgaande Worldline/Ogone API-referentie (alle velden, alle endpoints) — alleen wat nodig is om de nl-portal-integratie te snappen.
- Andere betaalproviders of een algemene payments-cursus.
