# Funkcionalni testni scenariji in poročilo izvedbe

**Projekt:** DotaOps  
**Dokument:** FT-DOTAOPS-01, različica 1.2  
**Datum priprave:** 3. 11. 2026 · **Datum ponovne izvedbe in uskladitve:** 4. 11. 2026  
**Obseg:** trije funkcionalni scenariji – po en za vsakega od treh članov ekipe

## 1. Pregled dokumentacije in funkcionalnosti

V repozitoriju je obstoječa dokumentacija, ki opisuje namen, arhitekturo, API-je, podatkovne tokove in trenutno stanje rešitve:

| Dokument | Ugotovitev |
| --- | --- |
| `README.md` | Povzame DotaOps, tehnologije, strukturo in osnovni zagon. |
| `docs/project-current-state-overview.md` | Najbolj celovit pregled implementiranih funkcij, endpointov, podatkovnega modela, testov in omejitev. |
| `docs/reports/Iteracija_1_DotaOps_pregled.md` | Seznam GitHub issue-jev s kratkimi zahtevami (npr. #40 Profile API, #42 Ekipe CRUD). |
| `docs/backend-my-team-api.md` | Opis ekip, članstva in povezane API funkcionalnosti. |
| `docs/backend-storage-upload-api.md` | Opis podpisanih URL-jev za nalaganje avatarjev in slik ekip. |
| `docs/backend-analytics-api.md` | Opis analitičnih endpointov in filtrov. |
| `docs/backend-demo-seed.md` | Opis priprave demo podatkov. |
| Testi pod `backend/src/test/java/` | Enotski, servisni, controller in PostgreSQL integracijski testi. |

Ključne funkcionalnosti rešitve so: profili in prijava (Supabase, Steam), upravljanje ekip in rosterjev (povabila, prošnje za pridružitev), turnirji in skupine, tekme, uvoz podatkov OpenDota, analitika, audit in shranjevanje slik prek Supabase Storage. Dokumentacija dobro pokriva funkcionalnosti in API-je, ne vsebuje pa specifikacije zahtev (SZPO) in dogovorjenega seznama funkcionalnih scenarijev z lastniki in zapisnikom izvedbe. Ta dokument dopolnjuje to vrzel; kot zahteve so, kjer obstajajo, navedeni GitHub issue-ji.

## 2. Način oblikovanja in obseg testiranja

Scenariji so zapisani po predlogi, prilagojeni po ISO/IEC/IEEE 29119-3:2021 (specifikacija testnega primera/postopka): identifikacija, aplikacija in verzija, datum, avtor, kratek opis, referenca, zahteve, namen, predpogoji, vhodni podatki, koraki z akcijo in pričakovanim rezultatom, pogoji uspešnega zaključka ter opombe. Vsakemu scenariju je dodan rezultat izvedbe. [ISO – opis standarda](https://www.iso.org/standard/79429.html), [IEEE SA – 29119-3-2021](https://standards.ieee.org/ieee/29119-3/7499/).

Scenariji so izvedeni avtomatizirano na ravni backend servisov in controllerjev z JUnit 5 testi. Baza in OpenDota sta nadomeščeni z Mockito nadomestnimi objekti (mock), controller testi pa HTTP sloj preverijo z MockMvc. Testi torej preverjajo poslovna pravila in HTTP pogodbe, ne pa brskalniškega UI-ja, prave baze ali zunanjih storitev; izvedba **ni** polni E2E test.

Iz obsega so izpuščeni tokovi, ki jih naloga prepoveduje: (1) javno iskanje in ogled turnirja; (2) prijava ekipe, pregled organizatorja in check-in; (3) generiranje izločilnega razporeda, vnos rezultata in napredovanje zmagovalca.

## 3. Testno okolje in splošni kriterij uspeha

| Lastnost | Vrednost |
| --- | --- |
| Aplikacija / verzija | DotaOps backend `0.0.1-SNAPSHOT` (Spring Boot 4.0.6) |
| Orodja | Maven Wrapper (Maven 3.9.14), JDK 23.0.2 (prevajanje z `--release 21`), JUnit 5, Mockito, AssertJ, MockMvc |
| Profil | Servisni testi: čisti Mockito; controller testi: Spring `test` profil (H2 v pomnilniku) z MockMvc |
| Operacijski sistem | Windows 11 |
| Zunanji sistemi | Supabase, OpenDota in Steam niso bili klicani |
| Kriterij uspeha | Vsi navedeni testi uspejo; poslovni rezultat ustreza pričakovanemu |

Člani ekipe so označeni kot Član 1, Član 2 in Član 3; pred oddajo oznake zamenjajte z imeni.

## 4. Seznam scenarijev in odgovornosti

| ID | Naziv scenarija | Odgovorni član | Izvedeni testi |
| --- | --- | --- | --- |
| FT-01 | Ustvaritev ekipe in samodejna nastavitev kapitana | Član 1 | `TeamServiceTest#createTeamSetsAuthenticatedUserAsCaptainAndGeneratesSlug` |
| FT-02 | Lastnik objavi veljaven osnutek turnirja | Član 2 | `TournamentServiceTest#ownerCanPublishTournament`; `TournamentControllerTest#publishTournamentReturnsPublishedTournament` |
| FT-03 | Uvoz tekme OpenDota zaključi uspešno in zabeleži statuse | Član 3 | `MatchImportServiceTest#importMatchCreatesProcessingRecordFetchesOpenDotaAndStoresReadyPayload` |

## 5. Specifikacije testnih scenarijev

### FT-01 – Ustvaritev ekipe in samodejna nastavitev kapitana

| Testni scenarij | **DotaOps-TEST-TestScenarij-FT-01** |
| --- | --- |
| **Aplikacija:** DotaOps · **Verzija aplikacije:** 0.0.1-SNAPSHOT | **Datum scenarija:** 3. 11. 2026 · **Avtor:** Član 1 |
| **Kratek opis** | Ustvaritev ekipe; ustvarjalec postane kapitan |
| **Referenca:** `TeamService.createTeam`; `POST /api/teams`; `docs/backend-my-team-api.md` | **Zahteve v SZPO:** / (GitHub issue #42 BE/DB-08 Ekipe CRUD) |

**Namen:** Preveriti normalizacijo vhodnih vrednosti, samodejno ustvarjen slug ter dodelitev kapetanske vloge in roster zapisa ustvarjalcu ekipe.

| Predpogoji | Vhodni podatki |
| --- | --- |
| Prijavljen profil z globalno vlogo `player`. | ime `"  Ancient Stack  "`, oznaka `" AS "`, regija `"  EU  "`, opis `"  Tier two squad  "`; slug ni podan. |

**Scenariji testiranja**

| Korak | Akcija | Pričakovan rezultat |
| --- | --- | --- |
| 1. | Ustvari ekipo z vhodnimi podatki. | Repozitorij prejme ukaz za ustvarjanje ekipe in ukaz za člana rostra. |
| 2. | Preveri ukaz za ekipo. | ime `Ancient Stack`, oznaka `AS`, slug `ancient-stack`, regija `EU`, opis `Tier two squad`; kapitan in avtor sta trenutni uporabnik. |
| 3. | Preveri ukaz za člana rostra. | Kapitan je dodan v roster z vlogo `SUPPORT`. |

**Pogoji uspešnega zaključka**

| Korak | Akcija | Pričakovan rezultat |
| --- | --- | --- |
| 1. | Zaženi `TeamServiceTest#createTeamSetsAuthenticatedUserAsCaptainAndGeneratesSlug`. | Test uspe (vse 9 trditev). |

**Opombe, komentarji, navodila:** Repozitoriji so mock; preveri se vsebina ukazov, ne zapis v bazi.

**Rezultat izvedbe (4. 11. 2026):** vse vrednosti in roster zapis ustrezajo pričakovanim – **PASS**.

### FT-02 – Lastnik objavi veljaven osnutek turnirja

| Testni scenarij | **DotaOps-TEST-TestScenarij-FT-02** |
| --- | --- |
| **Aplikacija:** DotaOps · **Verzija aplikacije:** 0.0.1-SNAPSHOT | **Datum scenarija:** 3. 11. 2026 · **Avtor:** Član 2 |
| **Kratek opis** | Objava osnutka turnirja |
| **Referenca:** `TournamentService.publishTournament`; `POST /api/organizer/tournaments/{tournamentId}/publish` | **Zahteve v SZPO:** / |

**Namen:** Preveriti prehod turnirja iz `draft` v `published`, ko objavo zahteva organizator, ki lahko upravlja turnir. Ne preverja javne strani, kataloga ali iskanja.

| Predpogoji | Vhodni podatki |
| --- | --- |
| Turnir v stanju `draft` z veljavnimi nastavitvami in datumi; trenutni organizator lahko upravlja turnir. | ID turnirja (testni fixture). |

**Scenariji testiranja**

| Korak | Akcija | Pričakovan rezultat |
| --- | --- | --- |
| 1. | Organizator objavi osnutek prek servisa. | Servis preveri pravico upravljanja, da je turnir v stanju, ki dovoli objavo, in veljavnost nastavitev; napaka se ne sproži. |
| 2. | Preveri odgovor storitve. | Uporabi se kontekst akterja; vrnjen status je `published`. |
| 3. | Pošlji `POST /api/organizer/tournaments/{id}/publish` z veljavnim JWT. | HTTP `200 OK`, v telesu status `published`. |

**Pogoji uspešnega zaključka**

| Korak | Akcija | Pričakovan rezultat |
| --- | --- | --- |
| 1. | Zaženi `TournamentServiceTest#ownerCanPublishTournament`. | Test uspe. |
| 2. | Zaženi `TournamentControllerTest#publishTournamentReturnsPublishedTournament`. | Test uspe. |

**Opombe, komentarji, navodila:** Repozitorij je mock (dovoljenje za upravljanje in objavljen turnir sta pripravljena v testu). V controller testu je servis mock, zato test potrjuje HTTP pogodbo, ne poslovne logike.

**Rezultat izvedbe (4. 11. 2026):** status `published`, kontekst akterja uporabljen, API vrne `200 OK` – **PASS**.

### FT-03 – Uvoz tekme OpenDota zaključi uspešno in zabeleži statuse

| Testni scenarij | **DotaOps-TEST-TestScenarij-FT-03** |
| --- | --- |
| **Aplikacija:** DotaOps · **Verzija aplikacije:** 0.0.1-SNAPSHOT | **Datum scenarija:** 3. 11. 2026 · **Avtor:** Član 3 |
| **Kratek opis** | Uspešen uvoz tekme iz OpenDota |
| **Referenca:** `MatchImportService.importMatch`; `POST /api/match-imports`; `README.md` (Match import lifecycle) | **Zahteve v SZPO:** / |

**Namen:** Preveriti uspešen življenjski cikel uvoza (`queued` → `processing` → `ready`) in normalizacijo podatkov tekme.

| Predpogoji | Vhodni podatki |
| --- | --- |
| Avtenticiran organizator; za tekmo še ni uvoza; odjemalec OpenDota (mock) vrne vnaprej pripravljen odgovor. | ID tekme `7894561230`, IP odjemalca `203.0.113.10`. |

**Scenariji testiranja**

| Korak | Akcija | Pričakovan rezultat |
| --- | --- | --- |
| 1. | Zaženi uvoz tekme. | Omejevalnik zahtev preveri organizatorja in IP `203.0.113.10`. |
| 2. | Preveri zaporedje klicev repozitorija. | `createQueued` → `markProcessing` (»Match import processing started.«) → `markReady`. |
| 3. | Preveri odgovor. | Status `READY`; dogodki v zaporedju `QUEUED`, `PROCESSING`, `READY`. |
| 4. | Preveri normalizirane podatke. | 1 igralec: junak `1`, račun `39734273`, zmagovalec; surovi odgovor vsebuje `match_id` 7894561230; `playersNormalized` = 1. |
| 5. | Preveri osvežitev analitike. | Zahtevana je osvežitev analitike po uspešnem uvozu. |

**Pogoji uspešnega zaključka**

| Korak | Akcija | Pričakovan rezultat |
| --- | --- | --- |
| 1. | Zaženi `MatchImportServiceTest#importMatchCreatesProcessingRecordFetchesOpenDotaAndStoresReadyPayload`. | Test uspe. |

**Opombe, komentarji, navodila:** Pravi OpenDota API ni klican. Število klicev odjemalca OpenDota v testu ni preverjeno.

**Rezultat izvedbe (4. 11. 2026):** status `READY`, pravilno zaporedje dogodkov in klicev, normalizacija uspe – **PASS**.

## 6. Zapisnik izvedbe

| Datum | ID-ji | Izvedeni testi | Uspešni | Neuspešni | Status |
| --- | --- | --- | ---: | ---: | --- |
| 3. 11. 2026 | FT-01–FT-03 | 4 izbrane JUnit metode | 4 | 0 | Uspešno |
| 4. 11. 2026 | FT-01–FT-03 | 4 izbrane JUnit metode (ponovna izvedba) | 4 | 0 | Uspešno |

Ponovna izvedba 4. 11. 2026 je potekala s standardnim Maven Wrapper zagonom (brez posebnih nastavitev). Izvedba 3. 11. 2026 je zaradi omejitev peskovnika (JVM self-attach, loopback povezave) potekala z `-DforkCount=0` in Mockito agentom, z enakim rezultatom.

Ukaz za ponovitev (v mapi `backend`):

```powershell
.\mvnw.cmd test "-Dtest=TeamServiceTest#createTeamSetsAuthenticatedUserAsCaptainAndGeneratesSlug,TournamentServiceTest#ownerCanPublishTournament,TournamentControllerTest#publishTournamentReturnsPublishedTournament,MatchImportServiceTest#importMatchCreatesProcessingRecordFetchesOpenDotaAndStoresReadyPayload" "-Dsurefire.failIfNoSpecifiedTests=false"
```

## 7. Sledljivost in omejitve

| Področje | Scenarij | Pokritje / omejitev |
| --- | --- | --- |
| Ekipe | FT-01 | Ustvarjanje ekipe in kapetanov roster zapis; repozitoriji so mock. |
| Turnirji | FT-02 | Objava osnutka (servis in HTTP pogodba); brez javne strani, prijave, check-ina ali izločilnega razporeda. |
| Zunanji podatki | FT-03 | Življenjski cikel uvoza in normalizacija z mock odgovorom OpenDota. |

Za polno uporabniško funkcionalno testiranje bi bilo treba scenarije ponoviti prek delujočega frontenda in backenda z ločeno testno bazo, testnimi računi ter razvojnim OpenDota okoljem. To ni bilo izvedeno.

## 8. Popravki v različici 1.2

| Scenarij | Prej | Popravek |
| --- | --- | --- |
| Vsi | Devet scenarijev (po trije na člana). | Obdržani so trije scenariji, po en na člana; preostali so odstranjeni, ID-ji so preštevilčeni v FT-01–FT-03. |
| FT-03 | »OpenDota se kliče enkrat«. | Test števila klicev ne preverja; navedene so dejansko preverjene točke (omejevalnik z IP, zaporedje klicev, podatki igralca, osvežitev analitike). |
| FT-02 | Koraki niso navajali preverjanj servisa. | Dodano, da servis pred objavo preveri pravico upravljanja, stanje in veljavnost osnutka; controller del označen kot preverjanje HTTP pogodbe. |

## Viri

- ISO/IEC/IEEE 29119-3:2021, *Software and systems engineering — Software testing — Part 3: Test documentation*: https://www.iso.org/standard/79429.html
- IEEE SA, *IEEE/ISO/IEC 29119-3-2021*: https://standards.ieee.org/ieee/29119-3/7499/
- Projektna dokumentacija in implementacija v tem repozitoriju, zlasti `docs/project-current-state-overview.md`, `docs/reports/Iteracija_1_DotaOps_pregled.md`, API dokumenti pod `docs/` in testi pod `backend/src/test/java/`.
