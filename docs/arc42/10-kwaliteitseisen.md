# 10. Kwaliteitseisen

Uitwerking van de kwaliteitsdoelen uit hoofdstuk 1.2 als concrete, (idealiter) toetsbare scenario's.
**Dit zijn voorstellen, geen vastgestelde eisen** — net als hoofdstuk 1.2 zijn ze afgeleid uit de
bestaande code/CI/documentatie, niet uit een stakeholderinterview.

## 10.1 Kwaliteitsboom (voorstel)

| Kwaliteit | Scenario | Huidige borging |
|---|---|---|
| Vertrouwd/beveiligd | Een afhankelijkheid met een bekende CVE wordt binnen één sprint gesignaleerd | OWASP Dependency-Check in CI (`dependency-check.yml`) — reactief, geen SLA vastgelegd |
| Vertrouwd/beveiligd | Een document met malware wordt geweigerd vóór opslag in Open Zaak | ClamAV-integratie (zie hoofdstuk 6.3) — werking niet apart getest in deze review |
| Interoperabiliteit | Een nieuwe ZGW-bron kan worden toegevoegd zonder bestaande modules te wijzigen | Gedeeltelijk: modulaire opzet bestaat, maar zonder gedeelde adapter (zie hoofdstuk 5.2/8.2) kost het toevoegen copy-paste, geen isolatie van bugs |
| Herbruikbaarheid | Een andere gemeente kan draaien zonder NL Portal-code te wijzigen, alleen configuratie | Niet geverifieerd in deze review — vereist een aparte configuratie-audit |
| Onderhoudbaarheid | Een externe bijdrager kan een bugfix indienen die binnen de bestaande PR-flow past | Issue-first workflow, Conventional Commits, squash-merge (`CONTRIBUTING.md`) — organisatorisch goed geborgd |
| Testbaarheid | Een ZGW-clientfout (404/401/500) kan unit-getest worden zonder een live ZGW-API | **Nee, vandaag niet mogelijk** — zie architectuurreview kandidaat 1; dit is de scherpste testbaarheidslacune die deze review vond |
| Testbaarheid | Een pagina's data-fetching-logica (loading/error) kan los van rendering getest worden | **Nee, vandaag niet systematisch** — zie architectuurreview kandidaat 4 |

## 10.2 Hoe dit te onderhouden

Voeg een rij toe zodra een kwaliteitsdoel een concreet, meetbaar scenario krijgt (bijv. een
performance-budget, een beschikbaarheids-SLA). Verwijder een rij niet stilzwijgend als de borging
verandert — noteer in plaats daarvan de nieuwe borging, zodat dit hoofdstuk een geschiedenis van
"waar stonden we" blijft in plaats van alleen een huidige snapshot.
