# Specifikacije enotnih testov

## Namen in ogrodje

Dokument določa pričakovano obnašanje šestih izbranih enot v backendu. Testi so napisani z JUnit 5 in preverjajo rezultate z AssertJ. Zunanji sistemi, kot so podatkovna baza, omrežje ali Supabase, niso potrebni.

`ClientIpAddressResolver` sprejme HTTP-zahtevo; zanjo testi uporabljajo Springov `MockHttpServletRequest`. `RequestRateLimiter` prejme nadzorovano uro, zato časovni prehodi ne zahtevajo čakanja. Ostale enote so preverjene z neposrednimi vhodnimi vrednostmi.

## `AnalyticsFilters`

**Enota:** `backend/src/main/java/si/um/feri/dotaops/backend/analytics/domain/AnalyticsFilters.java`  
**Testni razred:** `backend/src/test/java/si/um/feri/dotaops/backend/analytics/domain/AnalyticsFiltersTest.java`

| Test | Vhod / scenarij | Pričakovani rezultat |
| --- | --- | --- |
| `limitDefaultsToTenAndCapsAtOneHundred` | Omejitev 0 ali negativna, 1, 100 ali 250. | Vrednosti `<= 0` postanejo 10; 1 in 100 ostaneta; 250 se omeji na 100. |
| `allowsInclusiveEqualDateBounds` | `from` in `to` sta isti trenutek. | Zapis se ustvari, obe meji ostaneta enaki podanemu trenutku. |
| `withMethodsPreserveUnchangedFiltersAndDateBounds` | Za vsak `with*` se spremeni en filter ob nastavljenih ostalih filtrih in datumih. | Spremeni se le izbrani filter; vsi drugi filtri, datumi in omejitev ostanejo nespremenjeni. |
| `rejectsTimeRangeWhereFromIsAfterTo` | Začetni datum je za končnim datumom. | Ustvarjanje vrže `BadRequestException` s sporočilom `Analytics time range is invalid.` |
| `allowsOpenEndedTimeRanges` | Podan je samo `from` ali samo `to`. | Obe obliki sta dovoljeni in podana meja se ohrani. |

## `RequestRateLimiter`

**Enota:** `backend/src/main/java/si/um/feri/dotaops/backend/common/security/RequestRateLimiter.java`  
**Testni razred:** `backend/src/test/java/si/um/feri/dotaops/backend/common/security/RequestRateLimiterTest.java`

| Test | Vhod / scenarij | Pričakovani rezultat |
| --- | --- | --- |
| `rejectsMatchImportAfterPerUserLimit` | Isti uporabnik pošlje 10 zahtev za uvoz, nato še eno. | Prvih 10 uspe; 11. vrže `RateLimitExceededException` s sporočilom za uporabniško kvoto. |
| `rejectsMatchImportAfterPerIpLimit` | Različni uporabniki pošljejo 30 zahtev z istega IP-ja, nato še eno. | Prvih 30 uspe; 31. vrže izjemo s sporočilom za IP-kvoto. |
| `rejectsSteamLoginAfterPerIpLimit` | Z istega IP-ja se izvede 20 poskusov Steam prijave, nato še eden. | Prvih 20 uspe; 21. vrže izjemo s sporočilom za Steam kvoto. |
| `matchImportCountersAreIsolatedByUserAndIpAndScope` | Uporabnik in IP dosežeta vsak svojo kvoto; drugi uporabnik, drug IP in Steam prijave uporabljajo ločene števce. | Kvota se spremlja ločeno po uporabniku, IP-ju in vrsti operacije; zavrnitev enega uporabnika ne blokira drugega uporabnika na novem IP-ju. |
| `matchImportCountersResetAtTheNextOneMinuteWindow` | Kvota za uvoz je dosežena tik pred naslednjo minutno mejo; nadzorovana ura se pomakne do meje. | Zahteva pred mejo je zavrnjena; po začetku nove minute se števec ponastavi in nova zahteva uspe. |
| `steamLoginCounterResetsAtTheNextFiveMinuteWindow` | Kvota Steam prijav je dosežena tik pred naslednjo petminutno mejo; ura se pomakne do meje. | Zahteva pred mejo je zavrnjena; po začetku novega okna je nova zahteva dovoljena. |
| `normalizesClientIpBeforeApplyingRateLimit` | Dovoljene zahteve uporabljajo IP z okoliškimi presledki, zadnja pa isti IP brez presledkov. | Vse zahteve štejejo v isti IP-števec; zadnja je zavrnjena. |

## `AuditJsonSanitizer`

**Enota:** `backend/src/main/java/si/um/feri/dotaops/backend/audit/service/AuditJsonSanitizer.java`  
**Testni razred:** `backend/src/test/java/si/um/feri/dotaops/backend/audit/service/AuditJsonSanitizerTest.java`

| Test | Vhod / scenarij | Pričakovani rezultat |
| --- | --- | --- |
| `recursivelyRedactsSensitiveFieldsAndDropsFieldsOutsideProjection` | Gnezden JSON vsebuje varna polja, gesla, žetone, API-ključe in polje zunaj dovoljenega seznama. | Občutljive vrednosti na vseh ravneh in v objektih znotraj seznamov postanejo `[REDACTED]`; varne vrednosti ostanejo; nedovoljena vrhnja polja se izpustijo. |
| `projectsOnlyAllowlistedTopLevelFieldsAndNormalizesSensitiveNames` | Vhod vsebuje dovoljeni in nedovoljeni polji ter občutljiva imena z različno velikostjo črk, camelCase, vezaji in priponami. | V projekciji ostanejo samo dovoljena vrhnja polja; občutljiva ugnezdena polja so redigirana, varna pa ohranjena. |
| `malformedOrNonObjectInputsReturnEmptyProjectionWhileBlankAndNullReturnNull` | Vhod je `null`, prazen niz, neveljaven JSON, JSON `null`, seznam ali število. | `null` in prazen vhod vrneta `null`; neveljaven JSON in veljaven JSON, ki ni objekt, vrneta prazno projekcijo. |
| `nestedArraysPreserveScalarAndNullShapesWhileRedactingObjectMembers` | Seznam vsebuje besedilo, število, logično vrednost, `null`, objekt z žetonom in ugnezden seznam. | Vrstni red in tipi vrednosti se ohranijo; občutljivo polje v objektu se redigira. |
| `returnedProjectionAndNestedObjectsAreImmutable` | Poskus spreminjanja vrnjene projekcije in ugnezdene mape. | Obe spremembi vržeta `UnsupportedOperationException`. |

## `DotaAccountIdConverter`

**Enota:** `backend/src/main/java/si/um/feri/dotaops/backend/opendota/service/DotaAccountIdConverter.java`  
**Testni razred:** `backend/src/test/java/si/um/feri/dotaops/backend/opendota/service/DotaAccountIdConverterTest.java`

| Test | Vhod / scenarij | Pričakovani rezultat |
| --- | --- | --- |
| `convertsSteamId64ToDotaAccountId32` | Veljaven 17-mestni SteamID64 `76561198000000001`. | Pretvori se v Dota account ID32 `39734273`. |
| `rejectsInvalidSteamId64` | Neštevilski niz `not-a-steam-id`. | Vrže `IllegalArgumentException` s sporočilom, da mora biti SteamID64 17-mestna številska vrednost. |
| `acceptsTheLowestSupportedSteamId64` | Spodnja meja SteamID64 `76561197960265728`. | Pretvori se v account ID32 `0`. |

## `ClientIpAddressResolver`

**Enota:** `backend/src/main/java/si/um/feri/dotaops/backend/common/security/ClientIpAddressResolver.java`  
**Testni razred:** `backend/src/test/java/si/um/feri/dotaops/backend/common/security/ClientIpAddressResolverTest.java`

| Test | Vhod / scenarij | Pričakovani rezultat |
| --- | --- | --- |
| `resolvesFirstForwardedForAddress` | `X-Forwarded-For` vsebuje dva naslova, oddaljeni naslov je drugačen. | Vrne prvi naslov iz `X-Forwarded-For`. |
| `fallsBackToRemoteAddress` | Posredovani glavi manjkata, nastavljen je oddaljeni naslov. | Vrne oddaljeni naslov zahteve. |
| `usesRealIpHeaderWhenForwardedForIsAbsent` | `X-Forwarded-For` manjka, `X-Real-IP` vsebuje naslov s presledki, oddaljeni naslov je drugačen. | Vrne obrezan naslov iz `X-Real-IP`. |

## `CorsProperties`

**Enota:** `backend/src/main/java/si/um/feri/dotaops/backend/config/properties/CorsProperties.java`  
**Testni razred:** `backend/src/test/java/si/um/feri/dotaops/backend/config/properties/CorsPropertiesTest.java`

| Test | Vhod / scenarij | Pričakovani rezultat |
| --- | --- | --- |
| `defaultsStayLimitedToLocalDevelopmentOrigins` | Konfiguriran seznam izvorov je `null`. | Uporabita se samo `http://localhost:3000` in `http://127.0.0.1:3000`. |
| `configuredOriginsAreTrimmedAndBlankEntriesAreIgnored` | Konfiguracija vsebuje izvor s presledki, prazen vnos in drug veljaven izvor. | Presledki se odstranijo, prazen vnos se izpusti, vrstni red veljavnih izvorov se ohrani. |
| `fallsBackToLocalOriginsWhenAllConfiguredEntriesAreBlank` | Konfiguriran seznam vsebuje samo prazne vnose. | Uporabita se privzeta lokalna izvora. |

## Zagon testov

V PowerShellu iz korenske mape projekta zaženi izbrane teste:

```powershell
cd backend
.\mvnw.cmd -B "-Dtest=AnalyticsFiltersTest,RequestRateLimiterTest,AuditJsonSanitizerTest,DotaAccountIdConverterTest,ClientIpAddressResolverTest,CorsPropertiesTest" "-Dspring.profiles.active=test" test
```

Za celoten backend testni nabor uporabi:

```powershell
cd backend
.\mvnw.cmd -B test "-Dspring.profiles.active=test"
```
