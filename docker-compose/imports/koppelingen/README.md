# Koppelingen

Deze map wordt read-only gemount op `/etc/nl-portal/koppelingen` en door Spring Boot geïmporteerd als
[configtree](https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.files.configtree)
(`SPRING_CONFIG_IMPORT=optional:configtree:...`). Het pad onder deze map is de propertynaam, de
bestandsinhoud de waarde, zodat endpoint en geheimen als losse bestanden zonder env-bestand of
versiebeheer worden aangeleverd:

```
nl-portal/koppelingen/<naam>/endpoint        ->  nl-portal.koppelingen.<naam>.endpoint
nl-portal/koppelingen/<naam>/client-secret   ->  nl-portal.koppelingen.<naam>.client-secret
```

Kopieer `endpoint.example` in `nl-portal/koppelingen/voorbeeld/` naar `endpoint` en pas het aan. Echte
bestanden zijn gitignored. De propertynamen per koppeling volgen uit
[#393](https://github.com/pinkroccade/nl-portal/issues/393); tot die er zijn leest niets deze waarden.
