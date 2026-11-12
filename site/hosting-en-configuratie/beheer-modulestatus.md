# Beheerpoort: modulestatus

Het beheerendpoint `GET /actuator/modulestatus` toont per module en API of de koppeling werkt, de reden en de laatste fout. De publieke `/api/public/frontend` toont dit bewust niet.

## Wat het toont

De status is passief: hij komt uit de laatste echte request-uitkomst, er is geen eigen polling. Aan of uit komt uit `nl-portal.config.<module>.enabled`; alle modules uit die configuratie staan erin, ook als ze niet publiek zijn. Per module:

| Veld | Betekenis |
|---|---|
| `status` | `UP`, `DEGRADED` (API onbereikbaar), `NOT_CONFIGURED` (configuratie ontbreekt), `UNKNOWN` (aangezet, nog geen verkeer) of `DISABLED` (uitgezet in de configuratie, geen storing) |
| `reden` | `UNREACHABLE` of `NOT_CONFIGURED`; leeg bij `UP` |
| `apis[]` | per API: `bereikbaar`, `laatstGecontroleerd`, `laatsteFout` (oorzaakcode en tijdstip) |

Er staan nooit URL's, foutmeldingen, stacktraces of geheimen in. Een herstelde API wordt weer `bereikbaar`; de laatste fout blijft zichtbaar.

## Beveiliging

- **Netwerkscheiding:** zet beheer op een eigen poort met `MANAGEMENT_SERVER_PORT` (bijvoorbeeld `8091`) en publiceer die poort niet via de publieke ingress. Zonder deze variabele draait het endpoint op de gewone poort en rust de beveiliging alleen op authenticatie.
- **Authenticatie:** HTTP Basic met een beheerrol, los van de JWT van gebruikers. Zonder ingestelde gebruiker is het endpoint dicht (altijd 401).

| Omgevingsvariabele | Betekenis |
|---|---|
| `MANAGEMENT_SERVER_PORT` | Eigen beheerpoort (optioneel, aanbevolen) |
| `NLPORTAL_BEHEER_GEBRUIKERSNAAM` | Gebruikersnaam |
| `NLPORTAL_BEHEER_WACHTWOORDHASH` | Wachtwoordhash met prefix, bijvoorbeeld `{bcrypt}$2a$10$...` |

Gebruik in productie een bcrypt-hash en lever die via een secret, nooit via een bestand in de repository. `{noop}wachtwoord` is alleen voor lokaal gebruik; de applicatie waarschuwt in de log als je dat buiten lokaal gebruikt.

Een hash maak je zo (met `htpasswd` uit apache2-utils; zet `{bcrypt}` ervoor):

```shell
htpasswd -bnBC 10 "" 'kies-een-sterk-wachtwoord' | tr -d ':\n' | sed 's/$2y/$2a/'
```

In een Compose-bestand schrijf je elke `$` in de hash als `$$`; in een `env_file` staat de hash letterlijk. In Kubernetes gebruik je een `Secret` als omgevingsvariabele.

## Wat er niet is

- **Geen throttling in de applicatie.** Een teller per pod helpt niet bij meerdere pods. De primaire maatregel is netwerkscheiding: publiceer de beheerpoort niet via de publieke ingress en beperk toegang met een netwerkpolicy. Throttling hoort bij de ingress. Mislukte aanmeldingen komen als waarschuwing in de log, zonder gebruikersnaam of wachtwoord.
- **Geen `refresh` of `restart`.** Die actuator-endpoints zijn uitgezet: configuratiewijzigingen gaan via een rolling update (ADR 0016). Alleen `health` is publiek bereikbaar via `/actuator/health`.
- **Een waarschuwing, geen startfout.** Zonder `MANAGEMENT_SERVER_PORT` draait het endpoint op de publieke poort en waarschuwt de applicatie bij het opstarten. Productie moet de variabele zetten; de health-probes blijven op de gewone poort tot je ze verplaatst.

## Docker Compose

`docker-compose/imports/backend.env` zet de beheerpoort op `8091` met de lokale gebruiker en het lokale wachtwoord uit dat bestand (alleen lokaal). Zet die in `BEHEER_GEBRUIKER` en `BEHEER_WACHTWOORD`. Bij `RUN_MODE=local` of `remote` bereik je het endpoint zo:

```shell
curl -u "$BEHEER_GEBRUIKER:$BEHEER_WACHTWOORD" http://localhost:8091/actuator/modulestatus
```
