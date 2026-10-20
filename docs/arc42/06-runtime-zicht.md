# 6. Runtime-zicht

Drie representatieve scenario's. Elk laat zien waarom de backend de enige ZGW-cliënt is (hoofdstuk
4.2) en wat dat betekent voor foutafhandeling.

## 6.1 Burger logt in en bekijkt een zaakdossier

```
Burger → Frontend: opent NL Portal
Frontend → Keycloak: redirect naar login (OIDC)
Burger → Keycloak: authenticeert met DigiD
Keycloak → Frontend: authorization code / tokens
Frontend → Backend: GraphQL-query (zaken), met access token
Backend → Keycloak: valideert token (JWT/OIDC)
Backend → Open Zaak: haalt zaken op (ZGW zaken-API), gefilterd op de geauthenticeerde burger
Backend → Frontend: zaaksamenvattingen (GraphQL-response)
```

Noot: Keycloak federeert DigiD/eHerkenning; NL Portal zelf kent geen eigen identiteiten (zie
hoofdstuk 2 en 3).

## 6.2 Klant dient een document in bij een verzoek

Gebaseerd op [`documentation/fundamentals/architectuur/patronen/verzoek.md`](../../documentation/fundamentals/architectuur/patronen/verzoek.md):

```
Klant → Formulierencomponent (buiten NL Portal): vult verzoek in, voegt document toe
Formulierencomponent → Documenten API (Open Zaak): legt het document vast (zaakinformatieobject)
Formulierencomponent → Open Object: dient het verzoek in, met verwijzing naar het document
Open Object → Open Notificaties: publiceert notificatie
Open Notificaties → Backend (NL Portal): webhook, "er is iets gewijzigd"
Backend → Open Object / Open Zaak: haalt de actuele status op (niet de notificatie-payload zelf vertrouwen)
```

Het indienen zelf verloopt dus **niet** via NL Portal (dat doet het formulierencomponent) — NL
Portal toont wel het resultaat ervan. Dit is een scope-grens die makkelijk te vergeten is bij nieuwe
functionaliteit: "een document uploaden" voelt als een NL Portal-feature, maar hoort architecturaal
bij het formulierencomponent of de documentupload-flow die NL Portal richting Open Zaak/ClamAV
aanbiedt voor zijn éígen upload-schermen (bijv. reactie op een externe taak), niet voor het
oorspronkelijke verzoek.

## 6.3 Document-upload met virusscan (externe taak)

Gebaseerd op [`documentation/fundamentals/architectuur/patronen/externe-taak.md`](../../documentation/fundamentals/architectuur/patronen/externe-taak.md):

```
Ketenpartner-systeem → Open Object / Taak-API: maakt een externe taak aan (bijv. "lever document aan")
Backend → Open Object: haalt openstaande taken op voor de klant
Klant → Frontend: uploadt het gevraagde document
Frontend → Backend: upload (GraphQL mutation / REST, afhankelijk van implementatie)
Backend → ClamAV: scant het document (ICAP)
Backend → Documenten API (Open Zaak): legt het document vast zodra schoon
Backend → Open Object: markeert de taak als afgerond
```

Een virusscan-afwijzing moet een expliciete, voor de klant begrijpelijke fout opleveren (zie
hoofdstuk 8, "generieke foutpagina") in plaats van een generieke 500 — dit is een concreet
testscenario voor hoofdstuk 10.
