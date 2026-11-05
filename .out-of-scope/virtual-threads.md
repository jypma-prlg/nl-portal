# Virtual threads

De backend zet geen virtual threads (Project Loom, Java 21+) in, ook niet voor
aanroepen naar ZGW-API's of andere externe koppelingen.

## Waarom dit buiten scope valt

Virtual threads lossen één probleem op: blokkerende I/O die een dure
platform-thread vasthoudt zolang hij wacht. Dat probleem heeft deze backend
niet.

De backend draait op Spring WebFlux met Kotlin-coroutines. Uitgaande
aanroepen naar ZGW-API's lopen via de reactieve `WebClient` en worden in
`suspend`-functies afgewacht (`awaitBody`, `awaitSingle`). Tijdens het wachten
op een antwoord wordt geen thread vastgehouden: de coroutine wordt
opgeschort en de event loop van Netty gaat door met ander werk. In
productiecode staat geen `.block()`.

```kotlin
// Zo ziet een typische ZGW-aanroep eruit: niet-blokkerend, zonder thread per request.
suspend fun getZaak(url: String): Zaak =
    webClient.get().uri(url).retrieve().awaitBody()
```

Virtual threads toevoegen zou dus geen capaciteit of doorvoer opleveren. Het
zou wel een tweede concurrency-model naast coroutines introduceren, met de
bijbehorende vragen over contextpropagatie (security-context, tracing) en
pinning bij `synchronized`-blokken.

## Wanneer dit opnieuw bekeken wordt

Als de backend overstapt van WebFlux naar de servlet-stack (Spring MVC met
blokkerende `RestClient`), verandert de afweging en is
`spring.threads.virtual.enabled` wél het overwegen waard.

## Eerdere verzoeken

- #119: "Vervolg op Java 25-upgrade: toolchain, taalfeatures en JVM-tuning beoordelen" (punt over virtual threads voor blokkerende aanroepen naar ZGW-API's)
