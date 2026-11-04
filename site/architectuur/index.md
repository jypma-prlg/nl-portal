# Wat is NL Portal

### **Wat is het?**

NL Portal is een open source Mijn Omgeving voor gemeentelijke- én andere overheden. Inwoners en bedrijven kunnen hierop een veilige manier inloggen om informatie te vinden die voor hun relevant is.&#x20;

Zo kan je

• Je persoonlijke gegevens en adresgegevens raadplegen;\
• Producten en diensten inzien die je bij de gemeente afneemt,

Op één centrale plek, waar je 24/7 terecht kunt.&#x20;

In deze wat verouderde video van de lancering wordt de achtergrond uitgelegd.

[Video: lancering van NL Portal](https://vimeo.com/545567338)

### **Productvisie**

Het heeft drie doelen, voor drie doelgroepen:

1. Gemak voor burgers en ondernemers.
2. Efficiëntie in de uitvoering voor overheden.
3. Voldoen aan wet- en regelgeving voor beleidsmakers.

**Gemak voor burgers en ondernemers**

We zijn gewend geraakt aan het gemak van online diensten. Een bestelling die vandaag wordt geplaatst en morgen of zelfs vandaag al wordt geleverd, is nu de norm. Burgers en ondernemers verwachten dezelfde efficiëntie van de overheid. NL Portal helpt hierbij door eenvoudig online aanvragen te kunnen indienen, inzicht te hebben in de afhandeling, toegang hebben tot producten en diensten online en snelle levering te ervaren. Kijk naar de&#x20;

**Efficientie in de uitvoering voor overheden**

Overheden hebben een enorme informatiestroom te verwerken. Dat moet sneller, met minder fouten en met minder mensen. Door automatisering kan een deel van de informatiestroom geautomatiseerd verwerkt worden, waardoor tijd vrij wordt gespeeld voor burgers en ondernemers die persoonlijke aandacht verdienen.

NL Portal zorgt voor gestandaardiseerde digitale communicatie met burgers en ondernemers. De verwerking van data wordt daardoor vereenvoudigd. Kwaliteit in = kwaliteit uit. NL Portal volgt de standaarden vanuit het Common Ground project 'Samenwerkende Portalen' - elk backoffice systeem dat deze standaarden volgt kan communiceren via NL Portal.

**Voldoen aan wet- en regelgeving voor beleidsmakers**

De wet- en regelgeving stelt hogere eisen aan digitale diensten van overheidsinstellingen. Toegankelijkheid, beveiliging, omgang met persoonsgegevens, auditing, taalgebruik, data-integriteit, beschikbaarheid en snelheid. BIO, ISO, AVG, WCAG, DigiD Assessment. NL Portal heeft tot doel 'uit de doos' technisch voldoen aan eisen.

NL Portal heeft de volgende uitgangspunten:

* Open standaarden, open source licentie EUPL 1.2;
* Compatible met diverse formulierapplicaties;
* Frontend UI gebaseerd op [NL Design System](https://nldesignsystem.nl/);
* Voldoet aan alle web toegankelijkheidsrichtlijnen (WCAG);
* Onafhankelijk van proces- of zaak management systemen;
* Horizontal schaalbaar.

### **Functionaliteiten**

Op dit moment kent NL Portal de volgende functionaliteiten

* Koppeling met DigiD en eHerkenning
  * DigiD en eHerkenning zijn vertrouwde systeem voor gebruikers, verhoogt de beveiliging door betrouwbare identiteitsverificatie, en biedt gemak doordat veel mensen al een DigiD hebben.
* Bezoekers kunnen nieuwe zaken aanmaken;
  * Inwoners en bedrijven kunnen via dit proces de start van een nieuwe zaak initiëren;
*   Bezoekers kunnen altijd de actuele status van hun zaken zien;
    * Track & trace-service waar inwoners en medewerkers aanvragen kunnen volgen. Deze service is vooral gericht op het bieden van inzicht in gegevens en minder op interactie. 
    ![track-and-trace](./img/track-and-trace.png)
* Bezoekers kunnen Taken krijgen in NL Portal
  * Hiermee kunnen inwoners direct zelf zaken regelen bij de gemeente. Deze service laat acties zien die nog door een inwoner of een medewerker moeten worden uitgevoerd. Inwoners kunnen hier bijvoorbeeld extra gegevens aanleveren voor een lopende zaak en in de toekomst kunnen ze een betaling doen in NL Portal.
* Bezoekers kunnen bestanden downloaden
  * Bezoekers kunnen de documenten die gekoppeld zijn aan hun zaak downloaden. Dit kunnen bijvoorbeeld de besluitdocumenten zijn.&#x20;
*   Bezoekers hebben een profielpagina
    * Binnen NL Portal is onder andere een profielpagina component. Deze profielpagina kan koppelen met HaalCentraal en zo de informatie uit de basisadministratie tonen.
    ![profiel](./img/profiel.png)

## Open source

### Uitgangspunten ontwikkeling NL Portal

* Open standaarden, open source licentie EUPL 1.2;
* Compatible met diverse formulierapplicaties;
* Frontend UI gebaseerd op [NL Design System](https://nldesignsystem.nl/);
* Voldoet aan alle web toegankelijkheidsrichtlijnen (WCAG);
* Onafhankelijk van proces- of zaak management systemen;
* Horizontal schaalbaar.

### Bijdrage leveren

Alle code is publiek beschikbaar in deze GitHub-repository. We zijn trots op ons werk, tonen je graag meer en staan klaar om je op gang te helpen.

### Feature of bugfixes toevoegen

1. Maak een issue aan in de Issues van deze repository. Hiermee willen we zorgen dat alle bugfixes en features afgestemd zijn op elkaar zodat we geen dubbel werk doen
2. Implementeer de feature of bugfix, test deze en maak een PullRequest naar `main`
3. Schrijf de release notes en documentatie
4. Merge de branch als je een akkoord hebt van het Portal team

We streven naar een maandelijkse release, mocht je sneller een fix of feature nodig hebben laat het ons weten.

### Deelnemers

Oorspronkelijk ontwikkeld door:

![deelnemers](./img/denhaag_amsterdam_ritense.jpg)

Sinds deze repository als zelfstandig project verderging, wordt het onderhouden door pinkroccade.

## Impressies

Het UX Design is ontwikkeld door de gemeente Den Haag, op basis van het [NL Design system](https://nldesignsystem.nl/).\
Den Haag beschikt over eigen UX lab, waarin de UX wordt getest met inwoners. Enkele impressies\
van ontwerpen van NL Portal in de _look & feel_ van de gemeente Den Haag.

## Gemeente Den Haag

In het onderstaande overzicht scherm staan alle lopende zaken van deze persoon en de openstaande\
taken die uitgevoerd moeten worden.

![overzichts-pagina](./img/impressies/overzichts-pagina.png)_Overzichtscherm_

Bij de lopende zaken staan de stappen en de status van een specifieke zaak en welke taken hierbij\
open staan.

![lopende-zaken](./img/impressies/lopende-zaken.png)_Lopende zaken_

NL Portal wisselt berichten uit tussen de klant/ burger en de gemeente. Alle openstaande en\
afgehandelde berichten worden verzameld op deze pagina.

![mijn-berichten](./img/impressies/mijn-berichten.png)_Mijn berichten_

Door op een bericht te klikken worden de details van het bericht zichtbaar. Eventuele taken\
zoals betalingen kunnen direct worden uitgevoerd.

![bericht-detail](./img/impressies/bericht-detail.png)_Bericht - detailpagina_

Bij mijn gegevens staan de profielgegegevens van deze persoon en kunnen persoonsgegevens\
gewijzigd worden.

![mijn-gegevens](./img/impressies/mijn-gegevens.png)_Mijn gegevens_

## Gemeente Amsterdam

Op het overzicht scherm staan de openstaande zaken van de betreffende persoon of organisatie

![overzicht-amsterdam](./img/impressies/overzicht-amsterdam.png)_Overzicht_

Op de 'mijn gegevens' pagina staan de profiel gegevens van de persoon of organisatie en kunnen\
de contactgegevens gewijzigd worden.

![mijn-gegevens-amsterdam](./img/impressies/mijn-gegevens-amsterdam.png)_Mijn gegevens_
