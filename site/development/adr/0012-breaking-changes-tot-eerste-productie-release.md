# 12. Breaking changes toegestaan tot de eerste productie-release (1.0.0)

## Status

Geaccepteerd

## Context

NL Portal is afgesplitst van de originele repository, die tot en met versie 4.1.0 releaste. Dit
project draait nog bij geen enkele organisatie in productie. Technische schuld (deprecated velden,
legacy token-exchange, deprecated API's, zie #106) kan daardoor nu zonder migratiepad of
overgangsperiode worden opgeruimd. Zodra er een productie-uitrol is, kost elke breuk een gemeente
werk, en is hard verwijderen niet meer verdedigbaar.

Het versienummer 4.1.0 (erfenis van upstream) zegt "stabiele major" en past daarom niet bij die
werkelijkheid. Een nieuw lezer vraagt zich af waarom er hier wel hard verwijderd is en later niet meer.

## Beslissing

1. De eerste productie-release is **1.0.0**. Alle `0.x`-versies zijn experimenteel: elke wijziging
   mag breken, zonder deprecation-periode, en dat hoeft niet in de changelog aangekondigd te worden.
2. Vanaf **1.0.0** geldt SemVer volledig: een breuk vraagt een major-versie, een voorafgaande
   deprecation-periode en een vermelding als breaking change in `CHANGELOG.md`
   (zie [ADR 0004](./0004-changelog-release-proces.md)).
3. De versie wordt teruggezet van 4.1.0 naar **0.1.0**. De codebasis is gebaseerd op upstream 4.1.0;
   die herkomst staat in de README.
4. Deze ADR is het enige moment waarop hard verwijderen zonder overgang is toegestaan. De
   opruimtickets onder #106 gebruiken dit venster en worden vóór 1.0.0 afgerond.

## Gevolgen

- Versienummers van deze repo en de originele repo lopen vanaf nu uiteen; een tag zoals `v0` of
  `release/0.x` verwijst altijd naar deze repo.
- Container-images en `release/<major>.x`-branches gebruiken major `0` tot 1.0.0.
- Tools die versies vergelijken (Renovate, Dependabot) zien een lagere versie dan 4.1.0. Er zijn nog
  geen consumenten, dus dit heeft nu geen gevolgen.
- Het moment van de eerste productie-release moet bewust gekozen worden; daarna is dit venster
  gesloten en gaan breaking changes door het normale proces.
