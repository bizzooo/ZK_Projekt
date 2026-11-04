# Funkcionalni testni scenariji in poročilo izvedbe

**Projekt:** DotaOps  
**Dokument:** FT-DOTAOPS-01, različica 1.1  
**Datum priprave:** 3. 11. 2026 · **Datum ponovne izvedbe in uskladitve:** 4. 11. 2026  
**Obseg:** devet funkcionalnih scenarijev – trije za vsakega od treh članov ekipe

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

Scenariji so izvedeni avtomatizirano na ravni backend servisov in controllerjev z JUnit 5 testi. Baza, Supabase Storage in OpenDota so nadomeščeni z Mockito nadomestnimi objekti (mock), controller testi pa HTTP sloj preverijo z MockMvc. Testi torej preverjajo poslovna pravila in HTTP pogodbe, ne pa brskalniškega UI-ja, prave baze ali zunanjih storitev; izvedba **ni** polni E2E test.

Iz obsega so izpuščeni tokovi, ki jih naloga prepoveduje: (1) javno iskanje in ogled turnirja; (2) prijava ekipe, pregled organizatorja in check-in; (3) generiranje izločilnega razporeda, vnos rezultata in napredovanje zmagovalca. V FT-06 je odobrena prijava ekipe samo vnaprej pripravljen testni podatek; prijava, pregled prijave in check-in se ne izvajajo.

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
| FT-01 | Posodobitev profila z normalizacijo polj | Član 1 | `ProfileServiceTest#updateCurrentProfileNormalizesPatchFields` |
| FT-02 | Ustvaritev ekipe in samodejna nastavitev kapitana | Član 1 | `TeamServiceTest#createTeamSetsAuthenticatedUserAsCaptainAndGeneratesSlug` |
| FT-03 | Kapitan sprejme prošnjo igralca za pridružitev ekipi | Član 1 | `TeamJoinRequestServiceTest#captainCanAcceptJoinRequestAndMemberIsCreated` |
| FT-04 | Organizator ustvari osnutek turnirja s privzetimi nastavitvami | Član 2 | `TournamentServiceTest#organizerCanCreateDraftTournamentWithDefaults`; `TournamentControllerTest#createTournamentReturnsCreatedTournament` |
| FT-05 | Lastnik objavi veljaven osnutek turnirja | Član 2 | `TournamentServiceTest#ownerCanPublishTournament`; `TournamentControllerTest#publishTournamentReturnsPublishedTournament` |
| FT-06 | Organizator razporedi odobreno ekipo v skupino | Član 2 | `TournamentGroupServiceTest#organizerCanAddApprovedParticipantToGroup` |
| FT-07 | Uvoz tekme OpenDota zaključi uspešno in zabeleži statuse | Član 3 | `MatchImportServiceTest#importMatchCreatesProcessingRecordFetchesOpenDotaAndStoresReadyPayload` |
| FT-08 | Ponovni uvoz po napaki uporabi isti zapis in ne podvoji igralcev | Član 3 | `MatchImportServiceTest#retryExistingErrorImportDoesNotCreateDuplicateImportAndReplacesPlayersOnce` |
| FT-09 | Podpisan avatar URL in potrditev shranjenega avatarja | Član 3 | `StorageUploadServiceTest#createCurrentAvatarUploadUrlGeneratesProfileScopedPath`, `#confirmCurrentAvatarPersistsBackendGeneratedPublicUrlAndPath`; `StorageUploadControllerTest#createCurrentAvatarUploadUrlReturnsSignedUploadContract`, `#confirmCurrentAvatarReturnsPersistedAvatarUrl` |

## 5. Specifikacije testnih scenarijev

### FT-01 – Posodobitev profila z normalizacijo polj

| Testni scenarij | **DotaOps-TEST-TestScenarij-FT-01** |
| --- | --- |
| **Aplikacija:** DotaOps · **Verzija aplikacije:** 0.0.1-SNAPSHOT | **Datum scenarija:** 3. 11. 2026 · **Avtor:** Član 1 |
| **Kratek opis** | Posodobitev lastnega profila z normalizacijo polj |
| **Referenca:** `ProfileService.updateCurrentProfile`; `PATCH /api/me/profile`; `docs/project-current-state-overview.md` | **Zahteve v SZPO:** / (GitHub issue #40 BE/DB-07 Profile API) |

**Namen:** Preveriti, da servis shrani poslana polja profila, odstrani odvečne presledke, kodo države pretvori v velike črke in nenavedenega polja ne obravnava kot spremembe.

| Predpogoji | Vhodni podatki |
| --- | --- |
| Avtenticiran uporabnik z obstoječim profilom `CarryOne`, država `SI`. | nickname `" SupportTwo "`, bio `"  Draft caller  "`, country `"de"`; displayName ni poslan (`null`). |

**Scenariji testiranja**

| Korak | Akcija | Pričakovan rezultat |
| --- | --- | --- |
| 1. | Pokliči posodobitev profila trenutnega uporabnika z vhodnimi podatki. | Servis poišče profil in pošlje ukaz za posodobitev repozitoriju. |
| 2. | Preveri ukaz, poslan repozitoriju. | nickname `SupportTwo`, bio `Draft caller`, država `DE`; vsa tri polja so označena kot poslana. |
| 3. | Preveri polje displayName v ukazu. | displayName je označen kot neposlan in ima vrednost `null`. |

**Pogoji uspešnega zaključka**

| Korak | Akcija | Pričakovan rezultat |
| --- | --- | --- |
| 1. | Zaženi `ProfileServiceTest#updateCurrentProfileNormalizesPatchFields`. | Test uspe (vse 8 trditev). |

**Opombe, komentarji, navodila:** Repozitorij je nadomeščen z mock objektom; dejanski zapis v bazo in HTTP sloj nista preverjena.

**Rezultat izvedbe (4. 11. 2026):** vse vrednosti ukaza ustrezajo pričakovanim – **PASS**.

### FT-02 – Ustvaritev ekipe in samodejna nastavitev kapitana

| Testni scenarij | **DotaOps-TEST-TestScenarij-FT-02** |
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

### FT-03 – Kapitan sprejme prošnjo igralca za pridružitev ekipi

| Testni scenarij | **DotaOps-TEST-TestScenarij-FT-03** |
| --- | --- |
| **Aplikacija:** DotaOps · **Verzija aplikacije:** 0.0.1-SNAPSHOT | **Datum scenarija:** 3. 11. 2026 · **Avtor:** Član 1 |
| **Kratek opis** | Sprejem prošnje za pridružitev ekipi |
| **Referenca:** `TeamJoinRequestService.acceptJoinRequest`; `docs/project-current-state-overview.md` (ekipe, join requests) | **Zahteve v SZPO:** / |

**Namen:** Preveriti, da kapitan sprejme čakajočo prošnjo, ta preide v status `accepted` in prosilec postane član ekipe. Gre za članstvo v ekipi, ne za prijavo ekipe na turnir.

| Predpogoji | Vhodni podatki |
| --- | --- |
| Prošnja v statusu `pending`; ekipa obstaja; trenutni uporabnik je kapitan ekipe (vloga `player`); omejitev rostra je 5. | ID čakajoče prošnje (testni fixture). |

**Scenariji testiranja**

| Korak | Akcija | Pričakovan rezultat |
| --- | --- | --- |
| 1. | Kapitan sprejme prošnjo. | Prošnja se v repozitoriju označi kot sprejeta. |
| 2. | Preveri odgovor storitve. | Status prošnje je `ACCEPTED`. |
| 3. | Preveri ustvarjeni roster zapis. | Prosilec je dodan v ekipo z vlogo `SUPPORT`. |

**Pogoji uspešnega zaključka**

| Korak | Akcija | Pričakovan rezultat |
| --- | --- | --- |
| 1. | Zaženi `TeamJoinRequestServiceTest#captainCanAcceptJoinRequestAndMemberIsCreated`. | Test uspe. |

**Opombe, komentarji, navodila:** Repozitoriji so mock. Pravilo, da prosilec ne sme biti član druge ekipe, v tem testu ni posebej preverjeno.

**Rezultat izvedbe (4. 11. 2026):** status `ACCEPTED`, roster zapis za prosilca ustvarjen – **PASS**.

### FT-04 – Organizator ustvari osnutek turnirja s privzetimi nastavitvami

| Testni scenarij | **DotaOps-TEST-TestScenarij-FT-04** |
| --- | --- |
| **Aplikacija:** DotaOps · **Verzija aplikacije:** 0.0.1-SNAPSHOT | **Datum scenarija:** 3. 11. 2026 · **Avtor:** Član 2 |
| **Kratek opis** | Ustvarjanje osnutka turnirja |
| **Referenca:** `TournamentService.createTournament`; `POST /api/organizer/tournaments` | **Zahteve v SZPO:** / |

**Namen:** Preveriti ustvarjanje turnirja, normalizacijo naslova, ustvarjen slug in privzete nastavitve, kadar jih organizator ne poda, ter HTTP odgovor API-ja. Scenarij se konča pri osnutku in ne preverja javnega iskanja ali ogleda turnirja.

| Predpogoji | Vhodni podatki |
| --- | --- |
| Avtenticiran organizator. | Servis: naslov `"  Mid Wars Open  "`, opis `"  Public qualifier  "`, `startsAt`, prijavno okno od `startsAt − 7 dni` do `startsAt − 1 dan`; format in nastavitve niso podani. API: JSON z naslovom `Mid Wars Open`, `startsAt` in nastavitvami. |

**Scenariji testiranja**

| Korak | Akcija | Pričakovan rezultat |
| --- | --- | --- |
| 1. | Ustvari turnir prek servisa. | Uporabi se kontekst akterja (organizatorja); repozitorij prejme ukaz za ustvarjanje. |
| 2. | Preveri ukaz repozitoriju. | naslov `Mid Wars Open`, slug `mid-wars-open`, format `SINGLE_ELIMINATION`, organizator in avtor sta trenutni uporabnik, največ ekip = privzeto (8), `teamSize` = 5. |
| 3. | Pošlji `POST /api/organizer/tournaments` z veljavnim JWT. | HTTP `201 Created`, glava `Location` s potjo do turnirja, v telesu ID in status `draft`. |

**Pogoji uspešnega zaključka**

| Korak | Akcija | Pričakovan rezultat |
| --- | --- | --- |
| 1. | Zaženi `TournamentServiceTest#organizerCanCreateDraftTournamentWithDefaults`. | Test uspe. |
| 2. | Zaženi `TournamentControllerTest#createTournamentReturnsCreatedTournament`. | Test uspe. |

**Opombe, komentarji, navodila:** Servisni test ne preverja statusa `draft` v ukazu repozitoriju. V controller testu je servis nadomeščen z mock objektom, zato status `draft` v odgovoru izvira iz pripravljenega odgovora; test potrjuje HTTP pogodbo (status, `Location`, oblika JSON), ne poslovne logike.

**Rezultat izvedbe (4. 11. 2026):** vrednosti ukaza in HTTP odgovor ustrezajo pričakovanim – **PASS**.

### FT-05 – Lastnik objavi veljaven osnutek turnirja

| Testni scenarij | **DotaOps-TEST-TestScenarij-FT-05** |
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

### FT-06 – Organizator razporedi odobreno ekipo v skupino

| Testni scenarij | **DotaOps-TEST-TestScenarij-FT-06** |
| --- | --- |
| **Aplikacija:** DotaOps · **Verzija aplikacije:** 0.0.1-SNAPSHOT | **Datum scenarija:** 3. 11. 2026 · **Avtor:** Član 2 |
| **Kratek opis** | Razporeditev ekipe v skupino turnirja |
| **Referenca:** `TournamentGroupService.addTeam`; `POST /api/organizer/tournament-groups/{groupId}/teams` | **Zahteve v SZPO:** / |

**Namen:** Preveriti razporeditev ekipe z že odobreno prijavo v skupino in shranitev seed številke. Scenarij ne odda prijave, ne pregleduje prijav in ne izvaja check-ina.

| Predpogoji | Vhodni podatki |
| --- | --- |
| Organizator lahko upravlja turnir; skupina in ekipa obstajata; za ekipo obstaja odobrena prijava (testni fixture); ekipa še ni razporejena. | ID skupine, ID ekipe, seed `2`. |

**Scenariji testiranja**

| Korak | Akcija | Pričakovan rezultat |
| --- | --- | --- |
| 1. | Dodaj ekipo v skupino s seed številko `2`. | Uporabi se kontekst akterja; servis poišče odobreno prijavo. |
| 2. | Preveri odgovor. | Vrnjeni so ID ekipe, ID odobrene prijave in seed `2`. |
| 3. | Preveri ukaz repozitoriju. | Ukaz vsebuje izbrano skupino, ekipo, prijavo in seed `2`. |

**Pogoji uspešnega zaključka**

| Korak | Akcija | Pričakovan rezultat |
| --- | --- | --- |
| 1. | Zaženi `TournamentGroupServiceTest#organizerCanAddApprovedParticipantToGroup`. | Test uspe. |

**Opombe, komentarji, navodila:** Repozitoriji so mock; odobrena prijava je predpripravljen podatek.

**Rezultat izvedbe (4. 11. 2026):** odgovor in ukaz repozitoriju ustrezata pričakovanemu – **PASS**.

### FT-07 – Uvoz tekme OpenDota zaključi uspešno in zabeleži statuse

| Testni scenarij | **DotaOps-TEST-TestScenarij-FT-07** |
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

### FT-08 – Ponovni uvoz po napaki uporabi isti zapis in ne podvoji igralcev

| Testni scenarij | **DotaOps-TEST-TestScenarij-FT-08** |
| --- | --- |
| **Aplikacija:** DotaOps · **Verzija aplikacije:** 0.0.1-SNAPSHOT | **Datum scenarija:** 3. 11. 2026 · **Avtor:** Član 3 |
| **Kratek opis** | Ponovna oddaja uvoza za tekmo, katere uvoz je bil neuspešen |
| **Referenca:** `MatchImportService.importMatch` (veja za obstoječ uvoz v stanju `error`); `POST /api/match-imports` | **Zahteve v SZPO:** / |

**Namen:** Preveriti, da ponovna oddaja uvoza za tekmo z neuspešnim uvozom ponovno uporabi obstoječi zapis (isti ID), ne ustvari novega in igralce zapiše samo enkrat.

| Predpogoji | Vhodni podatki |
| --- | --- |
| Za tekmo obstaja uvoz v stanju `error` (koda `RATE_LIMITED`, »Rate limited.«); odjemalec OpenDota (mock) tokrat vrne veljaven odgovor. | ID tekme `7894561230`, IP odjemalca `203.0.113.10`. |

**Scenariji testiranja**

| Korak | Akcija | Pričakovan rezultat |
| --- | --- | --- |
| 1. | Ponovno oddaj uvoz za isto tekmo. | Nov zapis uvoza se ne ustvari (`createQueued` ni klican). |
| 2. | Preveri prehod v obdelavo. | Obstoječi zapis preide v `processing` s sporočilom »Match import retry requested; processing restarted.« |
| 3. | Preveri odgovor. | Isti ID uvoza; status `READY`. |
| 4. | Preveri zapis normaliziranih podatkov. | `markReady` je klican natanko enkrat z 1 igralcem; zahtevana je osvežitev analitike. |

**Pogoji uspešnega zaključka**

| Korak | Akcija | Pričakovan rezultat |
| --- | --- | --- |
| 1. | Zaženi `MatchImportServiceTest#retryExistingErrorImportDoesNotCreateDuplicateImportAndReplacesPlayersOnce`. | Test uspe. |

**Opombe, komentarji, navodila:** Test pokriva ponovno oddajo prek `importMatch`. Ločen endpoint `POST /api/match-imports/{id}/retry` (`retryImport`) v tem scenariju ni preverjen. Pravi OpenDota API ni klican.

**Rezultat izvedbe (4. 11. 2026):** ohranjen isti ID, nov zapis ni ustvarjen, status `READY`, en normaliziran igralec – **PASS**.

### FT-09 – Podpisan avatar URL in potrditev shranjenega avatarja

| Testni scenarij | **DotaOps-TEST-TestScenarij-FT-09** |
| --- | --- |
| **Aplikacija:** DotaOps · **Verzija aplikacije:** 0.0.1-SNAPSHOT | **Datum scenarija:** 3. 11. 2026 · **Avtor:** Član 3 |
| **Kratek opis** | Nalaganje avatarja prek podpisanega URL-ja |
| **Referenca:** `StorageUploadService`; `POST /api/me/avatar/upload-url`, `POST /api/me/avatar/confirm`; `docs/backend-storage-upload-api.md` | **Zahteve v SZPO:** / |

**Namen:** Preveriti, da je podpisan URL omejen na pot trenutnega profila, da ne razkrije skrivnega ključa, in da potrditev shrani javni URL, ki ga izračuna backend.

| Predpogoji | Vhodni podatki |
| --- | --- |
| Avtenticiran igralec; Supabase Storage je nadomeščen z mock objektom. | Izdaja: `avatar.png`, `image/png`, 1024 B. Potrditev (servis): `profiles/{id}/avatar.jpg`; potrditev (API): `profiles/{id}/avatar.png`. |

**Scenariji testiranja**

| Korak | Akcija | Pričakovan rezultat |
| --- | --- | --- |
| 1. | Zahtevaj podpisan URL za PNG prek servisa. | bucket `avatars`, pot `profiles/{id}/avatar.png`, metoda `PUT`, največ 2 MiB, tip `image/png`; URL ne vsebuje `service-role-key`. |
| 2. | Potrdi naložen JPG prek servisa. | Servis izračuna javni URL, posodobi profil in vrne `persisted=true`. |
| 3. | Pošlji `POST /api/me/avatar/upload-url`. | HTTP `200 OK`; bucket, pot, metoda `PUT`, podpisan in javni URL, 2 MiB, `upsert=true`; brez skrivnega ključa. |
| 4. | Pošlji `POST /api/me/avatar/confirm`. | HTTP `200 OK`; `avatarUrl` je javni URL, `persisted=true`. |

**Pogoji uspešnega zaključka**

| Korak | Akcija | Pričakovan rezultat |
| --- | --- | --- |
| 1. | Zaženi oba testa v `StorageUploadServiceTest`. | Oba testa uspeta. |
| 2. | Zaženi oba testa v `StorageUploadControllerTest`. | Oba testa uspeta. |

**Opombe, komentarji, navodila:** Dejanski prenos datoteke v Supabase ni del testnega okolja. V controller testih je servis mock, zato preverjata HTTP pogodbo.

**Rezultat izvedbe (4. 11. 2026):** vse preverjene vrednosti ustrezajo pričakovanim – **PASS**.

## 6. Zapisnik izvedbe

| Datum | ID-ji | Izvedeni testi | Uspešni | Neuspešni | Status |
| --- | --- | --- | ---: | ---: | --- |
| 3. 11. 2026 | FT-01–FT-09 | 14 izbranih JUnit metod | 14 | 0 | Uspešno |
| 4. 11. 2026 | FT-01–FT-09 | 14 izbranih JUnit metod (ponovna izvedba) | 14 | 0 | Uspešno |

Ponovna izvedba 4. 11. 2026 je potekala s standardnim Maven Wrapper zagonom (brez posebnih nastavitev). Rezultat Surefire: **Tests run: 14, Failures: 0, Errors: 0, Skipped: 0 – BUILD SUCCESS**. Izvedba 3. 11. 2026 je zaradi omejitev peskovnika (JVM self-attach, loopback povezave) potekala z `-DforkCount=0` in Mockito agentom, z enakim rezultatom.

Ukaz za ponovitev (v mapi `backend`):

```powershell
.\mvnw.cmd test "-Dtest=ProfileServiceTest#updateCurrentProfileNormalizesPatchFields,TeamServiceTest#createTeamSetsAuthenticatedUserAsCaptainAndGeneratesSlug,TeamJoinRequestServiceTest#captainCanAcceptJoinRequestAndMemberIsCreated,TournamentServiceTest#organizerCanCreateDraftTournamentWithDefaults+ownerCanPublishTournament,TournamentControllerTest#createTournamentReturnsCreatedTournament+publishTournamentReturnsPublishedTournament,TournamentGroupServiceTest#organizerCanAddApprovedParticipantToGroup,MatchImportServiceTest#importMatchCreatesProcessingRecordFetchesOpenDotaAndStoresReadyPayload+retryExistingErrorImportDoesNotCreateDuplicateImportAndReplacesPlayersOnce,StorageUploadServiceTest#createCurrentAvatarUploadUrlGeneratesProfileScopedPath+confirmCurrentAvatarPersistsBackendGeneratedPublicUrlAndPath,StorageUploadControllerTest#createCurrentAvatarUploadUrlReturnsSignedUploadContract+confirmCurrentAvatarReturnsPersistedAvatarUrl" "-Dsurefire.failIfNoSpecifiedTests=false"
```

## 7. Sledljivost in omejitve

| Področje | Scenariji | Pokritje / omejitev |
| --- | --- | --- |
| Profil | FT-01 | Normalizacija zahtevka in ukaza repozitoriju; brez zapisa v bazo in brez HTTP sloja. |
| Ekipe | FT-02–FT-03 | Ustvarjanje ekipe, kapetanov roster zapis in sprejem prošnje; repozitoriji so mock. |
| Turnirji in skupine | FT-04–FT-06 | Ustvarjanje osnutka, objava (servis in HTTP pogodba) ter razporeditev ekipe v skupino; brez javne strani, prijave, check-ina ali izločilnega razporeda. |
| Zunanji podatki | FT-07–FT-08 | Življenjski cikel uvoza, normalizacija in ponovna oddaja po napaki z mock odgovorom OpenDota; endpoint `/{id}/retry` ni pokrit. |
| Slike | FT-09 | Servisna logika in HTTP pogodba za podpisan URL in potrditev; brez prenosa datoteke v Supabase. |

Za polno uporabniško funkcionalno testiranje bi bilo treba scenarije ponoviti prek delujočega frontenda in backenda z ločeno testno bazo, testnimi računi ter razvojnim Supabase Storage/OpenDota. To ni bilo izvedeno.

## 8. Popravki v različici 1.1

Pri preverjanju skladnosti zapisanih rezultatov z dejanskimi testi so bili popravljeni naslednji zapisi:

| Scenarij | Prej | Popravek |
| --- | --- | --- |
| FT-08 | Sklic na `MatchImportService.retryImport` in `POST /api/match-imports/{id}/retry`. | Test kliče `importMatch` (ponovna oddaja prek `POST /api/match-imports`); `/{id}/retry` je navedeno kot nepokrito. |
| FT-07 | »OpenDota se kliče enkrat«. | Test števila klicev ne preverja; dodane so dejansko preverjene točke (omejevalnik z IP, zaporedje klicev, podatki igralca, osvežitev analitike). |
| FT-04 | Status `draft` naveden kot preverjen rezultat servisa. | Servisni test statusa ne preverja; `draft` v controller testu izvira iz mock odgovora. Dodana privzeta vrednost največ ekip (8). |
| FT-03 | Predpogoj »prosilec nima aktivnega članstva v drugi ekipi«. | V testu ni pripravljen niti preverjen; naveden kot omejitev. |
| FT-05 | Koraki niso navajali preverjanj servisa. | Dodano, da servis pred objavo preveri pravico upravljanja, stanje in veljavnost osnutka; controller del označen kot preverjanje HTTP pogodbe. |
| Vsi | Skrčena oblika tabel. | Scenariji so zapisani po predlogi (aplikacija, verzija, avtor, referenca, namen, predpogoji, vhodni podatki, koraki, pogoji uspešnega zaključka, opombe). |
| Zapisnik | Le izvedba v peskovniku. | Dodana ponovna izvedba 4. 11. 2026 in natančen ukaz. |

## Viri

- ISO/IEC/IEEE 29119-3:2021, *Software and systems engineering — Software testing — Part 3: Test documentation*: https://www.iso.org/standard/79429.html
- IEEE SA, *IEEE/ISO/IEC 29119-3-2021*: https://standards.ieee.org/ieee/29119-3/7499/
- Projektna dokumentacija in implementacija v tem repozitoriju, zlasti `docs/project-current-state-overview.md`, `docs/reports/Iteracija_1_DotaOps_pregled.md`, API dokumenti pod `docs/` in testi pod `backend/src/test/java/`.
