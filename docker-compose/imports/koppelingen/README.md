# Koppelingen

Deze map wordt read-only gemount op `/etc/nl-portal/koppelingen` in de backend. Per koppeling
(`nl-portal.koppelingen.<naam>`) staat het endpoint en de authenticatie in een eigen bestand;
geheimen staan als losse bestanden in `secrets/` en worden via een `*_FILE`-verwijzing gelezen,
zodat er geen geheim in een env-bestand of in versiebeheer staat.

1. Kopieer `voorbeeld.example.yaml` naar `<naam>.yaml` en vul het endpoint in.
2. Zet het geheim in `secrets/<naam>` (één waarde, geen newline nodig).
3. `docker compose up -d`; de bestanden zijn gitignored.

De applicatie kent het verschil tussen een echte API en een mockservice niet: alleen endpoint en
authenticatie verschillen per omgeving.
