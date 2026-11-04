# Sistemski test: stresni test javnega kataloga

| Podatek | Vrednost |
| --- | --- |
| Izbrani tip | Stresno testiranje |
| Orodje | k6 |
| Stanje | Izvedeno 4. 11. 2026; FAIL pri 40 in 80 VU zaradi p95 nad 1000 ms, okrevanje pri 5 VU uspešno |
| Obseg | `GET /api/public/tournaments?page=0&size=20` prek Spring Boot API-ja in testne PostgreSQL baze |

## Namen

Ugotoviti, kako se javni katalog turnirjev odziva na postopno naraščajočo
obremenitev, pri kateri ravni se začnejo slabšati odzivni časi ali pojavljati
napake ter ali se sistem po zmanjšanju obremenitve povrne. To je **stresni
test**: obremenitev namerno zvišujemo nad običajno raven in iščemo mejo
sprejemljivega delovanja. Test ne spreminja števila instanc, zato ne meri
skalabilnosti.

Vsak virtualni uporabnik ponavlja javni GET kataloga, preveri status HTTP 200
in obliko paginiranega odgovora (`data.items` in `data.page`) ter počaka eno
sekundo pred naslednjo zahtevo.

## Pogoji in profil obremenitve

| Pogoj | Nastavitev |
| --- | --- |
| Testno okolje | Lokalno ali namensko razvojno/demo okolje; produkcija ni dovoljena. |
| Ciljni sistem | Backend na `http://localhost:8080`; za oddaljeno testno okolje uporabite `BASE_URL`. |
| Podatki | Migrirana testna baza; priporočeni so ponovljivi demo podatki po navodilih za [backend demo seed](./backend-demo-seed.md). |
| Najvišja obremenitev | 80 virtualnih uporabnikov (VU); stopnje so izbrane za ta preizkus, ne predstavljajo potrjene produkcijske kapacitete. |
| Trajanje | 9 minut 30 sekund, vključno z rampami in preverjanjem okrevanja. |
| Zunanji sistemi | Test ne kliče OpenDota, Steam ali Supabase Storage. Uporablja samo backend in njegovo konfigurirano bazo. |
| Orodje | k6; ob izvedbi zabeležite različico k6 in aplikacije. |

Profil: pred vsako merjeno stopnjo se število uporabnikov v 30 sekundah zviša,
nato se ciljna obremenitev vzdržuje 1 minuto. Po najvišji stopnji se obremenitev
zniža na nič, nato se ponovno požene 5 VU za preverjanje okrevanja.

| Faza | Obremenitev | Trajanje | Namen |
| --- | ---: | ---: | --- |
| Osnovna | 5 VU | 30 s dvig + 1 min merjenja | Primerjalna izhodiščna meritev |
| Stres 1 | 10 VU | 30 s dvig + 1 min merjenja | Preverjanje prvega povečanja |
| Stres 2 | 20 VU | 30 s dvig + 1 min merjenja | Preverjanje naraščajoče obremenitve |
| Stres 3 | 40 VU | 30 s dvig + 1 min merjenja | Iskanje približevanja meji |
| Stres 4 | 80 VU | 30 s dvig + 1 min merjenja | Najvišja načrtovana obremenitev |
| Razbremenitev | 0 VU | 30 s | Zmanjšanje obremenitve |
| Okrevanje | 5 VU | 30 s dvig + 1 min merjenja | Preverjanje povratka na osnovno raven |

Predpogoj je, da backend in testna baza delujeta ter da endpoint brez prijave
vrne veljaven JSON. Če endpoint ni dosegljiv ali testna baza ni pripravljena,
izvedbo označite kot **BLOCKED** in ne kot uspešno.

## Merjene metrike in kriteriji presoje

V stabilnih enominutnih intervalih (5, 10, 20, 40, 80 VU in okrevanje pri
5 VU) k6 ločeno meri:

| Metrika | Kriterij ustreznosti posamezne stopnje |
| --- | --- |
| Delež neuspešnih HTTP zahtev (`http_req_failed`) | Manj kot 1 % |
| 95. percentil trajanja zahtev (`http_req_duration`) | Manj kot 1000 ms |
| Delež uspešnih preverjanj (`checks`) | Več kot 99 % |
| Oblika odziva | Status 200, polje `data.items` je seznam, paginacijska polja `page`, `size` in `totalElements` so cela števila. |

Meja sprejemljive obremenitve je najvišja zaporedna stopnja, ki izpolni vsa
merila. Prva stopnja, ki katerega od njih ne izpolni, je zabeležena kot
opažena meja pri tem profilu. Če osnovna stopnja ne ustreza, test ne potrjuje
ustreznega delovanja niti pri osnovni obremenitvi. Če merila po razbremenitvi
pri 5 VU niso izpolnjena, je okrevanje neuspešno. Če 80 VU izpolni vsa merila,
je sklep omejen na to, da meja ni bila dosežena do 80 VU.

Kriteriji so namenjeni primerjavi v navedenem testnem okolju in niso SLA za
produkcijo. Med izvedbo spremljajte tudi 5xx odzive, loge, porabo virov
aplikacije in baze ter omejitve testnega računalnika. Pragovi k6 za posamezno
stopnjo lahko povzročijo izhodno kodo testa, ko je meja presežena; pri
stresnem testu je to merilni rezultat, ne razlog za prikrivanje ali brisanje
zabeležene stopnje.

## Izvedba

V korenu repozitorija zaženite:

```sh
k6 run scripts/k6-public-tournaments-stress.js
```

Za drug backend določite osnovni URL brez končne poti `/api`:

```sh
BASE_URL=https://test.example.org k6 run scripts/k6-public-tournaments-stress.js
```

Uporabite samo namensko testno okolje in pred zagonom preverite ciljni URL.
80 sočasnih uporabnikov lahko obremeni skupno razvojno okolje in bazo; izvajalec
naj med testom spremlja vire in ob nenadzorovanih vplivih prekine preizkus.
Testa ne usmerjajte v produkcijo.

## Zapis izvedbe

| Podatek | Rezultat |
| --- | --- |
| Datum in izvajalec | 4. 11. 2026, približno 15:24–15:34; lokalna izvedba |
| Različica aplikacije in k6 | Aplikacija `0.0.1-SNAPSHOT`, commit `89bf595`; k6 v2.3.0 |
| Okolje / `BASE_URL` | `http://localhost:8080`; lokalni backend, uporabnik je potrdil, da gre za namensko testno Supabase okolje; Flyway je bil za ta zagon izklopljen |
| Doseženi VU in število zahtev | Največ 80 VU; 5.136 zahtev; 5.136 iteracij; 10.272 uspešnih preverjanj |
| Metrike po stopnjah (napake, p95, checks) | Glej spodnjo tabelo; HTTP napake 0 % na vseh stopnjah, checks 100 % na vseh stopnjah |
| Najvišja stopnja, ki ustreza kriterijem | 20 VU |
| Prva stopnja, ki ne ustreza kriterijem | 40 VU (p95 = 2.41 s); pri 80 VU p95 = 5.49 s |
| Rezultat okrevanja pri 5 VU | PASS; 0 % HTTP napak, 100 % checks, p95 = 391.10 ms |
| Opažanja / povezava do dokazil | Celoten profil je trajal 9 min 31 s. k6 je ob zaključku zabeležil prekoračena p95 praga pri 40 in 80 VU. Po testu sta health endpoint in frontend vrnila HTTP 200. |
| Sklep (PASS / FAIL / BLOCKED / NOT RUN) | FAIL — profil je dosegel pragove odzivnosti do 20 VU; pri 40 in 80 VU ju ni dosegel |

### Rezultati po merjenih stopnjah

| Stopnja | Zahteve | HTTP napake | p95 | Checks | Presoja |
| --- | ---: | ---: | ---: | ---: | --- |
| Osnovna, 5 VU | 224 | 0 % | 601.64 ms | 100 % | PASS |
| Stres 1, 10 VU | 476 | 0 % | 425.55 ms | 100 % | PASS |
| Stres 2, 20 VU | 795 | 0 % | 761.46 ms | 100 % | PASS |
| Stres 3, 40 VU | 790 | 0 % | 2.41 s | 100 % | FAIL — p95 |
| Stres 4, 80 VU | 818 | 0 % | 5.49 s | 100 % | FAIL — p95 |
| Okrevanje, 5 VU | 242 | 0 % | 391.10 ms | 100 % | PASS |

Skupno je bilo zabeleženih 5.136 zahtev, 0 neuspešnih HTTP zahtev, 10.272/10.272
uspešnih checks ter 8.99 zahtev/s v celotnem profilu. Izmerjena sprejemljiva
meja po kriterijih tega testa je 20 VU; prva neustrezna stopnja je 40 VU.
Preseganje p95 pri 40 in 80 VU ne pomeni HTTP napak, saj so vsi odgovori še
vedno prestali preverjanja statusa in oblike.

Rezultati veljajo samo za ta testni profil in okolje. Poraba CPU/pomnilnika ter
metrike baze niso bile zbrane; rezultat ni produkcijski SLA.
