# DotaOps: funkcionalni pregled in dokumentacija testiranja

| Podatek | Vrednost |
| --- | --- |
| Različica dokumenta | 1.1 |
| Stanje | 4. 11. 2026: FT-01 pregledan; FT-02 delno izveden in blokiran zaradi zaprte registracije; FT-03 read-only javni pregled uspešen; 112/112 JUnit PASS |
| Obseg | Funkcionalno ročno testiranje ključnih uporabniških tokov |
| Predloga | Prilagojena dobrim praksam iz ISO/IEC/IEEE 29119-3:2021 |

Dokument je praktična, projektu prilagojena predloga testne dokumentacije. Ne
predstavlja izjave o formalni skladnosti projekta s standardom. Vključuje načrt
testiranja, specifikacije primerov in postopkov, zahteve za testno okolje,
zapis izvedbe, evidenco napak in povzetek testiranja.

## 1. Namen in opis projekta

DotaOps je spletna aplikacija za upravljanje Dota 2 turnirjev. Sestavljajo jo
Next.js/React/TypeScript uporabniški vmesnik, Spring Boot API in baza
Supabase/PostgreSQL. Backend za spremembe sheme uporablja Flyway. Zunanje
integracije vključujejo Steam OpenID, OpenDota in Supabase Storage.

### Ključne funkcionalnosti

| Uporabnik/področje | Funkcionalnosti |
| --- | --- |
| Javni obiskovalec | Ogled kataloga in podrobnosti javnih turnirjev, skupin, lestvic, tekem in bracketov; ogled javne analitike. |
| Igralec in kapetan | Urejanje profila, povezava Steam računa, upravljanje ekipe in rosterja, prijava ekipe na turnir in check-in odobrene prijave. |
| Organizator | Ustvarjanje in objava turnirjev, pregled/odobritev prijav, upravljanje skupin, generiranje bracketov, razporejanje tekem, vnos rezultatov in uvoz OpenDota tekem. |
| Administrator | Pregled revizijske sledi, sinhronizacija referenčnih podatkov junakov, osveževanje analitike in obdelava notification outboxa. |
| Podatki in integracije | Vzdrževanje podatkov v PostgreSQL, validacija poslovnih pravil na backendu, uvoz in normalizacija tekem ter shranjevanje slik prek Supabase Storage. |

## 2. Načrt testiranja

### Cilj in obseg

Preveriti tri ključne uporabniške tokove od uporabniškega vmesnika do
pričakovanega stanja v aplikaciji:

1. javno iskanje in ogled turnirja;
2. prijava ekipe, pregled organizatorja in check-in;
3. javni pregled obstoječega bracketa, rezultatov in napredovanja zmagovalcev (read-only; brez dodatnih računov in sprememb podatkov).

Testi so funkcionalni in ročni. Ne nadomeščajo enotnih, integracijskih,
varnostnih, obremenitvenih ali dostopnostnih testov.

### Vstopni pogoji

- Testno okolje uporablja ločeno lokalno/razvojno/demo bazo; produkcijska baza
  ni dovoljena.
- Frontend, backend in baza so dosegljivi; migracije so uspešno izvedene.
- Testna oseba preveri `http://localhost:8080/api/health`; za FT-02 uporabi
  namenski PLAYER/kapetanski račun. FT-03 uporablja javno stran in ne zahteva
  prijavljenega računa.
- Testni račun FT-02 je povezan z ustreznim profilom in vlogo v Supabase.
- Testni podatki so pripravljeni po predpogojih posameznega scenarija.
- Pred začetkom se zabeležijo različica aplikacije, okolje, datum in izvajalec.

Demo seed lahko pripravi ponovljive podatke za javni pregled turnirjev, ekip,
lestvic in bracketov; navodila so v
[Backend Demo Seed](./backend-demo-seed.md). Seed sam ne ustvari uporabnikov
Supabase Auth, zato demo profili sami po sebi niso prijavni računi. Za
prijavljene tokove je treba uporabiti namenski testni račun in ga povezati z
ustreznim profilom. Seed in reset se izvajata samo na izrecno določeni testni
bazi.

### Izhodni pogoji

- Vsi pričakovani rezultati posameznega scenarija so označeni kot uspešni.
- Za neuspeh ali blokado je izpolnjena evidenca napake z dokazili.
- Nobena odprta napaka, ki preprečuje osnovno uporabo testiranega toka, ni
  zamolčana; povzetek navede njen status in vpliv.
- Po končanem testu je izpolnjen zapis izvedbe in povzetek testiranja.

### Odgovorne osebe

Imena članov ekipe v repozitoriju niso določena. Pred izvedbo je treba
nadomestiti označena mesta z dejanskimi imeni in priimki.

| ID scenarija | Naziv | Odgovorna oseba | Odgovornost |
| --- | --- | --- | --- |
| FT-01 | Javni katalog in podrobnosti turnirja | Testna oseba A — **vpišite ime in priimek** | Izvede korake, shrani dokazila in izpolni zapis. |
| FT-02 | Prijava ekipe, odobritev in check-in | Testna oseba B — **vpišite ime in priimek** | Preveri kapetanski tok; odobritev zahteva organizatorja in je blokirana, če predpogoji niso izpolnjeni. |
| FT-03 | Javni pregled bracketa in napredovanja | Testna oseba C — **vpišite ime in priimek** | Brez prijave pregleda javni bracket; ne spreminja tekem ali rezultatov. |

Odgovorna oseba zabeleži dejanskega izvajalca in potrdi rezultat.

### Prednost in merila

| Prednost | Scenarij | Razlog |
| --- | --- | --- |
| P1 | FT-02 | Prijava in check-in sta ključna za udeležbo ekipe na turnirju. |
| P1 | FT-03 | Napačno prikazan rezultat ali napredovanje zavaja javne obiskovalce. |
| P2 | FT-01 | Javni katalog je glavna vstopna točka za obiskovalce in igralce. |

Za vsak korak se uporabi eden od statusov: **PASS** (pričakovano doseženo),
**FAIL** (odstopanje), **BLOCKED** (izvedba ni mogoča zaradi predpogoja ali
okolja) ali **NOT RUN** (ni izvedeno). Scenarij je PASS samo, če so vsi njegovi
koraki PASS. Nepričakovano odstopanje se zabeleži kot napaka, tudi če ga je
mogoče obiti.

## 3. Testno okolje in podatki

| Postavka | Zahteva |
| --- | --- |
| Okolje | Lokalno ali namensko razvojno/demo okolje; nikoli produkcija. |
| Aplikacija | Frontend `http://localhost:3000`, backend `http://localhost:8080`. |
| Brskalnik | Podprt sodoben brskalnik; v poročilu navedite ime in različico. |
| Uporabniški računi | FT-01 in FT-03: javni obiskovalec brez prijave; FT-02: PLAYER/kapetan; organizator je potreben samo za nadaljevanje FT-02 po oddaji prijave. |
| Podatkovna baza | Migrirana testna PostgreSQL/Supabase baza. |
| Testna ekipa | Za FT-02 ekipa s kapetanom in rosterjem, ki ustreza nastavljeni velikosti ekipe. |
| Testni turnir | FT-02: objavljen/odprt za prijave; check-in omogočen in v aktivnem časovnem oknu. FT-03: javno dostopen turnir z že ustvarjenim bracketom; samo branje. |
| Dokazila | Posnetki zaslona brez gesel, žetonov, zasebnih ključev ali drugih skrivnosti; zabeleženi identifikatorji testnih zapisov. |

Za teste, ki spreminjajo podatke, uporabite enolična imena (na primer
`QA-FT02-<datum>`). FT-03 je samo za branje in uporablja obstoječ javni bracket;
ne ustvarjajte turnirja ali novih zapisov. Pred začetkom zabeležite ID turnirja,
ekipe, prijave in tekem, kjer so na voljo. Po izvedbi testne podatke označite
ali odstranite samo z odobrenim postopkom za testno okolje.

## 4. Specifikacije testnih primerov

### FT-01 — Javni katalog in podrobnosti turnirja

| Polje | Vsebina |
| --- | --- |
| Cilj | Preveriti, da obiskovalec brez prijave najde objavljen turnir in vidi njegove javne podatke. |
| Prednost | P2 |
| Vloga | Javni obiskovalec |
| Predpogoji | Backend in frontend delujeta. V testni bazi je objavljen javni turnir `DotaOps Demo Cup` z demo skupinami, standings, tekmami in bracketom; če teh podatkov ni, je test BLOCKED. |
| Testni podatki | `DotaOps Demo Cup`, njegov slug, javni status ter vnaprej preverjene skupine, tekme, rezultati in bracket iz testne baze. |
| Pričakovani rezultat | Katalog prikaže javni turnir. Podrobna stran prikaže isti turnir in javno dostopne podatke; stran se naloži brez prijave. Vrednosti ustrezajo testni bazi. |

| Korak | Navodilo | Pričakovani rezultat |
| --- | --- | --- |
| 1 | Odprite novo zasebno/anonimno okno brskalnika in pojdite na `/turnirji`. | Stran kataloga se naloži brez zahteve po prijavi. |
| 2 | Poiščite vnaprej dogovorjeni javni turnir. | Turnir je prikazan z ustreznim naslovom in statusom; ni prikazan kot zaseben/neobjavljen zapis. |
| 3 | Odprite podrobnosti turnirja. | URL in naslov ustrezata izbranemu turnirju; prikazani so podatki, ki jih testni zapis vsebuje. |
| 4 | Odprite javne prikaze skupin/lestvice, tekem in bracketa. | Prikazane ekipe, rezultati in razpored se ujemajo s preverjenimi demo podatki. |
| 5 | Osvežite stran in ponovno preverite glavni podatek (naslov ali status). | Podatek ostane pravilen in stran ne zahteva prijave. |

**Opomba za presojo:** če se prikažejo generični/mock podatki, API napaka ali
drugačen turnir, test ni PASS. Zabeležite odgovor API-ja oziroma stanje
backend povezave; ne sklepajte o uspehu samo iz vizualno izpolnjene strani.

### FT-02 — Prijava ekipe, odobritev in check-in

| Polje | Vsebina |
| --- | --- |
| Cilj | Preveriti prijavo ekipe s kapetanskim računom, pregled/odobritev organizatorja in check-in kapetana. |
| Prednost | P1 |
| Vloge | PLAYER/kapetan ekipe in ORGANIZER turnirja |
| Predpogoji | Testni turnir je objavljen in sprejema prijave; prijavno obdobje je odprto; check-in je omogočen in trenutni čas je znotraj check-in okna. Kapetanova aktivna ekipa ima točno zahtevano število članov in še ni prijavljena. Organizator lahko upravlja ta turnir. |
| Testni podatki | Turnir `QA-FT02-<datum>`, ID ekipe, kapetanski račun, organizatorski račun in veljaven kontaktni e-naslov, če ga obrazec zahteva. |
| Pričakovani rezultat | Po oddaji obstaja prijava v stanju `pending`; organizator jo lahko odobri; po odobritvi jo kapetan lahko potrdi s check-inom, pri čemer je prijava označena kot prijavljena na check-in. |

| Korak | Navodilo | Pričakovani rezultat |
| --- | --- | --- |
| 1 | Prijavite se kot kapetan in odprite javno stran testnega turnirja. | Prikaže se možnost prijave ekipe; aplikacija prepozna aktivno ekipo in kapetansko pravico. |
| 2 | Izpolnite zahtevana polja in oddajte prijavo. | Prijava se uspešno shrani, pojavi se potrdilo, status je `pending`; prijava je vidna v razdelku prijav ekipe. |
| 3 | Odjavite se oziroma uporabite ločeno sejo in prijavite kot organizator turnirja. Odprite organizatorski pogled prijav. | Organizator vidi pravkar oddano prijavo z ustrezno ekipo in statusom. |
| 4 | Odobrite prijavo. | Status se spremeni v `approved`; aplikacija ne prikaže napake in sprememba ostane po osvežitvi. |
| 5 | Vrnite se v kapetansko sejo in ponovno odprite stran turnirja/prijave. | Kapetan vidi odobreno prijavo in omogočeno dejanje check-in, če je časovno okno še odprto. |
| 6 | Izvedite check-in. | Aplikacija potrdi uspeh; zapis vsebuje čas check-in oziroma ustrezen prikaz `Checked In`; po osvežitvi je stanje ohranjeno. |

**Negativni kontrolni primer med izvedbo:** če je prijava še `pending`, je check-in
dejanje nedovoljeno/onemogočeno. Ne spreminjajte pričakovanega rezultata zato,
da bi test postal uspešen; morebitno napačno omogočeno dejanje zabeležite kot
napako. Po preverjanju nadaljujte odobritev in glavni tok.

### FT-03 — Javni pregled bracketa in napredovanja

| Polje | Vsebina |
| --- | --- |
| Cilj | Preveriti, da javni obiskovalec vidi zaključene rezultate polfinalov in pravilno napredovanje finalistov; scenarij ne zahteva dodatnih računov in ne spreminja podatkov. |
| Prednost | P1 |
| Vloga | Javni obiskovalec; prijava ni potrebna. |
| Predpogoji | Turnir je javno dostopen in ima bracket z vsaj enim zaključenim predhodnim krogom ter naslednjim krogom. Scenarij je samo za branje. |
| Testni podatki | Javni turnir `DotaOps Demo Cup` (`dotaops-demo-cup`), obstoječi polfinali in finale. |
| Pričakovani rezultat | Javni prikaz pokaže oba zaključena polfinala, rezultate in napredovala finalista; po osvežitvi se prikaz ohrani. Noben turnirski zapis ni spremenjen. |

| Korak | Navodilo | Pričakovani rezultat |
| --- | --- | --- |
| 1 | Brez prijave odprite `/turnirji/dotaops-demo-cup`. | Javno dostopna stran prikaže pravi turnir in status. |
| 2 | Odprite zavihek **Bracket & Advancement**. | Prikažeta se single-elimination bracket velikosti 4 in dva kroga. |
| 3 | Preverite prvi polfinale `R1-M1`. | `Radiant Wolves` premaga `Roshan Hunters` z 2:0; zmagovalec je označen kot napredoval. |
| 4 | Preverite drugi polfinale `R1-M2`. | `Ancient Titans` premaga `Dire Ravens` z 2:1; zmagovalec je označen kot napredoval. |
| 5 | Preverite finale `R2-M1`. | Finalista sta `Radiant Wolves` in `Ancient Titans`; finale je `Scheduled`, rezultat 0:0, čaka na rezultat. |
| 6 | Osvežite stran in ponovno preverite bracket. | Rezultati, statusi in finalista ostanejo enaki; med testom ni bila izvedena nobena mutacija. |

## 5. Navodila za izvajanje in poročanje

1. Pred izvedbo preverite vstopne pogoje in uporabljajte samo namensko testno
   okolje.
2. Vsak scenarij začnite s čistim oziroma dokumentiranim stanjem. Ne spreminjajte
   testnih podatkov med izvedbo, razen kot zahtevajo koraki.
3. Izvedite korake po vrstnem redu. Zabeležite dejanski rezultat takoj po
   vsakem koraku in pripnite dokazilo, če je rezultat FAIL ali BLOCKED.
4. V dokazila vključite čas, stran/korak in identifikator testnega zapisa.
   Odstranite poverilnice, sejne piškotke, žetone in druge skrivnosti.
5. Napako opišite z minimalnimi ponovljivimi koraki, pričakovanim in dejanskim
   rezultatom ter okoljem. Ne spreminjajte ali brišite podatkov zunaj dogovorjene
   testne baze.
6. Ob zaključku izpolnite povzetek. Če je scenarij blokiran zaradi okolja ali
   manjkajočih testnih računov, ga označite BLOCKED, ne PASS.

### Zapis izvedbe

Izvajalec avtomatiziranih testov: Copilot agent; imena človeškega
pregledovalca repozitorij ne določa. Različica aplikacije je `0.0.1-SNAPSHOT`,
commit delovnega drevesa `7f804bc` z necommit-animi popravki.
JUnit je bil izveden 4. 11. 2026 približno 15:55–15:56 z Maven 3.9.9 in JDK
23.0.2. Testi uporabljajo Mockito in ne spreminjajo Supabase podatkov. Backend
je bil lokalni `http://127.0.0.1:8080`, frontend `http://localhost:3000`;
Flyway je bil za lokalni zagon izklopljen. FT-01 je bil pregledan v
VS Code integriranem brskalniku, Chromium 150.0.7871.250. FT-02 je bil
preizkušen z uporabnikovim PLAYER računom v istem brskalniku; registracija ni
bila oddana, ker je prijavno obdobje poteklo. FT-03 je izveden kot read-only
javni ogled, brez novih uporabnikov in brez sprememb Supabase podatkov.

#### Izvedba FT-01 — Javni katalog in podrobnosti turnirja

| Polje | Vrednost |
| --- | --- |
| ID in naziv scenarija | FT-01 — Javni katalog in podrobnosti turnirja |
| Izvajalec / odgovorna oseba | Copilot agent (avtomatizirani testi in brskalniški pregled); človeški izvajalec ni določen |
| Datum in čas začetka / konca | 4. 11. 2026; brskalniški pregled in API smoke test pred 15:55, regresijski JUnit zaključen 15:56:07 |
| Različica ali commit aplikacije | `0.0.1-SNAPSHOT`, commit `7f804bc` + delovni popravki paginacije/piškotka/JWT |
| Okolje in URL | Lokalni frontend `http://localhost:3000`; backend `http://127.0.0.1:8080`; potrjeno namensko Supabase testno okolje, Flyway izklopljen |
| Brskalnik in različica | VS Code integrirani brskalnik, Chromium 150.0.7871.250 |
| Testni podatki/ID-ji zapisov | `DotaOps Demo Cup`, slug `dotaops-demo-cup`, turnir UUID `41391083-d71f-df20-c4f8-74c00046abdf`; javni API je vrnil 38 zapisov v katalogu |
| Končni status scenarija | FAIL — javni podatki so dostopni, vendar je brskalnik zabeležil napako anonimne seje v panelu prijave/ekipe; podrobnosti v koraku 5 |

| Korak | Dejanski rezultat | Status | Dokazilo / referenca napake |
| --- | --- | --- | --- |
| 1. Odpri `/turnirji` brez prijave. | Stran kataloga je vrnila HTTP 200 in prikazala javne turnirje brez preusmeritve na prijavo. | PASS | Integrirani brskalnik `/turnirji`; javni katalog API HTTP 200. |
| 2. Poišči dogovorjeni javni turnir. | Katalog API je vrnil `DotaOps Demo Cup` s statusom `live`; izbran je bil slug `dotaops-demo-cup`. | PASS | `GET /api/public/tournaments?page=0&size=100`; `GET /api/tournaments/dotaops-demo-cup` HTTP 200. |
| 3. Odpri podrobnosti turnirja. | Stran `/turnirji/dotaops-demo-cup` se je naložila brez prijave in prikazala naslov, status Live, format `groups_playoff`, 5/8 prijav in 7 tekem. | PASS | Brskalniški pregled podrobnosti DotaOps Demo Cup. |
| 4. Preglej skupine/lestvico, tekme in bracket. | Prikazana je bila skupina Demo Group A s 4 ekipami in lestvico, seznam 7 tekem ter bracket s 3 tekmami; finalista sta bila Radiant Wolves in Ancient Titans. | PASS | Brskalniški zavihki Groups & Standings, Bracket & Advancement in Schedule & Results. |
| 5. Osveži in preveri vztrajnost javnega prikaza. | Po osvežitvi so javni podatki ostali vidni. Sočasno je frontend zabeležil `AuthSessionMissingError` v panelu prijave/ekipe pri anonimnem obiskovalcu. Javni prikaz ni bil blokiran, vendar nepričakovana napaka pomeni odstopanje. | FAIL | Next.js razvojni dnevnik: napaka v `getCurrentUserProfile` → `loadTeamManagementData` → `TournamentRegistrationPanel`; zahteva ločeno odpravo. |

Regresijski test paginacije, izveden po popravku: zahteva
`page=0&size=20` zdaj vrne `page=0`, `size=20`, 20 elementov (od 38), HTTP
200. `PublicTournamentServiceTest` 5/5 in `PublicTournamentControllerTest`
4/4 PASS.

#### Izvedba FT-02 — Prijava ekipe, odobritev in check-in

| Polje | Vrednost |
| --- | --- |
| ID in naziv scenarija | FT-02 — Prijava ekipe, odobritev in check-in |
| Izvajalec / odgovorna oseba | Copilot agent; uporabnik je določil PLAYER račun; človeško ime ni bilo podano |
| Datum in čas začetka / konca | 4. 11. 2026, približno 16:39–16:43 CET |
| Različica ali commit aplikacije | `0.0.1-SNAPSHOT`, commit `7f804bc` + delovne spremembe |
| Okolje in URL | Lokalni frontend `http://localhost:3000`, backend `http://127.0.0.1:8080`; potrjeno testno Supabase okolje; brez oddanih sprememb |
| Brskalnik in različica | VS Code integrirani brskalnik, Chromium 150.0.7871.250 |
| Testni podatki/ID-ji zapisov | Uporabnikov PLAYER račun (e-pošta zamaskirana); ekipa `team`, kapetan/lastnik `01`, roster 5/5; `DotaOps Demo Open Qualifier` (`dotaops-demo-open-qualifier`); dashboard kaže 0 registracij trenutne ekipe |
| Končni status scenarija | BLOCKED — rok za prijavo 12. 6. 2026 je potekel, obrazec pa javlja manjkajoč ID turnirja; prijava ni bila oddana |

| Korak | Dejanski rezultat | Status | Dokazilo / referenca napake |
| --- | --- | --- | --- |
| 1. Kapetan se prijavi in odpre turnir. | Prijava je uspela; dashboard je prikazal PLAYER/TEAM OWNER in ekipo `team`. Panel prijave je potrdil kapetansko pravico ter roster 5/5. | PASS | Integrirani brskalnik `/dashboard?role=player` in zavihek Team Submission. |
| 2. Kapetan odda prijavo. | Rok v panelu je `Jun 12, 2026, 10:00 PM`, že pretečen. Gumb `Submit Registration` je onemogočen; validacijska matrika hkrati prikazuje `Real tournament backend id is required`, čeprav pravi, da turnir sprejema prijave. Prijava ni bila oddana. | BLOCKED | Panel Team Submission za `dotaops-demo-open-qualifier`; pretečen rok in neizpolnjen pogoj ID-ja; nobena mutacija ni bila poslana. |
| 3. Organizator odpre prijave. | Korak ni izvedljiv, ker nova prijava ni nastala in organizatorski račun ni bil zagotovljen. | BLOCKED | Predpogoj koraka 2 ni izpolnjen; organizer session ni bila uporabljena. |
| 4. Organizator odobri prijavo. | Ni prijave, ki bi jo bilo mogoče odobriti; noben zapis ni bil ustvarjen. | BLOCKED | Brez prijavnega ID-ja; Supabase ni bil spreminjan. |
| 5. Kapetan ponovno odpre status prijave. | Dashboard je pred testom prikazal 0 registracij trenutne ekipe; po blokiranem koraku nova prijava ne obstaja. | BLOCKED | Dashboard podatki in panel prijave; brez novega zapisa. |
| 6. Kapetan izvede check-in. | Check-in ni bil izveden: prijava ni bila odobrena, poleg tega je bil check-in za ta turnir izklopljen. | BLOCKED | Predpogoja `approved` in aktivno check-in okno nista izpolnjena. |

Ločeno od zgornjega UI scenarija je `TournamentRegistrationServiceTest`
uspešno izvedel 22/22 testov poslovnih pravil z mocki. Ti testi ne pomenijo,
da je bila prijava, odobritev ali check-in zapisana v testno bazo.

#### Izvedba FT-03 — Javni pregled bracketa in napredovanja

| Polje | Vrednost |
| --- | --- |
| ID in naziv scenarija | FT-03 — Javni pregled bracketa in napredovanja |
| Izvajalec / odgovorna oseba | Copilot agent; brez dodatnih uporabniških računov |
| Datum in čas začetka / konca | 4. 11. 2026, približno 16:41–16:43 CET |
| Različica ali commit aplikacije | `0.0.1-SNAPSHOT`, commit `7f804bc` + delovne spremembe |
| Okolje in URL | Lokalni frontend `http://localhost:3000`; javna stran `http://localhost:3000/turnirji/dotaops-demo-cup`; samo branje |
| Brskalnik in različica | VS Code integrirani brskalnik, Chromium 150.0.7871.250 |
| Testni podatki/ID-ji zapisov | `DotaOps Demo Cup`, UUID `41391083-d71f-df20-c4f8-74c00046abdf`; tekme `R1-M1`, `R1-M2`, `R2-M1`; javni prikaz |
| Končni status scenarija | PASS — javni bracket in napredovanje sta bila vidna ter sta ostala enaka po osvežitvi; brez sprememb podatkov |

| Korak | Dejanski rezultat | Status | Dokazilo / referenca napake |
| --- | --- | --- | --- |
| 1. Odpri javno stran turnirja. | `/turnirji/dotaops-demo-cup` se je naložil in prikazal `DotaOps Demo Cup`, status `Live`; uporabljen je bil že prijavljeni PLAYER iz FT-02, dodaten račun ali organizatorska vloga nista bila potrebna. | PASS | Integrirani brskalnik, naslov strani in status turnirja. |
| 2. Odpri zavihek Bracket & Advancement. | Javni backend prikaz je pokazal `Single Elimination`, velikost 4, 2 kroga in 2 zaključeni tekmi. | PASS | Brskalniški zavihek Bracket & Advancement. |
| 3. Preveri `R1-M1`. | `Radiant Wolves` – `Roshan Hunters`, BO3, 2:0; `Radiant Wolves advanced`. | PASS | Bracket UI, `R1-M1`. |
| 4. Preveri `R1-M2`. | `Ancient Titans` – `Dire Ravens`, BO3, 2:1; `Ancient Titans advanced`. | PASS | Bracket UI, `R1-M2`. |
| 5. Preveri `R2-M1`. | Finale je `Scheduled`, `Radiant Wolves` proti `Ancient Titans`, BO5, rezultat 0:0, `Awaiting result`. | PASS | Bracket UI, `R2-M1`. |
| 6. Osveži stran in ponovno preveri bracket. | Po osvežitvi je bil bracket ponovno odprt; isti ekipi, rezultati, statusi in finalista so ostali prikazani. Ni bilo izvedeno nobeno zapisovanje ali spreminjanje rezultata. | PASS | Brskalnik po `page.reload()`; ponovno odprt zavihek Bracket & Advancement. |

Ta prenovljeni scenarij preverja samo javni prikaz obstoječih rezultatov in
napredovanja. Ne preverja ustvarjanja bracketa ali organizatorskega vnosa
rezultata. Brskalnik je uporabil že prijavljeno sejo iz FT-02, vendar ogled ni
zahteval novega uporabnika ali organizatorskih dovoljenj. Ločeni
`TournamentBracketServiceTest`, `MatchManagementServiceTest`
in `MatchAdvancementServiceTest` so za poslovna pravila skupaj uspešno izvedli
37/37 testov; obstoječega Demo Cup bracketa nismo spreminjali.

#### Povzetek izvedenih avtomatiziranih testov

| Obseg | Testi | PASS | FAIL | Napake | Preskočeni |
| --- | --- | ---: | ---: | ---: | ---: |
| FT-01 katalog/API | `PublicTournamentServiceTest`, `PublicTournamentControllerTest` | 9 | 0 | 0 | 0 |
| FT-02 prijava/odobritev/check-in | `TournamentRegistrationServiceTest` | 22 | 0 | 0 | 0 |
| FT-03 bracket/rezultat/napredovanje | `TournamentBracketServiceTest`, `MatchManagementServiceTest`, `MatchAdvancementServiceTest` | 37 | 0 | 0 | 0 |
| Povezane regresije | `SteamSessionCookieServiceTest`, `SupabaseJwtAuthenticationFilterTest`, `SteamAuthControllerTest`, `SecurityConfigTest` | 44 | 0 | 0 | 0 |
| **Skupaj** | **10 testnih razredov** | **112** | **0** | **0** | **0** |

Ukaz za ponovitev:

```sh
cd backend
mvn -o -Dtest=PublicTournamentServiceTest,PublicTournamentControllerTest,TournamentRegistrationServiceTest,TournamentBracketServiceTest,MatchAdvancementServiceTest,MatchManagementServiceTest,SteamSessionCookieServiceTest,SupabaseJwtAuthenticationFilterTest,SteamAuthControllerTest,SecurityConfigTest test
```

### Povzetek poročila testiranja

| Podatek | Vrednost |
| --- | --- |
| Obdobje testiranja | 4. 11. 2026; JUnit 15:55–15:56, brskalniški pregled približno 16:39–16:43 CET |
| Različica aplikacije in okolje | `0.0.1-SNAPSHOT`, commit `7f804bc` + delovne spremembe; lokalni frontend/backend in potrjeno testno Supabase okolje |
| Izvedeni scenariji | FT-01 javni prikaz/API; FT-02 delna prijavljena izvedba; FT-03 javni read-only bracket |
| PASS / FAIL / BLOCKED / NOT RUN | 112 JUnit PASS, 0 FAIL; FT-01 UI FAIL zaradi anonimne sejne napake; FT-02 BLOCKED po potrditvi računa, FT-03 PASS za prenovljeni javni pregled |
| Odprte napake in tveganja | `AuthSessionMissingError` pri anonimnem nalaganju prijavnega panela; FT-02 obrazec ima neusklajena validacijska sporočila (pretečen rok, status pravi, da prijave sprejema, manjka ID turnirja), check-in je izklopljen in organizatorski račun ni na voljo. |
| Odstopanja od načrta | FT-02 je bil ustavljen pred mutacijo zaradi pretečene registracije. FT-03 je bil na novo omejen na javni pregled, ki ne potrebuje dodatnih uporabnikov. |
| Sklep: sprejemljivo za namen testiranja? | Javni prikaz bracketa je potrjen; prijava, odobritev in check-in niso potrjeni v UI/E2E. Regresijski testi potrjujejo tri popravljene napake. |
| Pripravil/-a in datum | Copilot agent, 4. 11. 2026 |
| Pregledal/-a in datum | Ni pregledano |

### Evidenca napake

| Polje | Vrednost |
| --- | --- |
| ID napake | FT-01-UI-01 |
| Povezani scenarij/korak | FT-01, korak 5 |
| Povzetek | Javni pogled brez prijave sproži `AuthSessionMissingError` pri nalaganju panela prijave/ekipe. |
| Resnost in prednost | Srednja / P2; javni podatki ostanejo vidni, vendar je zabeležena neobdelana napaka v brskalniku. |
| Okolje, različica, čas | macOS, `0.0.1-SNAPSHOT`, `89bf595` + delovne spremembe; 4. 11. 2026 |
| Predpogoji in testni podatki | Anonimna seja; javni turnir `dotaops-demo-cup`. |
| Koraki za ponovitev | Odpri `/turnirji/dotaops-demo-cup` brez prijave in opazuj razvojni dnevnik/brskalnik. |
| Pričakovani rezultat | Anonimni uporabnik vidi javni turnir; odsotnost seje se obravnava kot običajen anonimni dostop brez neobdelane napake. |
| Dejanski rezultat | Javna stran se prikaže; Next.js dnevnik zabeleži neobdelani `AuthSessionMissingError` prek `getCurrentUserProfile` in `loadTeamManagementData`. |
| Dokazila (brez skrivnosti) | Lokalni Next.js razvojni dnevnik 4. 11. 2026; brez izpisanih skrivnosti. |
| Status napake / odgovorna oseba | Odprto; odgovorna oseba ni določena. |

| Polje | Vrednost |
| --- | --- |
| ID napake | FT-02-UI-01 |
| Povezani scenarij/korak | FT-02, korak 2 |
| Povzetek | Panel prijave kaže, da turnir sprejema prijave, čeprav je rok potekel; hkrati označi ID turnirja kot neveljaven in onemogoči oddajo. |
| Resnost in prednost | Srednja / P2; stanje obrazca je neusklajeno in ne pojasni zanesljivo, zakaj prijava ni mogoča. |
| Okolje, različica, čas | macOS, `0.0.1-SNAPSHOT`, `7f804bc` + delovne spremembe; 4. 11. 2026, približno 16:42 CET |
| Predpogoji in testni podatki | PLAYER/kapetanska seja; `dotaops-demo-open-qualifier`, rok 12. 6. 2026 ob 22:00. |
| Koraki za ponovitev | Odpri Team Submission za `dotaops-demo-open-qualifier` in preglej rok, validacijsko matriko ter stanje gumba. |
| Pričakovani rezultat | Ob pretečenem roku panel jasno prikaže, da je registracija zaprta; razlog za onemogočeno oddajo je skladen s stanjem oziroma je obrazec skrit. |
| Dejanski rezultat | Panel kaže `Tournament can accept registrations` in `Real tournament backend id is required`; `Submit Registration` je onemogočen. Prijava ni bila oddana. |
| Dokazila (brez skrivnosti) | Integrirani brskalnik, zavihek Team Submission; brez poslanega zahtevka za prijavo. |
| Status napake / odgovorna oseba | Odprto; preveriti je treba ID preslikavo demo turnirja in validacijo prijavnega roka. |

## 6. Povezana dokumentacija

- [Pregled trenutnega stanja projekta](./project-current-state-overview.md)
- [Demo podatki in navodila za seed](./backend-demo-seed.md)
- [Načrt in rezultati enotnih testov](./unit-test-plan.md)
- [Stresni test javnega kataloga](./system-stress-test.md)
- [API za ekipe](./backend-my-team-api.md)
- [API za analitiko](./backend-analytics-api.md)
