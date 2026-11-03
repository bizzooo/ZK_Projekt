# PublicTournamentService - Poslovna pravila za javne turnirje

## Namen
Ta dokument opisuje poslovna pravila za javne turnirje, sestavljanje Steam sejnega piškotka in avtentikacijo prek Supabase JWT ali Steam piškotka. Pravila so zapisana tako, da jih je mogoče neposredno preverjati z enotnimi testi.

## PublicTournamentService

### Odgovornost
Storitev:
- pridobi javne podatke o turnirjih,
- pripravi sezname, lestvice, tekme, metrike in razpored,
- vrne nedostopen turnir kot `ni najden`.

### Poslovna pravila
- Pri seznamu mora omejiti neveljavne vrednosti strani in velikosti.
- Pri razporedu mora privzeti ime etape ob praznem vhodu.
- Tekme mora pravilno urediti.
- Če turnir ni dostopen, mora storitev vrniti `ni najden`.

### Pokritost s testi
Testi pokrivajo:
- robne vrednosti paginacije,
- prazen naziv etape,
- vrstni red krogov in tekem,
- izjemo za nedostopen turnir.

## SteamSessionCookieService - Sestavljanje Steam piškotka

### Odgovornost
Storitev sestavi sejni piškotek Steam iz nastavitev.

### Poslovna pravila
Piškotek mora imeti:
- pravilno ime,
- pravilno vrednost,
- domeno,
- pot,
- `HttpOnly`,
- `Secure`,
- `SameSite`,
- življenjsko dobo.

Pri odjavi mora storitev izdati piškotek:
- z istim obsegom,
- z življenjsko dobo `0`,
- tako da brskalnik odstrani obstoječega.

### Pokritost s testi
Testi pokrivajo:
- običajno ustvarjanje,
- `null` ali prazen prostor za domeno,
- pravilno brisanje piškotka,
- preverjanje atributov piškotka.

## SupabaseJwtAuthenticationFilter - Avtentikacijski filter

### Odgovornost
Filter obravnava avtentikacijo prek Bearer JWT ali Steam piškotka.

### Poslovna pravila
- Če je prisoten Bearer JWT, ga filter preveri, poišče profil in nastavi avtentikacijo.
- Če JWT-ja ni, lahko preveri Steam piškotek.
- Neveljaven JWT zavrne zahtevo.
- Neveljaven Steam piškotek počisti in zahtevo nadaljuje brez avtentikacije.
- Če ni poverilnic, filter zahtevo prepusti naprej.

### Pokritost s testi
Testi pokrivajo:
- veljaven JWT,
- veljaven Steam piškotek,
- neveljavno glavo oziroma žeton,
- potekel Steam piškotek,
- zahtevo brez poverilnic,
- varnostni kontekst,
- nadaljevanje verige filtrov,
- klic vstopne točke.

## Razlog za izbiro teh enot
Te enote sem izbral, ker vsebujejo pomembna, preverljiva pravila z jasnimi vhodi in izhodi. Testi hitro odkrijejo regresije pri paginaciji, varnostnih atributih piškotkov in razvejitvah avtentikacije, ne da bi bili odvisni od zunanjih storitev.

## Namerne napake

### PublicTournamentService
- napačna spodnja meja strani: `1` namesto `0`,
- napačna zgornja meja velikosti: `10` namesto `100`.

Posledica je napačna paginacija.

### SteamSessionCookieService
- življenjska doba ustvarjenega piškotka je `ttl - 1 s`,
- brisanje uporablja `maxAge(1)` namesto `0`.

### SupabaseJwtAuthenticationFilter
- odstranjeno je nastavljanje podrobnosti avtentikacije, ki ga testi pričakujejo.

## Spremembe v obstoječih unit testih

### RoleBasedAnalyticsRepositoryTest
Test je preverjal samo:
- ali ustvarjeni SQL vsebuje presledek na določenem mestu,
- ali so bili datumski parametri posredovani.

Tak test bi lahko bil uspešen tudi, če bi poizvedba napačno upoštevala časovne meje.

### DashboardAnalyticsRepositoryIntegrationTest.java
Dodani je integracijski test, ki:
- v bazo vstavi tekme tik pred `from`,
- natanko ob `from`,
- natanko ob `to`,
- nato preveri, da je šteta samo ena.

S tem preveri, da je spodnja meja vključena, zgornja pa izključena: `[from, to)`.



Pri izdelavi unit testov je bil uporabljen Github Copilot vgrajen v IntelliJ IDEA.