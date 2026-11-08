# 17. Vluchtige state toegestaan, persistente state niet

## Status

Geaccepteerd. Verfijnt [ADR 0013](./0013-stateless-backend-zonder-database.md) (#216).

## Context

ADR 0013 noemt de backend "stateless" en sluit daarmee een eigen database uit. Gelezen als "geen enkele
state" is dat te ruim: een BFF-aanpak (de backend houdt de sessie van de ingelogde gebruiker bij) en een
cache vragen om een voorziening zoals Redis. De bedoeling van ADR 0013 was een andere: NL Portal bewaart
geen persistente applicatieconfiguratie of gebruikersdata waarvoor een back-up nodig is.

## Beslissing

1. **Het onderscheid is persistent versus vluchtig** (zie [glossarium](../glossarium.md)), niet
   "state versus geen state". "Stateless" in ADR 0013 betekent: geen persistente state.
2. **Vluchtige state mag**, bijvoorbeeld sessies of een cache in Redis, als aan alle drie voldaan is:
   - de uitval beperkt zich tot vluchtige data (zoals opnieuw moeten inloggen), of de applicatie werkt
     zonder die voorziening gewoon door;
   - er is geen back-up of herstel voor nodig;
   - er worden geen brondata of persoonsgegevens bewaard buiten wat AVG-dataminimalisatie toestaat, en
     elke bewaartermijn is beperkt (TTL).
3. **Redis als sessieopslag is niet optioneel voor wie een BFF kiest**: zonder sessieopslag werkt de BFF
   niet, en horizontaal schalen vraagt een gedeelde opslag. **Redis als cache is wel optioneel**: zonder
   cache is de applicatie trager, niet stuk. Een hostingpartij die een BFF draait, levert dus Redis.
4. **Persistente state blijft uitgesloten**, zoals in ADR 0013: geen eigen database, JPA of Liquibase.
   Een functie die persistente state nodig heeft, vraagt een nieuwe ADR.

## Overwogen alternatieven

- **ADR 0013 inhoudelijk herschrijven:** ADR's worden niet herschreven; de oorspronkelijke beslissing
  (geen database, #142) blijft staan en wordt hier alleen aangescherpt.
- **Sessies in een database:** brengt back-up, herstel en migraties terug, wat ADR 0013 juist vermijdt.
- **Sessie in een versleutelde cookie, zonder Redis:** kan, maar is geen BFF-sessie en bewaart tokens
  aan de clientkant; dat is een andere keuze met eigen beveiligingsafwegingen.

## Gevolgen

- Hostingeisen: een BFF-deployment vereist Redis (of een gelijkwaardige sessieopslag), zonder back-up.
  Redis-uitval betekent opnieuw inloggen, geen dataverlies.
- Nieuwe vluchtige voorzieningen toetsen aan de drie voorwaarden in beslissing 2.
- Het register van normatieve bronnen (#212) kan de AVG-dataminimalisatie en de bewaartermijnen per
  eis traceerbaar maken; dit ADR verwijst daarnaar zodra dat register bestaat.
