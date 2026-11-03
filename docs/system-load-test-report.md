# Poročilo sistemskega testiranja – obremenitveni test

**Projekt:** DotaOps  
**Datum izvedbe:** 3. 11. 2026, 22:05–22:06 (Europe/Ljubljana)  
**Odgovorni član:** Član 1 (Windows 11; oznako pred oddajo zamenjajte z imenom)  
**Izbrani tip:** Obremenitveno testiranje (load test)  
**Orodje:** Locust 2.47.0

## Namen in scenarij

Preveriti, ali lokalni backend pri zmernem, pričakovanem prometu ohranja razpoložljivost in odzivnost javnega endpointa `GET /api/health`. Vsak simulirani uporabnik ponavlja zahtevo, preveri HTTP 200 ter vrednosti `status=ok` in `service=dotaops-backend`.

Locust je bil izbran, ker omogoča določitev števila sočasnih uporabnikov, hitrosti njihovega zagona in trajanja testa ter izpis meritev v CSV/HTML. Test je obremenitveni z majhnim profilom; ne gre za stresni test in ne uporablja knjižnice k6. [Locust: izvajanje brez spletnega UI-ja](https://docs.locust.io/en/stable/running-without-web-ui.html), [Locust: izvoz statistike](https://docs.locust.io/en/stable/retrieving-stats.html).

## Pogoji izvedbe

| Nastavitev | Vrednost |
| --- | --- |
| Okolje | Windows 11; lokalna instanca DotaOps backend |
| Profil aplikacije | Spring `test`, H2 v pomnilniku; Flyway izključen |
| Naslov | `http://127.0.0.1:18080` |
| Sočasni uporabniki | 5 |
| Zagon uporabnikov | 1 uporabnik/s; polna obremenitev dosežena po 5 s |
| Čas testa | 60 s |
| Ritem uporabnika | 1 s premora med zahtevami |
| Zahteve | `GET /api/health`; časovna omejitev 3 s |
| Zunanje storitve | Supabase, OpenDota in Steam niso bili klicani |

Scenarij je v [`scripts/system-loadtest/locustfile.py`](../scripts/system-loadtest/locustfile.py). Samodejno označi zahtevo kot neuspešno ob neveljavnem HTTP statusu, neveljavnem JSON-u ali nepričakovanem statusu storitve.

## Merila uspeha

| Metrika | Kriterij |
| --- | --- |
| Neuspele zahteve | največ 1 % vseh zahtev; odgovor in telo morata ustrezati pričakovanemu |
| Latenca p95 | največ 500 ms |
| Prepustnost | najmanj 4 zahteve/s |

## Rezultati

| Metrika | Izmerjeno | Kriterij | Izid |
| --- | ---: | ---: | --- |
| Skupno zahtev | 285 | — | — |
| Neuspele zahteve | 0 (0 %) | ≤ 1 % | Uspešno |
| Povprečna latenca | 3,75 ms | informativno | — |
| Mediana | 3 ms | informativno | — |
| Najnižja / najvišja latenca | 2,30 / 76,02 ms | informativno | — |
| Latenca p95 | 5 ms | ≤ 500 ms | Uspešno |
| Latenca p99 | 6 ms | informativno | — |
| Prepustnost | 4,85 zahtev/s | ≥ 4 zahtev/s | Uspešno |

**Sklep:** V določenem 60-sekundnem profilu je endpoint izpolnil vsa merila. Locust ni zabeležil napak, p95 je bil 5 ms, prepustnost pa 4,85 zahtev/s.

## Obseg in omejitve

Rezultat velja za javni health endpoint lokalnega backend-a in testno okolje H2. Ne predstavlja meritve frontend-a, turnirskih poizvedb, produkcijske baze, OpenDota ali kapacitete sistema pri visoki/stresni obremenitvi. CPU in poraba pomnilnika med testom nista bila merjena.

Locustov CSV in HTML izpis sta nastala v ignorirani mapi `backend/target/system-loadtest/`; agregirane vrednosti so zapisane zgoraj. Testni backend je bil po izvedbi ustavljen.
