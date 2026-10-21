# Načrt in rezultati enotnih testov

## Izbrane enote in pogodbe

| Enota | Zakaj je primerna | Pričakovano delovanje |
| --- | --- | --- |
| `AnalyticsFilters` — `backend/src/main/java/si/um/feri/dotaops/backend/analytics/domain/AnalyticsFilters.java` | Normalizira uporabniško omejitev in preverja časovni razpon. | Omejitev `<= 0` postane privzeta vrednost 10; veljavna pozitivna vrednost ostane nespremenjena do 100; višja vrednost se omeji na 100. Časovni razpon z `from` za `to` je zavrnjen z `BadRequestException`, enaki meji pa sta dovoljeni. Metode `with*` spremenijo samo izbrani filter in ohranijo druge filtre. |
| `RequestRateLimiter` — `backend/src/main/java/si/um/feri/dotaops/backend/common/security/RequestRateLimiter.java` | Vsebuje kvote, ločene števce ter časovna okna, ki vplivajo na zaščito API-ja. | V eni časovni minuti je dovoljenih 10 uvozov na uporabnika in 30 na IP; v petih minutah je dovoljenih 20 poskusov Steam prijave na IP. Zahteva na meji kvote uspe, naslednja pa vrne `RateLimitExceededException`. Uporabniški, IP in Steam števci so ločeni; ob začetku novega okna se števec ponastavi. |
| `AuditJsonSanitizer` — `backend/src/main/java/si/um/feri/dotaops/backend/audit/service/AuditJsonSanitizer.java` | Rekurzivno obdeluje JSON in preprečuje uhajanje poverilnic iz dnevnikov. | Vrne samo izrecno dovoljena vrhnja polja; v objektih in poljih polja občutljiva imena nadomesti z `[REDACTED]`, tudi pri camelCase, vezajih, različnih velikostih črk ter občutljivih priponah. Varne skalarne vrednosti, `null` in gnezdena polja ohranijo obliko. `null` ali prazen vhod vrne `null`; neveljaven JSON in neobjektni JSON vrneta prazno projekcijo. |
| `DotaAccountIdConverter` — `backend/src/main/java/si/um/feri/dotaops/backend/opendota/service/DotaAccountIdConverter.java` | Pretvarja SteamID64 v Dota account ID32 in preverja podprto območje. | Najnižji veljavni SteamID64 se pretvori v 0; neštevilske ali napačno dolge vrednosti ter ID-ji zunaj območja se zavrnejo. |
| `ClientIpAddressResolver` — `backend/src/main/java/si/um/feri/dotaops/backend/common/security/ClientIpAddressResolver.java` | Izbere naslov odjemalca iz posredovanih HTTP-glav ali iz zahteve. | Prednost ima prvi naslov `X-Forwarded-For`, nato `X-Real-IP`, nato oddaljeni naslov; izbrana vrednost se obreže. |
| `CorsProperties` — `backend/src/main/java/si/um/feri/dotaops/backend/config/properties/CorsProperties.java` | Normalizira konfiguracijo dovoljenih CORS-izvorov. | Prazne vrednosti se odstranijo; če ni uporabnega izvora, se uporabita omejena lokalna privzeta izvora. |

## Testi

Uporabljen je obstoječi JUnit 5 z AssertJ. Dodani scenariji so v:

- `backend/src/test/java/si/um/feri/dotaops/backend/analytics/domain/AnalyticsFiltersTest.java`
- `backend/src/test/java/si/um/feri/dotaops/backend/common/security/RequestRateLimiterTest.java`
- `backend/src/test/java/si/um/feri/dotaops/backend/audit/service/AuditJsonSanitizerTest.java`
- `backend/src/test/java/si/um/feri/dotaops/backend/opendota/service/DotaAccountIdConverterTest.java`
- `backend/src/test/java/si/um/feri/dotaops/backend/common/security/ClientIpAddressResolverTest.java`
- `backend/src/test/java/si/um/feri/dotaops/backend/config/properties/CorsPropertiesTest.java`

Testi preverjajo dejanske vrednosti, enakost celotnih zapisov, pomembne meje kvot, izjemne primere, ohranjanje oblike JSON, prioriteto IP-glav, mejo pretvorbe SteamID64 in rezervne CORS-nastavitve. Za časovne preizkuse `RequestRateLimiter` uporablja nastavljivo uro, zato so testi deterministični in ne čakajo. `ClientIpAddressResolver` prejme Springov `MockHttpServletRequest`; druge izbrane enote ne potrebujejo mockov, ker nimajo zunanjih odvisnosti.

Pregled je pokazal vrzeli v obstoječih testih: manjkali so zgornja meja 100 in enaki časovni meji, ponastavitev omejevalnika na meji okna, izolacija števcev, projekcija dovoljenih polj, napačen JSON ter občutljiva imena s priponami. Dodatni testi pokrijejo tudi odprta časovna obdobja, normalizacijo IP-ja za omejevanje, nespremenljivost očiščenega JSON-a, spodnjo mejo pretvorbe SteamID64, prednost `X-Real-IP` in rezervne CORS-nastavitve. Ni bilo treba spreminjati produkcijske logike.

## Nadzorovani mutacijski preizkusi

Vsaka napaka je bila vnesena ločeno, ustrezen ciljni test pa je vrnil neuspešno izhodno kodo Maven. Po vsakem preizkusu je bila izvirna datoteka takoj obnovljena.

| Enota | Začasna napaka | Test, ki jo je zaznal |
| --- | --- | --- |
| `AnalyticsFilters` | Zgornja omejitev znižana s 100 na 99. | `limitDefaultsToTenAndCapsAtOneHundred` je pričakoval 100 in dobil 99. |
| `AnalyticsFilters` | Primerjava `from.isAfter(to)` obrnjena. | `rejectsTimeRangeWhereFromIsAfterTo` ni dobil pričakovane izjeme; test veljavnega obdobja je zaznal tudi napačno zavrnitev. |
| `RequestRateLimiter` | Primerjava `count > maxRequests` zamenjana z `count >= maxRequests`. | Testi kvot so odpovedali, ker je bila zavrnjena že dovoljena zahteva na meji kvote. |
| `RequestRateLimiter` | Enominutno okno za uvoz podaljšano na dve minuti. | `matchImportCountersResetAtTheNextOneMinuteWindow` je odpovedal, ker se števec ni ponastavil. |
| `AuditJsonSanitizer` | Izklopljena redakcija občutljivih polj. | Rekurzivni test, test normaliziranih imen in test gnezdenih polj so dobili nerazkrite skrivnosti. |
| `AuditJsonSanitizer` | Vrhnja projekcija JSON-a vrnjena brez zaščite pred spreminjanjem. | `returnedProjectionAndNestedObjectsAreImmutable` ni več dobil pričakovane izjeme pri spremembi mape. |
| `DotaAccountIdConverter` | Osnovni SteamID64 odmik povečan za 1. | Test pretvorbe je dobil napačen account ID, test spodnje meje pa je dobil izjemo. |
| `DotaAccountIdConverter` | Spodnja meja spremenjena iz `accountId < 0` v `accountId <= 0`. | `acceptsTheLowestSupportedSteamId64` je odpovedal z izjemo. |
| `ClientIpAddressResolver` | Izbran drugi namesto prvega naslova v `X-Forwarded-For`. | `resolvesFirstForwardedForAddress` je pričakoval `198.51.100.10`, dobil pa `203.0.113.20`. |
| `ClientIpAddressResolver` | Odstranjeno obrezovanje presledkov pri `X-Real-IP`. | `usesRealIpHeaderWhenForwardedForIsAbsent` je dobil naslov z začetnimi in končnimi presledki. |
| `CorsProperties` | Privzeti lokalni izvor spremenjen z vrat 3000 na 3001. | Test privzetih izvorov in test rezervnih vrednosti sta zaznala napačen naslov. |
| `CorsProperties` | Filter praznih vrednosti obrnjen. | Test konfiguriranih izvorov in test praznega seznama sta zaznala napačne vrednosti. |

Vsaka od šestih izbranih enot je bila preverjena z dvema ločenima začasnima napakama. Ciljni testni razred je pri vseh dvanajstih mutacijah odpovedal. Po vsakem poskusu je bila izvorna produkcijska datoteka obnovljena; po končnem zagonu ni produkcijskih sprememb zaradi mutacij.

## Pokritost

Poročilo JaCoCo za celoten backend je v `backend/target/site/jacoco/index.html`; strojno berljiva različica je `backend/target/site/jacoco/jacoco.xml`. Poročilo je bilo ponovno izdelano po celotnem testnem zagonu.

| Metrika | Pokritost |
| --- | ---: |
| Vrstice | 57,7 % |
| Bajtkodne instrukcije (približek za stavke) | 58,3 % |
| Metode | 60,1 % |
| Veje | 54,0 % |

Pokritost izbranih enot: `AnalyticsFilters` 100 % vrstic / 100 % vej; `RequestRateLimiter` 85,3 % vrstic / 66,7 % vej; `AuditJsonSanitizer` 98,2 % vrstic / 92,0 % vej; `DotaAccountIdConverter` 87,5 % vrstic / 62,5 % vej; `ClientIpAddressResolver` 90,9 % vrstic / 83,3 % vej; `CorsProperties` 100 % vrstic / 87,5 % vej. Pri omejevalniku ostane nepokrita predvsem redka veja čiščenja več kot 10.000 števcev. Prag pokritosti ni nastavljen.

## CI/CD

`backend/pom.xml` uporablja JaCoCo 0.8.15, ki podpira lokalni JDK 25 in CI JDK 21. Uradna zgodovina izdaj potrjuje podporo za Java 25: [JaCoCo Change History](https://www.jacoco.org/jacoco/trunk/doc/changes.html).

V `.github/workflows/backend-ci.yml` se po `clean test` izdela poročilo JaCoCo, nato sledi gradnja in Postgres integracijski korak. GitHub Actions privzeto ustavi job pri neuspešnem koraku; testni ukaz nima `continue-on-error`, zato se naslednji koraki ne izvedejo. Na koncu se poročilo objavi kot artefakt `backend-jacoco-report`, tudi če kasnejši korak odpove.

## Izvajanje in omejitve

Lokalna ukaza:

```powershell
cd backend
.\mvnw.cmd -B test "-Dspring.profiles.active=test"
.\mvnw.cmd -B jacoco:report
```

Celoten backend testni zagon po dodajanju šestih novih testov: 541 testov, 0 napak, 0 neuspehov, 49 preskočenih. Preskočeni so integracijski testi, ki zahtevajo PostgreSQL in Supabase-kompatibilne vloge. Dodatno ciljno preverjanje šestih izbranih testnih razredov je izvedlo vseh 26 testov brez napak. Testi niso bili izvajani proti povezani uporabnikovi Supabase bazi, ker bi lahko spreminjali podatke. Mavenov status ob namernih napakah je bil 1, kar potrjuje, da neuspešni test prekine izvajanje.

Lokalni integracijski profil in CI job, ki zažene zunanje Postgres integracijske teste, nista bila izvedena v tej nalogi. Backend CI pa je konfiguriran, da ju izvede po uspešnih enotnih testih in objavi poročilo o pokritosti.
