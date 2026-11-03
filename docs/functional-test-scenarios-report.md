# Funkcionalni testni scenariji in poročilo izvedbe

**Projekt:** DotaOps  
**Datum priprave in izvedbe:** 3. 11. 2026  
**Dokument:** FT-DOTAOPS-01, različica 1.0  
**Obseg:** devet funkcionalnih scenarijev – trije za vsakega od treh članov ekipe

## Pregled dokumentacije in funkcionalnosti

V repozitoriju je obstoječa dokumentacija, ki opisuje namen, arhitekturo, API-je, podatkovne tokove in trenutno stanje rešitve:

| Dokument | Ugotovitev |
| --- | --- |
| `README.md` | Povzame DotaOps, tehnologije, strukturo in osnovni zagon. |
| `docs/project-current-state-overview.md` | Najbolj celovit pregled implementiranih funkcij, endpointov, podatkovnega modela, testov in omejitev. |
| `docs/backend-my-team-api.md` | Opis ekip, članstva in povezane API funkcionalnosti. |
| `docs/backend-storage-upload-api.md` | Opis podpisanih URL-jev za nalaganje avatarjev in slik ekip. |
| `docs/backend-analytics-api.md` | Opis analitičnih endpointov in filtrov. |
| `docs/backend-demo-seed.md` | Opis priprave demo podatkov. |
| Testi pod `backend/src/test/java/` | Vsebujejo enotske, servisne, controller in PostgreSQL integracijske teste. |

Rešitev vključuje profile in prijavo, upravljanje ekip in rosterjev, turnirje ter skupine, tekme, uvoz podatkov OpenDota, analitiko, audit in shranjevanje slik prek Supabase Storage. Dokumentacija dobro pokriva funkcionalnosti in API-je, ne vsebuje pa dogovorjenega seznama ročnih/funkcionalnih scenarijev z lastniki in zapisnikom izvedbe. Ta dokument dopolnjuje to vrzel.

## Način oblikovanja in obseg testiranja

Dokument je oblikovan po prilagojeni predlogi ISO/IEC/IEEE 29119-3:2021: vsebuje identifikacijo scenarijev, cilj, predpogoje in testne podatke, korake z pričakovanimi rezultati, sledljivost do funkcionalnosti, lastnike ter dnevnik izvedbe. Naslovi in razdelki so prilagojeni projektu. Standard opredeljuje predloge dokumentacije za različne vrste testiranja; ta dokument je projektna prilagoditev, ne prepis standarda. [ISO – opis standarda](https://www.iso.org/standard/79429.html), [IEEE SA – 29119-3-2021](https://standards.ieee.org/ieee/29119-3/7499/).

Testi so bili izvedeni avtomatizirano na ravni backend servisov in controllerjev z obstoječimi JUnit testi ter Mockito nadomestnimi objekti. Preverjajo poslovna pravila in izbrane HTTP pogodbe, ne pa delovanja brskalniškega UI-ja, prave baze Supabase ali dejanske storitve OpenDota/Storage. Izvedba v tem dokumentu zato ni predstavljena kot polni E2E test.

Iz obsega so izpuščeni natanko tokovi, navedeni kot prepovedani v nalogi: javno iskanje in ogled turnirja; prijava ekipe skupaj z organizatorjevim pregledom in check-inom; generiranje izločilnega razporeda, oddaja rezultata ter napredovanje zmagovalca. Pri testu razporejanja v skupino je odobrena prijava ekipe samo predpripravljen testni podatek; prijava, pregled prijave in check-in se ne izvajajo.

## Testno okolje in merila

| Lastnost | Vrednost |
| --- | --- |
| Datum | 3. 11. 2026 |
| Maven | 3.9.14 |
| JDK | 23.0.2; backend je preveden z `--release 21` |
| Profil | Spring `test`; izbrani servisni testi uporabljajo Mockito; kjer je potrebno, H2 |
| Zunanji sistemi | Supabase, OpenDota in Steam niso bili klicani |
| Izvedba | Selektivni JUnit 5/Surefire testi po razredih in imenih metod; brez sprememb produkcijske kode. Zaradi omejitve JVM self-attach so bili testi zagnani brez forkanja in z Mockito agentom. |
| Pogoj uspeha | Vsi navedeni testi uspejo; poslovni rezultat ustreza pričakovanemu; nedovoljen ali neveljaven tok ne spremeni stanja, kjer je to del scenarija |

V obstoječem poročilu so člani ekipe navedeni samo kot Član 1 (Windows 11), Član 2 (macOS Sonoma) in Član 3 (Ubuntu 22.04), brez imen. Zato so spodaj uporabljene iste oznake; pred oddajo jih zamenjajte z dejanskimi imeni.

## Seznam scenarijev in odgovornosti

| ID | Naziv scenarija | Odgovorni član | Izvedeni test |
| --- | --- | --- | --- |
| FT-01 | Posodobitev profila z normalizacijo polj | Član 1 (Windows 11) | `ProfileServiceTest#updateCurrentProfileNormalizesPatchFields` |
| FT-02 | Ustvaritev ekipe in samodejna nastavitev kapitana | Član 1 (Windows 11) | `TeamServiceTest#createTeamSetsAuthenticatedUserAsCaptainAndGeneratesSlug` |
| FT-03 | Kapitan sprejme prošnjo igralca za pridružitev ekipi | Član 1 (Windows 11) | `TeamJoinRequestServiceTest#captainCanAcceptJoinRequestAndMemberIsCreated` |
| FT-04 | Organizator ustvari osnutek turnirja s privzetimi nastavitvami | Član 2 (macOS Sonoma) | `TournamentServiceTest#organizerCanCreateDraftTournamentWithDefaults`; `TournamentControllerTest#createTournamentReturnsCreatedTournament` |
| FT-05 | Lastnik objavi veljaven osnutek turnirja | Član 2 (macOS Sonoma) | `TournamentServiceTest#ownerCanPublishTournament`; `TournamentControllerTest#publishTournamentReturnsPublishedTournament` |
| FT-06 | Organizator razporedi odobreno ekipo v skupino | Član 2 (macOS Sonoma) | `TournamentGroupServiceTest#organizerCanAddApprovedParticipantToGroup` |
| FT-07 | Uvoz tekme OpenDota zaključi uspešno in zabeleži statuse | Član 3 (Ubuntu 22.04) | `MatchImportServiceTest#importMatchCreatesProcessingRecordFetchesOpenDotaAndStoresReadyPayload` |
| FT-08 | Ponovni uvoz po napaki uporabi isti zapis in ne podvoji igralcev | Član 3 (Ubuntu 22.04) | `MatchImportServiceTest#retryExistingErrorImportDoesNotCreateDuplicateImportAndReplacesPlayersOnce` |
| FT-09 | Podpisan avatar URL in potrditev shranjenega avatarja | Član 3 (Ubuntu 22.04) | `StorageUploadServiceTest#createCurrentAvatarUploadUrlGeneratesProfileScopedPath`; `#confirmCurrentAvatarPersistsBackendGeneratedPublicUrlAndPath`; `StorageUploadControllerTest#createCurrentAvatarUploadUrlReturnsSignedUploadContract`; `#confirmCurrentAvatarReturnsPersistedAvatarUrl` |

## Specifikacije testnih scenarijev

### FT-01 – Posodobitev profila z normalizacijo polj

| Polje | Vsebina |
| --- | --- |
| Odgovorni | Član 1 (Windows 11) |
| Cilj / sledljivost | Preveriti, da servis shrani dovoljena polja profila uporabnika, normalizira presledke in kodo države ter ne pošlje nenavedenega polja kot spremembe. `ProfileService.updateCurrentProfile`; `docs/project-current-state-overview.md` – profilni API. |
| Predpogoji / podatki | Avtenticiran profil `CarryOne`, država `SI`; posodobitev: nickname `" SupportTwo "`, bio `"  Draft caller  "`, country `"de"`; display name ni poslan. |
| Koraki in pričakovani rezultat | 1. Pokliči posodobitev profila za trenutnega uporabnika. 2. Preveri ukaz, poslan repozitoriju. Pričakovano: nickname `SupportTwo`, bio `Draft caller`, država `DE`; display name je označen kot nespremenjen. |
| Dejanski rezultat | Normalizirana vrednost in prisotnost polj v ukazu ustrezata pričakovanemu. PASS. |
| Dokaz izvedbe | `ProfileServiceTest#updateCurrentProfileNormalizesPatchFields` – 1 test, brez napake. |

### FT-02 – Ustvaritev ekipe in samodejna nastavitev kapitana

| Polje | Vsebina |
| --- | --- |
| Odgovorni | Član 1 (Windows 11) |
| Cilj / sledljivost | Preveriti normalizacijo vhodnih vrednosti in dodelitev kapetanske vloge ustvarjalcu ekipe. `TeamService.createTeam`; `docs/backend-my-team-api.md`. |
| Predpogoji / podatki | Prijavljen profil z globalno vlogo `player`; ime `"  Ancient Stack  "`, oznaka `" AS "`, regija `"  EU  "`, opis `"  Tier two squad  "`; slug ni podan. |
| Koraki in pričakovani rezultat | 1. Ustvari ekipo. 2. Preveri ukaza za ekipo in člana rostera. Pričakovano: ime `Ancient Stack`, oznaka `AS`, slug `ancient-stack`, regija `EU`; ustvarjalec je kapitan in se doda v roster z vlogo `SUPPORT`. |
| Dejanski rezultat | Vse preverjene vrednosti in kapetanov roster zapis ustrezajo pričakovanemu. PASS. |
| Dokaz izvedbe | `TeamServiceTest#createTeamSetsAuthenticatedUserAsCaptainAndGeneratesSlug` – 1 test, brez napake. |

### FT-03 – Kapitan sprejme prošnjo igralca za pridružitev ekipi

| Polje | Vsebina |
| --- | --- |
| Odgovorni | Član 1 (Windows 11) |
| Cilj / sledljivost | Preveriti, da sprejem čakajoče prošnje spremeni njen status in doda prosilca v roster. To je tok članstva v ekipi; ne gre za prijavo ekipe na turnir. `TeamJoinRequestService.acceptJoinRequest`; `docs/project-current-state-overview.md` – ekipe in join requests. |
| Predpogoji / podatki | Čakajoča prošnja; zahtevo obravnava kapitan ekipe; roster ima prostor; prosilec je igralec brez aktivnega članstva v drugi ekipi. |
| Koraki in pričakovani rezultat | 1. Kapitan sprejme prošnjo. 2. Preveri odgovor in ustvarjen roster zapis. Pričakovano: status `accepted`; prosilec je dodan v ekipo z vlogo `SUPPORT`. |
| Dejanski rezultat | Odgovor vsebuje status `accepted`; ustvarjen je roster zapis za prosilca. PASS. |
| Dokaz izvedbe | `TeamJoinRequestServiceTest#captainCanAcceptJoinRequestAndMemberIsCreated` – 1 test, brez napake. |

### FT-04 – Organizator ustvari osnutek turnirja s privzetimi nastavitvami

| Polje | Vsebina |
| --- | --- |
| Odgovorni | Član 2 (macOS Sonoma) |
| Cilj / sledljivost | Preveriti ustvarjanje turnirja in določitev privzetih vrednosti. Scenarij se konča pri osnutku; ne preverja javnega iskanja ali ogleda turnirja. `TournamentService.createTournament`; `POST /api/organizer/tournaments`. |
| Predpogoji / podatki | Avtenticiran organizator; naslov `"  Mid Wars Open  "`; opis `"  Public qualifier  "`; `startsAt=STARTS_AT`, prijavno okno od `STARTS_AT - 7 dni` do `STARTS_AT - 1 dan`; nekatere nastavitve so izpuščene. |
| Koraki in pričakovani rezultat | 1. Ustvari turnir. 2. Preveri ukaz, poslan repozitoriju. Pričakovano: naslov `Mid Wars Open`, slug `mid-wars-open`, stanje `draft`, format `SINGLE_ELIMINATION`, privzeta kapaciteta ekip in velikost ekipe 5. |
| Dejanski rezultat | Naslov, slug, format, organizator, privzeta kapaciteta in velikost ekipe ustrezajo pričakovanemu. PASS. |
| Dokaz izvedbe | `TournamentServiceTest#organizerCanCreateDraftTournamentWithDefaults` in `TournamentControllerTest#createTournamentReturnsCreatedTournament` – 2 testa, brez napake; controller vrne `201 Created`, ID, lokacijo in stanje `draft`. |

### FT-05 – Lastnik objavi veljaven osnutek turnirja

| Polje | Vsebina |
| --- | --- |
| Odgovorni | Član 2 (macOS Sonoma) |
| Cilj / sledljivost | Preveriti poslovni prehod iz `draft` v `published`, ko objavo zahteva lastnik/organizator. Ne preverja javne strani, kataloga ali iskanja. `TournamentService.publishTournament`; `POST /api/organizer/tournaments/{tournamentId}/publish`. |
| Predpogoji / podatki | Obstoječ osnutek z veljavnimi nastavitvami in datumi; trenutno prijavljen organizator lahko upravlja turnir. |
| Koraki in pričakovani rezultat | 1. Lastnik objavi osnutek. 2. Preveri odgovor storitve in actor kontekst. Pričakovano: vrnjen status je `published`; izvedba beleži avtenticiranega organizatorja kot akterja. |
| Dejanski rezultat | Status je `published`; actor kontekst je uporabljen. PASS. |
| Dokaz izvedbe | `TournamentServiceTest#ownerCanPublishTournament` in `TournamentControllerTest#publishTournamentReturnsPublishedTournament` – 2 testa, brez napake; controller vrne `200 OK` in stanje `published`. |

### FT-06 – Organizator razporedi odobreno ekipo v skupino

| Polje | Vsebina |
| --- | --- |
| Odgovorni | Član 2 (macOS Sonoma) |
| Cilj / sledljivost | Preveriti razporeditev ekipe z že odobreno prijavo v skupino in shranitev seeda. Predpogoji so testni fixture; scenarij ne odda prijave, ne pregleduje prijav in ne izvaja check-ina. `TournamentGroupService.addTeam`; `POST /api/organizer/tournament-groups/{groupId}/teams`. |
| Predpogoji / podatki | Organizator lahko upravlja turnir; skupina in ekipa obstajata; za ekipo obstaja že odobrena prijava; seed `2`; ekipa še ni razporejena. |
| Koraki in pričakovani rezultat | 1. Dodaj ekipo v skupino s seed številko `2`. 2. Preveri odziv in ukaz repozitoriju. Pričakovano: vrnjen je ID ekipe, ID odobrene prijave in seed `2`; shranjena razporeditev pripada izbrani skupini. |
| Dejanski rezultat | Odgovor vsebuje pričakovano ekipo, prijavo in seed; repozitorij prejme pravilen ukaz. PASS. |
| Dokaz izvedbe | `TournamentGroupServiceTest#organizerCanAddApprovedParticipantToGroup` – 1 test, brez napake. |

### FT-07 – Uvoz tekme OpenDota zaključi uspešno in zabeleži statuse

| Polje | Vsebina |
| --- | --- |
| Odgovorni | Član 3 (Ubuntu 22.04) |
| Cilj / sledljivost | Preveriti uspešen življenjski cikel uvoza in normalizacijo testnega payload-a. Klic OpenDota je nadomeščen z vnaprej določenim testnim odgovorom. `MatchImportService.importMatch`; `README.md` – Match import lifecycle; `docs/project-current-state-overview.md`. |
| Predpogoji / podatki | Avtenticiran organizator; testni ID tekme `7894561230`; odjemalec OpenDota vrne vnaprej določen payload; za ta ID še ni uvoza. |
| Koraki in pričakovani rezultat | 1. Zaženi uvoz. 2. Preveri status, dogodke, klic limiterja in normalizirane podatke. Pričakovano: zaporedje statusov `queued` → `processing` → `ready`; OpenDota kliče se enkrat; normalizirani rezultat shrani testne podatke igralcev. |
| Dejanski rezultat | Status je `ready`; dogodki so v pričakovanem zaporedju; limiter in repo tok sta preverjena; normalizacija uspe. PASS. |
| Dokaz izvedbe | `MatchImportServiceTest#importMatchCreatesProcessingRecordFetchesOpenDotaAndStoresReadyPayload` – 1 test, brez napake. |

### FT-08 – Ponovni uvoz po napaki uporabi isti zapis in ne podvoji igralcev

| Polje | Vsebina |
| --- | --- |
| Odgovorni | Član 3 (Ubuntu 22.04) |
| Cilj / sledljivost | Preveriti retry importa v stanju `error` in idempotentno uporabo obstoječega zapisa. `MatchImportService.retryImport`; `POST /api/match-imports/{id}/retry`. |
| Predpogoji / podatki | Za ID tekme že obstaja import v stanju `error` z razlogom rate limit; odjemalec pri ponovnem poskusu vrne veljaven testni payload. |
| Koraki in pričakovani rezultat | 1. Sproži ponovni uvoz. 2. Preveri ID importa, statuse, število normaliziranih igralcev in klice repozitorija. Pričakovano: isti ID importa preide v `processing` in nato `ready`; nov import zapis ni ustvarjen; igralci se zapišejo enkrat. |
| Dejanski rezultat | Ohranjen je isti ID; nov import ni ustvarjen; rezultat je `ready`; normaliziran je en testni zapis igralca. PASS. |
| Dokaz izvedbe | `MatchImportServiceTest#retryExistingErrorImportDoesNotCreateDuplicateImportAndReplacesPlayersOnce` – 1 test, brez napake. |

### FT-09 – Podpisan avatar URL in potrditev shranjenega avatarja

| Polje | Vsebina |
| --- | --- |
| Odgovorni | Član 3 (Ubuntu 22.04) |
| Cilj / sledljivost | Preveriti omejitev podpisa na pot trenutnega profila, varen upload kontrakt in potrditev URL-ja, ki ga izračuna backend. Dejanski prenos datoteke v Supabase ni del testnega okolja. `StorageUploadService`; `docs/backend-storage-upload-api.md`. |
| Predpogoji / podatki | Avtenticiran igralec; ločena testna fixture-a za izdajo URL-ja (`avatar.png`, `image/png`, 1024 B) in potrditev (`profiles/{PROFILE_ID}/avatar.jpg`); profilni ID iz testnega fixture-a. |
| Koraki in pričakovani rezultat | 1. V testu izdaje zahtevaj podpisan URL za PNG. Pričakovano: bucket `avatars`, profilno omejena pot, metoda `PUT`, največja velikost 2 MiB, tip `image/png`; skrivni service-role ključ ni v URL-ju. 2. V ločenem testu potrdi JPG objekt. Pričakovano: servis izračuna javni URL na osnovi bucketa in poti, posodobi profil ter vrne `persisted=true`. |
| Dejanski rezultat | Pot, bucket, metoda, omejitev velikosti in tip so pravilni; skrivni ključ ni razkrit; potrditev uporabi backend URL in posodobi profilni zapis. PASS. |
| Dokaz izvedbe | Dva servisna testa ter `StorageUploadControllerTest#createCurrentAvatarUploadUrlReturnsSignedUploadContract` in `#confirmCurrentAvatarReturnsPersistedAvatarUrl` – 4 testi, brez napake; MockMvc preveri tudi HTTP odgovor API-ja. |

## Zapisnik izvedbe

| Datum | ID-ji | Izvedeni testi | Uspešni | Napake | Status |
| --- | --- | --- | ---: | ---: | --- |
| 3. 11. 2026 | FT-01–FT-09 | 14 izbranih JUnit metod | 14 | 0 | Uspešno |

Končni ciljani zagoni so uporabili selekcijo navedenih servisnih in controller testov. Rezultat Surefire: **14 testov, 0 neuspešnih, 0 napak, 0 preskočenih; oba zagona BUILD SUCCESS**.

Za ponovitev v backend mapi uporabite Maven Wrapper in izberite metode iz zgornje tabele z lastnostjo Surefire `-Dtest=Razred#metoda,...`; uporabite profil `test`. Lokalna izvedba je zaradi omejitve self-attach uporabila Maven 3.9.14, `-DforkCount=0` in Mockito agent; ta nastavitev je vezana na testno okolje.

Prvi poskus controller testov v sandboxu je bil blokiran pri inicializaciji Spring konteksta: `SupabaseImageStorageService` je poskusil vzpostaviti loopback povezavo in okolje je vrnilo `SocketException: Permission denied: connect`. Po dovoljenem ponovnem zagonu z lokalnim loopback dostopom sta oba controller testna razreda uspešno zaključila. Končni rezultat vključuje vse štiri izbrane API/controller teste.

## Sledljivost in omejitve rezultata

| Področje | Scenariji | Pokritje / omejitev |
| --- | --- | --- |
| Profil | FT-01 | Normalizacija zahtevka in poslanega ukaza; brez preverjanja dejanskega zapisa v bazi ali HTTP sloju. |
| Ekipe | FT-02–FT-03 | Ustvarjanje ekipe, kapetanov roster in sprejem prošnje; podatkovna baza je mock. |
| Turnirji in skupine | FT-04–FT-06 | Ustvarjanje/osnutek, objava (servis in MockMvc controller) ter dodelitev ekipe v skupino; brez javne strani, prijave, check-ina ali bracket/result toka. |
| Zunanji podatki | FT-07–FT-08 | Import, normalizacija, lifecycle, retry in idempotentnost z mock OpenDota odgovorom; brez klica pravega OpenDota API-ja. |
| Slike | FT-09 | Servisna validacija in MockMvc pogodba za upload URL/potrditev; brez HTTP prenosa datoteke v Supabase. |

Za zaključek polnega uporabniškega funkcionalnega testiranja je treba scenarije ponoviti prek delujočega frontend/backend okolja z ločeno testno bazo, testnimi računi in dostopom do razvojnega Supabase Storage/OpenDota. To ni bilo potrebno za zgoraj zapisane servisne teste in ni bilo izvedeno.

## Viri

- ISO/IEC/IEEE 29119-3:2021, *Software and systems engineering — Software testing — Part 3: Test documentation*: https://www.iso.org/standard/79429.html
- IEEE SA, *IEEE/ISO/IEC 29119-3-2021*: https://standards.ieee.org/ieee/29119-3/7499/
- Projektna dokumentacija in implementacija v tem repozitoriju, zlasti `docs/project-current-state-overview.md`, API dokumenti pod `docs/` in testi pod `backend/src/test/java/`.
