# Beheerpoort: modulestatus

Het beheerendpoint `GET /actuator/modulestatus` toont per module en API of de koppeling werkt, de reden en de laatste fout. De publieke `/api/public/frontend` toont dit bewust niet.

## Wat het toont

De status is passief: hij komt uit de laatste echte request-uitkomst, er is geen eigen polling. Per module:

| Veld | Betekenis |
|---|---|
| `status` | `UP`, `DEGRADED` (API onbereikbaar) of `NOT_CONFIGURED` (configuratie ontbreekt) |
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

Gebruik in productie een bcrypt-hash en lever die via een secret. `{noop}wachtwoord` is alleen voor lokaal gebruik.

## Docker Compose

`docker-compose/imports/backend.env` zet de beheerpoort op `8091` met gebruiker `beheer` en wachtwoord `beheer` (alleen lokaal). Bij `RUN_MODE=local` of `remote` bereik je het endpoint zo:

```shell
curl -u beheer:beheer http://localhost:8091/actuator/modulestatus
```
