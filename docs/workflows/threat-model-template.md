# Threat-model werkblad (STRIDE)

Kopieer dit bestand (bijv. naar een PR-beschrijving of een issue) bij het toevoegen van een
nieuwe externe integratie of een nieuwe trust-boundary. Gebruik de trust-boundaries uit
[`docs/likec4`](../likec4) (view `index`, C1) als startpunt — elke pijl in dat diagram is een
boundary die dit werkblad verdient.

## Integratie / wijziging

- **Wat**: _naam van de nieuwe integratie of wijziging_
- **Trust-boundary**: _welke pijl in `docs/likec4` view `index` is dit, of welke nieuwe pijl komt
  erbij?_
- **Data die de boundary oversteekt**: _welke velden/gegevenscategorieën (bijv. BSN, BRP-gegevens,
  documentinhoud)?_

## STRIDE

| Categorie | Vraag | Van toepassing? | Mitigatie |
|---|---|---|---|
| **S**poofing | Kan de andere partij zich voordoen als iemand anders (bijv. een vervalst OIDC-token, een nagemaakte ZGW-bron)? | | |
| **T**ampering | Kan data onderweg of bij de bron worden gemanipuleerd zonder dat NL Portal het merkt? | | |
| **R**epudiation | Kan een actie (bijv. een ingediend verzoek) achteraf worden ontkend bij gebrek aan logging? | | |
| **I**nformation disclosure | Kan deze boundary gevoelige data lekken (aan de verkeerde klant, in een log, in een foutmelding)? | | |
| **D**enial of service | Kan deze integratie NL Portal (of andandersom het externe systeem) laten vastlopen bij misbruik of overbelasting? | | |
| **E**levation of privilege | Kan een klant/ketenpartner iets zien of doen dat niet bij zijn rol hoort via deze boundary? | | |

## Besluit

- **Risiconiveau**: Laag / Midden / Hoog
- **Vervolgstap**: _nieuwe ADR nodig (`docs/adr`)? Extra test? Alleen documenteren in
  `docs/arc42/08-dwarsdoorsnede-concepten.md`?_
