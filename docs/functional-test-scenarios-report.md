# Funkcionalni testni scenariji in poročilo izvedbe

**Projekt:** DotaOps  
**Dokument:** FT-DOTAOPS-01, različica 2.0  
**Datum priprave:** 3. 11. 2026 · **Datum prenove v black-box obliko:** 4. 11. 2026  
**Obseg:** trije funkcionalni scenariji – po en za vsakega od treh članov ekipe

## 1. Pregled dokumentacije in funkcionalnosti

V repozitoriju je obstoječa dokumentacija, ki opisuje namen, arhitekturo, API-je, podatkovne tokove in trenutno stanje rešitve:

| Dokument | Ugotovitev |
| --- | --- |
| `README.md` | Povzame DotaOps, tehnologije, strukturo in osnovni zagon. |
| `docs/project-current-state-overview.md` | Najbolj celovit pregled implementiranih funkcij, endpointov, podatkovnega modela, testov in omejitev. |
| `docs/reports/Iteracija_1_DotaOps_pregled.md` | Seznam GitHub issue-jev s kratkimi zahtevami (npr. #40 Profile API, #42 Ekipe CRUD). |
| `docs/backend-my-team-api.md` | Opis ekip, članstva in povezane API funkcionalnosti. |
| `docs/backend-analytics-api.md` | Opis analitičnih endpointov in filtrov. |
| `docs/backend-demo-seed.md` | Opis priprave demo podatkov. |

Ključne funkcionalnosti rešitve so: profili in prijava (Supabase, Steam), upravljanje ekip in rosterjev, turnirji in skupine, tekme, uvoz podatkov OpenDota, analitika in shranjevanje slik. Dokumentacija ne vsebuje specifikacije zahtev (SZPO), zato so kot zahteve, kjer obstajajo, navedeni GitHub issue-ji.

## 2. Način oblikovanja in obseg testiranja

Scenariji so zapisani po predlogi, prilagojeni po ISO/IEC/IEEE 29119-3:2021 (specifikacija testnega primera/postopka): identifikacija, aplikacija in verzija, datum, avtor, kratek opis, referenca, zahteve, namen, predpogoji, vhodni podatki, koraki z akcijo in pričakovanim rezultatom, pogoji uspešnega zaključka ter opombe. [ISO – opis standarda](https://www.iso.org/standard/79429.html), [IEEE SA – 29119-3-2021](https://standards.ieee.org/ieee/29119-3/7499/).

Testiranje je **black-box**: tester pozna le zahteve in uporabniški vmesnik, ne pa izvorne kode. Vsi koraki se izvajajo ročno v brskalniku prek frontenda DotaOps, pričakovani rezultati pa opisujejo samo to, kar uporabnik vidi na zaslonu. Notranje delovanje (baza, API klici, servisi) ni predmet preverjanja.

Iz obsega so izpuščeni tokovi, ki jih naloga prepoveduje: (1) javno iskanje in ogled turnirja; (2) prijava ekipe, pregled organizatorja in check-in; (3) generiranje izločilnega razporeda, vnos rezultata in napredovanje zmagovalca.

## 3. Testno okolje in splošni kriterij uspeha

| Lastnost | Vrednost |
| --- | --- |
| Aplikacija / verzija | DotaOps frontend (Next.js) + backend `0.0.1-SNAPSHOT` (Spring Boot 4) |
| Dostop | Brskalnik (Chrome ali Edge, zadnja verzija), frontend na `http://localhost:3000` |
| Baza | Ločena testna baza z demo podatki (`docs/backend-demo-seed.md`) |
| Testni računi | Organizator (`organizer`) in igralec brez ekipe (`player`) – podatki iz demo seed-a |
| Zunanji sistemi | OpenDota mora biti dosegljiv (FT-03) |
| Kriterij uspeha | Vsi koraki se izvedejo, dejanski rezultat ustreza pričakovanemu |

Člani ekipe so označeni kot Član 1, Član 2 in Član 3; pred oddajo oznake zamenjajte z imeni.

## 4. Seznam scenarijev in odgovornosti

| ID | Naziv scenarija | Odgovorni član | Vloga testerja |
| --- | --- | --- | --- |
| FT-01 | Igralec ustvari ekipo in postane njen kapitan | Član 1 | Igralec brez ekipe |
| FT-02 | Organizator objavi osnutek turnirja | Član 2 | Organizator |
| FT-03 | Organizator uvozi tekmo iz OpenDota | Član 3 | Organizator |

## 5. Specifikacije testnih scenarijev

### FT-01 – Igralec ustvari ekipo in postane njen kapitan

| Testni scenarij | **DotaOps-TEST-TestScenarij-FT-01** |
| --- | --- |
| **Aplikacija:** DotaOps · **Verzija aplikacije:** 0.0.1-SNAPSHOT | **Datum scenarija:** 4. 11. 2026 · **Avtor:** Član 1 |
| **Kratek opis** | Ustvaritev nove ekipe; ustvarjalec postane kapitan |
| **Referenca:** stran »Ekipe« (`/ekipe`); `docs/backend-my-team-api.md` | **Zahteve v SZPO:** / (GitHub issue #42 BE/DB-08 Ekipe CRUD) |

**Namen:** Preveriti, da prijavljen igralec brez ekipe lahko ustvari ekipo, da se odvečni presledki v vnosnih poljih odstranijo in da ustvarjalec v ekipi nastopa kot kapitan.

| Predpogoji | Vhodni podatki |
| --- | --- |
| Tester je prijavljen kot igralec, ki ni član nobene ekipe. Ekipa z imenom `Ancient Stack` še ne obstaja. | Team name `"  Ancient Stack  "` (s presledki), Team tag `" AS "`, Slug prazno, Region `"  EU  "`, Description `"  Tier two squad  "`. |

**Scenariji testiranja**

| Korak | Akcija | Pričakovan rezultat |
| --- | --- | --- |
| 1. | V meniju odpri stran **Ekipe**. | Prikaže se stran brez ekipe in gumb **Create Team**. |
| 2. | Klikni **Create Team**. | Prikaže se obrazec s polji Team name, Team tag, Slug, Region in Description. |
| 3. | Vnesi vhodne podatke in klikni **Create Team** pod obrazcem. | Prikaže se sporočilo »Team created successfully.« |
| 4. | Preglej podatke ekipe v pregledu (Overview). | Ime `Ancient Stack`, oznaka `AS`, regija `EU`, opis `Tier two squad` – brez odvečnih presledkov. |
| 5. | Preglej roster ekipe. | Tester je v rosterju in označen kot **Team owner** (kapitan). |
| 6. | Osveži stran (F5). | Ekipa in kapetanska oznaka ostaneta prikazani (podatki so shranjeni). |

**Pogoji uspešnega zaključka**

| Korak | Akcija | Pričakovan rezultat |
| --- | --- | --- |
| 1. | Primerjaj dejanske rezultate korakov 3–6 s pričakovanimi. | Vsi se ujemajo; ni sporočila o napaki. |

**Opombe, komentarji, navodila:** Po izvedbi testno ekipo izbriši ali ponastavi bazo, da je scenarij ponovljiv.

**Rezultat izvedbe:** _datum: ____ · izvedel: ____ · PASS / FAIL · opombe: ____ _

### FT-02 – Organizator objavi osnutek turnirja

| Testni scenarij | **DotaOps-TEST-TestScenarij-FT-02** |
| --- | --- |
| **Aplikacija:** DotaOps · **Verzija aplikacije:** 0.0.1-SNAPSHOT | **Datum scenarija:** 4. 11. 2026 · **Avtor:** Član 2 |
| **Kratek opis** | Objava turnirja, ki je v stanju osnutka |
| **Referenca:** stran »Organizator« (`/organizator`) | **Zahteve v SZPO:** / |

**Namen:** Preveriti, da organizator lahko objavi svoj osnutek turnirja, da se status spremeni iz »Draft« v »Published« in da sprememba ostane shranjena. Scenarij ne preverja javnega kataloga ali iskanja.

| Predpogoji | Vhodni podatki |
| --- | --- |
| Tester je prijavljen kot organizator. Na seznamu obstaja turnir tega organizatorja v stanju **Draft** z izpolnjenimi nastavitvami in datumi v prihodnosti. | Izbran osnutek turnirja (npr. `Mid Wars Open`). |

**Scenariji testiranja**

| Korak | Akcija | Pričakovan rezultat |
| --- | --- | --- |
| 1. | V meniju odpri stran **Organizator**. | Prikaže se »Organizer Tournament Dashboard« s seznamom turnirjev. |
| 2. | Poišči izbrani turnir. | Turnir ima status **Draft**; gumb **Publish** je omogočen. |
| 3. | Klikni **Details**. | Odpre se podrobni pogled turnirja z razdelkom »Quick Actions«. |
| 4. | Klikni **Publish**. | Prikaže se sporočilo »Tournament published successfully.« |
| 5. | Preglej status turnirja. | Status je **Published**; prikazan je datum objave. |
| 6. | Vrni se na seznam in osveži stran (F5). | Turnir ima status **Published**; števec »Published« se je povečal za 1. |

**Pogoji uspešnega zaključka**

| Korak | Akcija | Pričakovan rezultat |
| --- | --- | --- |
| 1. | Primerjaj dejanske rezultate korakov 4–6 s pričakovanimi. | Vsi se ujemajo; ni sporočila o napaki. |

**Opombe, komentarji, navodila:** Objavljenega turnirja ni mogoče vrniti v osnutek; za ponovitev pripravi nov osnutek.

**Rezultat izvedbe:** _datum: ____ · izvedel: ____ · PASS / FAIL · opombe: ____ _

### FT-03 – Organizator uvozi tekmo iz OpenDota

| Testni scenarij | **DotaOps-TEST-TestScenarij-FT-03** |
| --- | --- |
| **Aplikacija:** DotaOps · **Verzija aplikacije:** 0.0.1-SNAPSHOT | **Datum scenarija:** 4. 11. 2026 · **Avtor:** Član 3 |
| **Kratek opis** | Uspešen uvoz podatkov tekme prek Match ID |
| **Referenca:** stran »Dashboard« (`/dashboard`), panel »Match Data Import«; `README.md` (Match import lifecycle) | **Zahteve v SZPO:** / |

**Namen:** Preveriti, da organizator lahko uvozi javno tekmo Dota 2 iz OpenDota z vnosom njenega ID-ja in da aplikacija prikaže potek uvoza do uspešnega zaključka.

| Predpogoji | Vhodni podatki |
| --- | --- |
| Tester je prijavljen kot organizator. Tekma s spodnjim ID-jem v DotaOps še ni bila uvožena. OpenDota je dosegljiv. | Match ID obstoječe javne tekme na OpenDota (npr. `7894561230`). |

**Scenariji testiranja**

| Korak | Akcija | Pričakovan rezultat |
| --- | --- | --- |
| 1. | V meniju odpri stran **Dashboard**. | Prikazan je panel **Match Data Import** s statusom »Idle«. |
| 2. | V polje **Match ID** vnesi ID tekme in klikni **Import**. | Gumb se spremeni v »Importing...«, nato se prikaže sporočilo »Import job accepted by backend.« |
| 3. | Opazuj status uvoza (po potrebi klikni gumb za osvežitev). | Status preide skozi »Queued« in »Processing« do **Ready**. |
| 4. | Preglej podatke uvoza. | Polje »Dota Match« prikazuje vneseni ID; izpolnjena sta »Started« in »Completed«. |
| 5. | Preglej seznam dogodkov uvoza. | Dogodki so prikazani v zaporedju Queued → Processing → Ready, brez napake. |

**Pogoji uspešnega zaključka**

| Korak | Akcija | Pričakovan rezultat |
| --- | --- | --- |
| 1. | Primerjaj dejanske rezultate korakov 2–5 s pričakovanimi. | Vsi se ujemajo; status ni »Error«. |

**Opombe, komentarji, navodila:** Rezultat je odvisen od razpoložljivosti OpenDota. Če se prikaže status »Error« zaradi omejitve zahtev, to zabeleži v opombe in scenarij ponovi kasneje.

**Rezultat izvedbe:** _datum: ____ · izvedel: ____ · PASS / FAIL · opombe: ____ _

## 6. Zapisnik izvedbe

| Datum | ID | Izvedel | Status | Opombe / napake |
| --- | --- | --- | --- | --- |
| | FT-01 | | | |
| | FT-02 | | | |
| | FT-03 | | | |

Za vsak scenarij shranite dokazilo (posnetek zaslona končnega stanja).

## 7. Sledljivost in omejitve

| Področje | Scenarij | Pokritje / omejitev |
| --- | --- | --- |
| Ekipe | FT-01 | Ustvaritev ekipe in kapetanska vloga; povabila in prošnje za pridružitev niso vključeni. |
| Turnirji | FT-02 | Objava osnutka; brez javne strani, prijave, check-ina ali izločilnega razporeda. |
| Zunanji podatki | FT-03 | Uspešen uvoz tekme; ponovni uvoz po napaki ni vključen. |

Scenariji so dopolnjeni z avtomatiziranimi white-box testi v backendu (`TeamServiceTest`, `TournamentServiceTest`, `TournamentControllerTest`, `MatchImportServiceTest`), ki preverjajo isto poslovno logiko na ravni kode. Ti testi niso del tega black-box poročila.

## 8. Popravki v različici 2.0

| Prej | Popravek |
| --- | --- |
| Koraki so preverjali notranjost kode (ukaze repozitoriju, zaporedje klicev, mock objekte) in HTTP klice z JWT. | Scenariji so prepisani v black-box obliko: koraki se izvajajo v uporabniškem vmesniku, pričakovani rezultati opisujejo, kar uporabnik vidi. |
| Rezultati izvedbe so izvirali iz JUnit testov. | Rezultati so izpraznjeni, ker je treba scenarije ročno izvesti v delujoči aplikaciji. |
| Devet scenarijev (različica 1.1). | Trije scenariji, po en na člana. |

## Viri

- ISO/IEC/IEEE 29119-3:2021, *Software and systems engineering — Software testing — Part 3: Test documentation*: https://www.iso.org/standard/79429.html
- IEEE SA, *IEEE/ISO/IEC 29119-3-2021*: https://standards.ieee.org/ieee/29119-3/7499/
- Projektna dokumentacija in implementacija v tem repozitoriju, zlasti `docs/project-current-state-overview.md` in frontend pod `frontend/src/`.
