# 13. Stateless backend zonder eigen database

## Status

Geaccepteerd en uitgevoerd (#142). Voortbouwend op [ADR 0012](./0012-breaking-changes-tot-eerste-productie-release.md).
Verfijnd door [ADR 0017](./0017-vluchtige-state-toegestaan.md): "stateless" betekent geen *persistente* state;
vluchtige state (sessies, cache) is toegestaan.

## Context

De backend had een eigen PostgreSQL-database met JPA en Liquibase (module `data`). Na het
verwijderen van de `case`-module ([ADR 0010](./0010-verwijderen-case-module.md), #103) bleef alleen
`form` over als gebruiker; ook dat database-pad is verwijderd (#135). Daarmee gebruikt geen enkele module
nog persistentie. NL Portal is een interactielaag (laag 5) zonder eigen brondata: zaken, taken,
berichten en producten blijven in de ZGW-bronnen. Een lezer die ziet dat de database en de
`data`-module ontbreken, vraagt zich af of dat een vergissing is.

## Beslissing

1. De backend is **stateless** (zonder persistente state, zie ADR 0017): er is geen eigen database, geen JPA, geen Liquibase en geen
   Postgres-driver. Module `data` is verwijderd.
2. Een volgende functie die state nodig heeft, kiest bewust opnieuw: eerst bij de bronsystemen
   (ZGW, Open Object) en pas als dat niet kan met een nieuwe ADR die persistentie weer introduceert.
3. Het opruimen valt binnen het venster van ADR 0012: er is geen migratiepad voor bestaande
   `form`- of `case`-tabellen, omdat er nog geen productie-installaties zijn.

## Gevolgen

- Hosting hoeft geen database te leveren; de docker-compose-stack en de backend-env hebben geen
  `DATABASE_*`-variabelen meer. Keycloak en de ZGW-componenten houden hun eigen databases.
- Integratietests starten geen Postgres; alleen `zgw:openklant` start een OpenKlant-container.
- Horizontaal schalen en herstarten zijn eenvoudiger: geen schema-migraties bij opstart.
- Een toekomstige functie met eigen state moet persistentie opnieuw opbouwen (module, migraties,
  hosting-eis). Dat is de prijs van deze keuze.
- Het arc42-model (hoofdstuk 4, 5, 7, 8) en het C4-model beschrijven geen database-container meer.
