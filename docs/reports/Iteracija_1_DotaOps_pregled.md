# DotaOps

## Poročilo 1. iteracije

**Datum:** 13. maj 2026  
**Repozitorij:** `DotaOps/DotaOps`  
**Kratek opis:** DotaOps je spletna platforma za organizacijo Dota 2 turnirjev, upravljanje ekip, pripravo turnirskih tokov in kasnejšo analitiko podatkov iz OpenDota.

**Člani oziroma sodelavci:** Filip Knez, Gal Kovše, Vid Bezget in Luka Četina.

**Meja poročila:** poročilo opisuje opravljeno delo 1. iteracije in začetno projektno osnovo, ki je bila zaključena pred začetkom 2. iteracije.

# 1. Uvod

DotaOps je aplikacija za Dota 2 turnirsko organizacijo. Namen sistema je omogočiti javni pregled turnirjev in ekip, prijavo oziroma upravljanje uporabnikov, ekipne rosterje, kasnejše prijave na turnirje, vodenje tekem in povezavo z OpenDota podatki.

Cilj 1. iteracije ni bil dokončati celotnega turnirskega procesa. Poudarek je bil na tehnični osnovi, podatkovnem modelu, avtentikaciji, profilih, ekipah, osnovnih javnih pogledih in začetni pripravi za zunanje Steam/OpenDota podatke. Tak vrstni red je smiseln, ker turnirski tokovi v 2. iteraciji potrebujejo zanesljive profile, preverjeno identiteto, ekipno lastništvo, rosterje in enoten API dogovor.

Iteracija je zato pripravila temelje: Spring Boot zaledje, Flyway migracije za PostgreSQL/Supabase, Next.js vmesnik, varnostni sloj, API ovojnice, profile, ekipe, povabila, začetni OpenDota/Steam bootstrap in teste. Na tej osnovi se lahko v 2. iteraciji doda življenjski cikel turnirjev brez ponovnega izumljanja identitete, avtorizacije in osnovne podatkovne strukture.

# 2. Struktura projekta in izbrane tehnologije

Projekt je razdeljen na več jasno ločenih delov:

| Del projekta | Namen | Evidenca |
|---|---|---|
| `backend/` | Spring Boot REST API, varnost, poslovna logika, repozitoriji, Flyway migracije in testi. | `backend/README.md`, `backend/pom.xml`, `backend/src/main/java` |
| `frontend/` | Next.js aplikacija z javnimi, ekipnimi, turnirskimi in organizatorskimi pogledi. | `frontend/README.md`, `frontend/package.json`, `frontend/src/app` |
| `backend/src/main/resources/db/migration/` | Nadzorovana zgodovina sheme baze prek Flyway migracij. | migracije `V1` do `V10` za Iteracijo 1 |
| `supabase/` | Supabase konfiguracija in dodatna navodila za projekt. | `supabase/README.md`, `supabase/config.toml` |
| `.github/workflows/` | CI preverjanje backenda, migracij in frontenda. | `backend-ci.yml`, `frontend-ci.yml` |
| `docs/` | Projektna in review dokumentacija. | `docs/code_review.md` |

Uporabljene različice so bile preverjene neposredno v konfiguraciji projekta:

| Tehnologija | Verzija oziroma vir |
|---|---|
| Java | 21, `backend/pom.xml` |
| Spring Boot | 4.0.6, `backend/pom.xml` |
| Next.js | 16.2.4, `frontend/package.json` |
| React | 19.2.5, `frontend/package.json` |
| TypeScript | 6.0.3, `frontend/package.json` |
| PostgreSQL/Supabase | Supabase Postgres, opisano v `README.md` in `supabase/README.md` |
| Flyway | `spring-boot-starter-flyway` in migracije v backendu |

## 2.1 Zakaj PostgreSQL/Supabase

Podatki DotaOps so naravno relacijski: uporabniki imajo profile, profili vodijo ekipe, ekipe imajo člane in povabila, turnirji imajo prijave, prijave se vežejo na ekipe, tekme pa na turnirje in rezultate. PostgreSQL zato ustreza bolje kot shranjevanje samo v pomnilnik ali v nenadzorovan JSON, ker omogoča tuje ključe, unikatne omejitve, transakcije, indekse in `CHECK` pravila.

Supabase je bil izbran kot gostovani PostgreSQL ekosistem, ki lahko poleg baze podpira tudi avtentikacijo, RLS politike in integracijo s projektom. V migracijah se uporabljajo enum tipi, `jsonb`, RLS politike, varnostne funkcije, indeksi, triggerji in materializirani pogledi. To je razvidno iz `V1__initial_dotaops_schema.sql` in `V2__professional_tournament_database.sql`.

`jsonb` se uporablja za polja, kjer je potrebna nadzorovana fleksibilnost, na primer zunanji OpenDota payloadi, metapodatki, nastavitve in audit vrstice. To ni zamenjava za relacijski model, ampak dopolnilo tam, kjer so podatki po naravi variabilni.

## 2.2 Zakaj Spring Boot backend

Spring Boot je primeren za zaledje, ker omogoča strukturiran REST API, zrel varnostni sloj, validacijo vhodnih DTO-jev, transakcije, konfiguracijo okolij in dobro testiranje. V DotaOps je to pomembno, ker poslovna pravila niso trivialna: uporabnik ne sme sam spreminjati vloge, kapetan lahko upravlja samo svojo ekipo, organizatorji imajo ločene pravice, Steam identiteta pa mora biti preverjena na strežniku.

Zaledje uporablja slojni pristop controller -> service -> repository -> domain record -> response DTO. Primeri so `ProfileController`, `ProfileService`, `ProfileRepository` in `ProfileResponse`, podobno pa tudi `TeamController`, `TeamService`, `TeamRepository` in `TeamResponse`. Tak pristop omogoča, da controller ostane tanek, poslovna pravila so v service sloju, repozitorij pa skrbi za SQL in preslikavo podatkov.

## 2.3 Zakaj Next.js frontend

Next.js omogoča sodoben React vmesnik, strežniško in odjemalsko renderiranje, App Router strukturo in dobro organizacijo strani. V projektu je uporabljen za strani, kot so `/dashboard`, `/turnirji`, `/turnirji/[slug]`, `/ekipe`, `/organizator`, `/login` in `/register`.

Frontend je ločen od zaledja prek API plasti v `frontend/src/lib/api.ts` in podatkovnega sloja v `frontend/src/lib/data.ts`. To omogoča, da se UI razvija z začasnimi podatki, nato pa se zamenja z realnimi endpointi, ne da bi bilo treba preoblikovati vse strani.

## 2.4 Zakaj Flyway migrations

Flyway migracije so bile izbrane zato, da je shema baze ponovljiva in sledljiva. Ker je backend lastnik strukture baze, so migracije v `backend/src/main/resources/db/migration/`. To preprečuje nenadzorovano generiranje sheme in omogoča, da vsak razvijalec ali CI okolje ustvari enako bazo iz iste zgodovine migracij.

To je posebej pomembno pri Supabase/PostgreSQL, ker projekt uporablja RLS politike, enum tipe, triggerje, poglede in varnostne funkcije. Teh pravil ni smiselno prepustiti naključnemu ORM generiranju.

# 3. Backend arhitektura

Backend je zgrajen kot klasična slojna aplikacija:

| Sloj | Vloga | Primeri |
|---|---|---|
| Controller | Sprejme HTTP zahtevo, validira DTO in vrne `ApiResponse`. | `ProfileController`, `TeamController`, `TeamRosterController`, `SteamAuthController` |
| Service | Vsebuje poslovna pravila, avtorizacijo in transakcije. | `ProfileService`, `TeamService`, `TeamRosterService`, `SteamAuthService` |
| Repository | Izvaja SQL prek `JdbcTemplate` in preslika vrstice v domain recorde. | `ProfileRepository`, `TeamRepository`, `TeamMemberRepository`, `TeamInvitationRepository` |
| Domain | Predstavlja notranji model podatkov. | `Profile`, `Team`, `TeamMember`, `TeamInvitation`, `MatchImportStatus` |
| DTO | Predstavlja stabilno API pogodbo za frontend. | `ProfileResponse`, `TeamResponse`, `CreateTeamRequest`, `UpdateProfileRequest` |
| Common API/error | Enoten odziv, napake in paginacija. | `ApiResponse`, `GlobalExceptionHandler`, `PageResponse` |

Čeprav je v `pom.xml` prisoten `spring-boot-starter-data-jpa`, poslovni moduli 1. iteracije niso modelirani kot JPA entitete. Repozitoriji uporabljajo `JdbcTemplate`, kar je skladno z DB-first pristopom in Flyway migracijami. Iskanje po kodi ni pokazalo `@Entity` ali `JpaRepository` poslovnih modelov.

Arhitektura je bila izbrana zaradi vzdrževanja:

- Controllerji ostanejo kratki in ne vsebujejo poslovnih pravil.
- Service sloj centralizira avtorizacijo, validacije in lastništvo.
- Repozitoriji ne odločajo o uporabniških pravicah, ampak o podatkovnem dostopu.
- DTO-ji preprečujejo, da bi se notranja struktura baze nenadzorovano izpostavila frontendu.
- Testi lahko ciljajo posamezne sloje brez potrebe po zunanjem API-ju.

Varnostni model uporablja `CurrentUserProvider` in `AuthenticatedActor`. `profileId` je ključen, ker je to stabilna aplikacijska identiteta za igralce, kapetane in ekipe. `authUserId` je lahko `null`, ker Steam-only uporabnik lahko obstaja brez Supabase Auth uporabnika. Zato service sloj ne sme predpostaviti, da je vsaka oseba v sistemu nujno vezana na Supabase UUID.

Podprta sta dva avtentikacijska toka:

- Supabase Bearer JWT, ki ga obdela `SupabaseJwtAuthenticationFilter`.
- Steam session cookie, ki ga isti filter preveri prek `SteamSessionTokenService`.

Service-level avtorizacija je nujna, ker varnost ni odvisna samo od URL pravil. Primer: `TeamService.updateTeam` preveri, ali je trenutni profil kapetan ekipe, organizator ali admin. `TeamRosterService` enako preverja upravljanje rosterja in povabil.

# 4. Podatkovni model in baza

Podatkovni model je bil v 1. iteraciji zastavljen širše kot trenutni UI, ker mora podpirati več kasnejših iteracij. Najpomembnejše migracije za 1. iteracijo so:

| Migracija | Namen | Pomembna vsebina |
|---|---|---|
| `V1__initial_dotaops_schema.sql` | Začetna shema. | Profili, ekipe, člani ekip, turnirji, prijave, tekme, OpenDota importi, junaki, igralci tekem, osnovni enum tipi, indeksi, RLS. |
| `V2__professional_tournament_database.sql` | Profesionalna razširitev modela. | Zunanji računi, tournament staff, skupine, snapshot rosterja prijave, team invitations, match slots, match games, import events, notification outbox, audit log, RLS helperji, analytics pogledi. |
| `V3__harden_auth_and_steam_identity.sql` | Utrditev identitete in Steam toka. | `profile_external_accounts`, `private.steam_login_states`, backend-only helperji za Steam profil, revokacija neposrednega pisanja Steam identitet s strani klienta. |
| `V4` do `V8` | Dodatne izboljšave Steam/RLS toka. | Indeksi, helperji za povezavo Steam računov, zategovanje identitetnih funkcij in popravek RLS za registracije. |
| `V9__add_profile_opendota_bootstrap_fields.sql` | OpenDota bootstrap polja na profilu. | `opendota_account_id`, čas zadnjega Steam/OpenDota synca, čas zadnje napake, unikatni indeks za OpenDota account. |
| `V10__enforce_profile_nickname_uniqueness.sql` | Kakovost profila. | Case-insensitive unikatnost `nickname`. |

`V11__audit_actor_context_fallback.sql` obstaja v trenutni veji, vendar spada v kasnejše delo 2. iteracije in ni štet kot zaključek 1. iteracije.

Glavne skupine tabel:

| Skupina | Kaj shranjuje | Zakaj je pomembno | Omejitve in prihodnja uporaba |
|---|---|---|---|
| `profiles` | Aplikacijski profil, vzdevek, prikazno ime, vlogo, Steam mirror in OpenDota polja. | Osnovna identiteta za igralce, kapetane, organizatorje in admin uporabnike. | Dolžina nickname, unikatnost auth userja, case-insensitive nickname po `V10`, OpenDota account range po `V9`. |
| `profile_external_accounts` | Povezane zunanje identitete, predvsem Steam. | Omogoča več zunanjih identitet in ločitev od javnega mirrorja `profiles.steam_id`. | Unikatnost provider/account, primary account po providerju, backend-only helperji. |
| `teams` | Ekipa, slug, kapetan, regija, opis, logo. | Ekipe so osnovna enota za prijavo na turnir. | Unikaten slug/name, slug format, `captain_profile_id`. |
| `team_members` | Roster ekipe, vloga igralca, aktivnost, `joined_at`, `left_at`. | Roster se skozi čas spreminja, zato je pomembna zgodovina. | Po `V2` je unikatna samo aktivna povezava team/profile, zgodovinski zapisi ostanejo. |
| `team_invitations` | Povabila v ekipo po profilu ali e-pošti, status in predlagana vloga. | Omogoča nadzorovan tok pridruževanja ekipi. | Statusi `pending`, `accepted`, `declined`, `cancelled`, `expired`; unikatna pending povabila. |
| `tournaments` in `tournament_staff` | Osnovni turnirski model, status, datumi, organizator, osebje, nastavitve. | Podlaga za 2. iteracijo. | Status enum, datumske omejitve, `settings jsonb`, RLS helperji za lastnike in staff. |
| `tournament_registrations` in `tournament_registration_members` | Prijave ekip in snapshot rosterja. | Omogoča stabilen roster v času prijave, tudi če se ekipa kasneje spremeni. | Unikatna prijava ekipe na turnir, status prijave, starter snapshot. |
| `matches`, `match_slots`, `match_games` | Serije, sloti bracketov in posamezne Dota igre. | Priprava za Iteracijo 3. | Best-of in score omejitve, časovni red, povezava ekip in zmagovalca. |
| `match_imports`, `match_import_events`, `match_players`, `heroes` | OpenDota uvoz, surovi in normalizirani podatki, junaki in igralčeve metrike. | Priprava za analitiko. | Statusi `queued`, `processing`, `ready`, `error`, unikatni match id, `jsonb` payload. |
| `audit_log` | Operativna sled za spremembe pomembnih tabel. | Pomaga pri sledljivosti sprememb. | Triggerji iz `V2` pišejo insert/update/delete za ekipe, turnirje, registracije, tekme in uvoze. |
| `notification_outbox` | Osnova za kasnejša obvestila. | Priprava za poznejšo stabilizacijo in integracije. | Status dostave in število poskusov. |

# 5. Avtentikacija, uporabniki in profili

V 1. iteraciji sta bila pripravljena dva načina avtentikacije. Supabase JWT tok podpira organizatorje in admin uporabnike prek Bearer tokena. Steam tok podpira igralce, kjer backend začne Steam OpenID prijavo, preveri callback pri Steam endpointu in iz preverjenega `claimed_id` izlušči `steam_id64`.

Pomembna varnostna odločitev je, da backend ne zaupa Steam ID-ju iz frontend JSON payload-a. Steam identiteta se preveri na strežniku v `SteamAuthService`, povezava pa se shrani prek backend helperjev in `profile_external_accounts`.

Profilni API je implementiran v `ProfileController`:

- `GET /api/profiles`
- `GET /api/profiles/{profileId}`
- `GET /api/profiles/by-nickname/{nickname}`
- `GET /api/me/profile`
- `POST /api/me/profile`
- `PATCH /api/me/profile`

Javni endpointi vračajo profilne podatke prek `ProfileResponse`, ne neposrednih DB vrstic. Lastni profil uporablja trenutni varnostni kontekst, zato uporabnik ne more poljubno poslati tujega `authUserId` ali si sam nastaviti vloge.

`ProfileService` normalizira vhodna polja, prepreči podvojen profil za istega Supabase uporabnika in preslika DB constraint napake v uporabne API napake. Vloga profila ni del navadnega uporabniškega update requesta, kar preprečuje samovoljno povišanje pravic.

Steam session je združen z obstoječim modelom uporabnika. `SupabaseJwtAuthenticationFilter` najprej obravnava Bearer JWT, če ga ni, pa preveri Steam session cookie. Steam-only uporabnik ima lahko `profileId`, `steamId` in `role`, `authUserId` pa je lahko prazen. To je razlog, da sta `AuthenticatedActor` in `CurrentUserProvider` ločena od predpostavke, da je vsak uporabnik nujno Supabase uporabnik.

# 6. Ekipe in roster logika

Ekipe so bile v 1. iteraciji implementirane kot realen backend modul. `TeamController` podpira:

- javni seznam ekip,
- branje ekipe po id,
- branje ekipe po slug-u,
- ustvarjanje ekipe,
- posodobitev ekipe.

`TeamService` pri ustvarjanju ekipe uporablja trenutni profil kot kapetana, ne vrednosti, ki bi jo poslal frontend kot lastništvo. Pri posodobitvi preveri, ali je uporabnik kapetan ekipe, organizator ali admin. To je pomembno, ker se dovoljenja ne smejo zanašati samo na frontend ali na to, da uporabnik pošlje pravi `captainProfileId`.

Roster logika je ločena v `TeamRosterController` in `TeamRosterService`. Ločitev je smiselna, ker roster ni isto kot osnovni zapis ekipe. Člani se dodajajo, spreminjajo vlogo ali deaktivirajo, zgodovina pa ostane s polji `is_active` in `left_at`. Vloga člana je nadzorovana z enum vrednostmi, na primer `carry`, `mid`, `offlane`, `support`, `coach` in `substitute`.

Povabila omogočajo nadzorovan vstop v ekipo. Podprti so:

- ustvarjanje povabila,
- pregled povabil ekipe,
- pregled povabil trenutnega uporabnika,
- sprejem,
- zavrnitev,
- preklic.

Ponavljajoča pending povabila so preprečena z bazo in servisno validacijo. Ta del neposredno pripravlja 2. iteracijo, ker se bo ekipa na turnir prijavljala skupaj z rosterjem, ne z ročno vpisanim seznamom igralcev.

# 7. Javni in organizatorski pregledi

V 1. iteraciji sta se razvijala tako backend API kot frontend pogledi.

Na backendu so javno berljivi predvsem profili in ekipe. `SecurityConfig` omogoča javne `GET` klice za profile in ekipe, zaščiteni tokovi pa zahtevajo avtentikacijo za `/api/me/**`, roster/povabila in druge zapisovalne akcije. Protected endpointi uporabljajo tudi service-level avtorizacijo.

Na frontendu so bili pripravljeni začetni pogledi:

| Pogled | Datoteke | Stanje v 1. iteraciji |
|---|---|---|
| Javna začetna stran | `frontend/src/components/home/public-homepage.tsx`, `frontend/src/app/page.tsx` | Dodana v PR #77. |
| Login/register UI | `frontend/src/app/login/page.tsx`, `frontend/src/app/register/page.tsx`, `frontend/src/components/auth/*` | Dodano v PR #77. |
| Turnirski pregled | `frontend/src/app/turnirji/page.tsx`, `frontend/src/app/turnirji/[slug]/page.tsx` | UI in podatkovni sloj pripravljena; realen polni backend lifecycle je predmet 2. iteracije. |
| Ekipe | `frontend/src/app/ekipe/page.tsx`, `frontend/src/components/team-*` | UI uporablja API plast in fallback podatke. |
| Organizator | `frontend/src/app/organizator/page.tsx`, `frontend/src/components/organizer-*` | Prikaz organizatorskega toka, forma pripravljena za kasnejšo povezavo z validacijami in API-jem. |
| Role dashboard | `frontend/src/components/dashboard/*` | Role-based UI iz PR #74. |

Pomembna ugotovitev je, da so frontend issue-ji #8, #9 in #10 na Kanban tabli še odprti oziroma v delu. Zato poročilo ne trdi, da je celotna frontend integracija s pravim API-jem zaključena. Dokazano je, da so bili pripravljeni pogledi, podatkovna plast in mock fallback, polna zamenjava mock podatkov pa je še odprta naloga.

# 8. Steam/OpenDota foundation

V 1. iteraciji je bila izvedena preverjena Steam prijava in začetna OpenDota podlaga.

Steam del vključuje:

- `GET /api/auth/steam/login` za začetek OpenID redirecta,
- `GET /api/auth/steam/callback` za preverjen callback,
- `POST /api/auth/steam/logout` za čiščenje session cookie-ja,
- kratkoživeče state zapise v `private.steam_login_states`,
- backend-only helperje za ustvarjanje oziroma povezovanje Steam profila.

OpenDota del je začetna podlaga, ne polna analitika. Implementirani so `DotaAccountIdConverter`, `OpenDotaClient`, `SteamProfileBootstrapService` in osnovni match import tok. Po Steam loginu backend izračuna 32-bitni OpenDota account id in asinhrono poskusi pridobiti profilne podatke. Če Steam ali OpenDota začasno ne odgovarja, login tok ne sme pasti; napaka se zabeleži, profil pa se lahko vseeno ustvari.

`OpenDotaClient` ima metode za igralca, zadnje tekme in posamezen match. Pri napakah vrača prazen rezultat oziroma prazno zbirko in logira opozorilo, namesto da bi zunanji API neposredno podrl osnovni uporabniški tok.

Statusi OpenDota importov so nadzorovani: `queued`, `processing`, `ready`, `error`. To je zapisano v `dotaops_import_status`, `MatchImportStatus` in migracijah.

Polna OpenDota analitika še ni zaključena. Issue-ji #51, #52, #53, #54 in #55 so na Kanban tabli v kasnejših iteracijah in ostajajo odprti, zato se ta del šteje kot foundation, ne kot končni analytics modul.

# 9. Testiranje v 1. iteraciji

Testiranje je bilo razdeljeno na enotske, controller in integracijske teste.

| Skupina testov | Primeri | Kaj dokazujejo |
|---|---|---|
| API/error/security testi | `GlobalExceptionHandlerTest`, `SecurityConfigTest`, `SupabaseJwtVerifierTest`, `SupabaseAuthoritiesTest` | Enotne napake, 401/403 obnašanje, JWT preverjanje in vloge. |
| Profilni testi | `ProfileServiceTest`, `ProfileControllerTest` | Ustvarjanje/urejanje profila, validacije, javni in lastni endpointi. |
| Ekipni testi | `TeamServiceTest`, `TeamControllerTest`, `TeamRosterServiceTest`, `TeamRosterControllerTest` | CRUD ekip, kapetan, roster, povabila in avtorizacija. |
| Steam testi | `SteamAuthServiceTest`, `SteamOpenIdClientTest`, `SteamSessionTokenServiceTest`, `SteamAuthControllerTest` | Steam OpenID, session token/cookie, invalidni callbacki in varnostni tokovi z mock odzivi. |
| OpenDota testi | `DotaAccountIdConverterTest`, `OpenDotaClientTest`, `MatchImportServiceTest`, `MatchImportStatusTest` | Pretvorba SteamID64 v account id, mock OpenDota odzivi, import statusi. |
| Integracijski testi | `MigrationIntegrationTest`, `DatabasePolicyIntegrationTest`, `ApiFlowIntegrationTest`, `TeamRosterApiIntegrationTest`, `SupabaseIntegrationTest` | Flyway migracije, RLS predpostavke, osnovni API tokovi in Postgres/Supabase okolje. |

Testni profil `test` uporablja H2 in ima Flyway izklopljen, zato je hiter za unit/controller testiranje. Profil `integration` uporablja PostgreSQL/Supabase konfiguracijo, Flyway in okoljske spremenljivke. Integracijski testi so označeni z `@EnabledIfEnvironmentVariable(named = "SUPABASE_DB_URL", matches = ".+")`, zato se lokalno preskočijo, če baza ni konfigurirana.

Primeri ukazov:

```powershell
cd backend
.\mvnw.cmd test "-Dspring.profiles.active=test"
.\mvnw.cmd verify "-Dspring.profiles.active=test"
```

# 10. Pregled nalog iz Kanban/GitHub

Tabela vključuje naloge, ki so bile preverljive iz GitHub issues, Kanban project podatkov, PR-jev, commitov ali kode. Pri odprtih frontend issue-jih je status namenoma označen kot odprt oziroma delno izveden.

| Issue/task number | Title | Short requirement | What was implemented | Main files/modules | Important decision | Status | Evidence/source |
|---|---|---|---|---|---|---|---|
| #1 | Vzpostavitev strukture repozitorija in projektne dokumentacije | Ločiti frontend, backend, Supabase in dokumentacijo. | Repo struktura z `backend/`, `frontend/`, `supabase/`, README navodili. | `README.md`, mape projekta | Ločen frontend/backend/API pristop. | Closed/Narejeno | Issue #1, `README.md` |
| #2 | Začetni Next.js vmesnik z začasnimi DotaOps pogledi | Pripraviti začetne poglede z mock podatki. | Dashboard, turnirji, ekipe, organizator in analitika pogledi z mock/fallback podatki. | `frontend/src/app`, `frontend/src/components`, `frontend/src/lib/mock-data.ts` | UI se lahko razvija pred polno API integracijo. | Closed/Narejeno | Issue #2, PR #64 |
| #3 | Povezava Supabase SSR in brskalniških pomožnih odjemalcev | Pripraviti Supabase helperje za frontend seje. | Browser/server/proxy helperji za Supabase. | `frontend/src/lib/supabase/*`, `frontend/src/proxy.ts` | Session helperji so ločeni od UI komponent. | Closed/Narejeno | Issue #3, README |
| #4 | Začetno Spring Boot zaledje s Flyway | Postaviti backend, Flyway in health check. | Spring Boot aplikacija, Maven Wrapper, health endpoint, Flyway struktura. | `backend/pom.xml`, `BackendApplication`, `HealthController`, migracije | Backend je lastnik sheme baze. | Closed/Narejeno | Issue #4, `backend/README.md` |
| #5 | Začetna DotaOps V1 shema baze | Osnovne tabele, ključi in migracija. | Profili, ekipe, turnirji, registracije, tekme, OpenDota importi, junaki, igralci tekem. | `V1__initial_dotaops_schema.sql` | Podatkovna integriteta v bazi, ne samo v aplikaciji. | Closed/Narejeno | Issue #5, migracija V1 |
| #35 | Profesionalna V2 Supabase shema | Razširiti bazo za turnirje, analitiko in audit. | Staff, skupine, roster snapshoti, povabila, match games, audit, RLS helperji, analytics pogledi. | `V2__professional_tournament_database.sql` | Pripraviti model za kasnejše iteracije brez ponovne zasnove. | Closed/Narejeno | Issue #35, migracija V2 |
| #36 | BE/DB-01 Preveri V2 migracijo | Preveriti ponovljivost migracij. | CI in integration smoke pristop za migracije. | `.github/workflows/backend-ci.yml`, `MigrationIntegrationTest` | Migracije se preverjajo na čisti PostgreSQL bazi. | Closed/Narejeno | Issue #36, PR #68 |
| #38 | BE/DB-03 Skupni API contract | Enoten response/error format in paginacija. | `ApiResponse`, `GlobalExceptionHandler`, `PageResponse`, validacijske napake. | `common/api`, `common/error`, `common/pagination` | Frontend dobi stabilno API obliko. | Closed/Narejeno | Issue #38, commit `2d0624e` |
| #39 | BE/DB-04 Supabase JWT filter | Preverjanje JWT in uporabniški kontekst. | JWT verifier, auth filter, profile lookup, vloge in JSON 401/403 odzivi. | `auth/*`, `SecurityConfig`, `CurrentUserProvider` | Service sloj bere trenutnega actorja, ne request body identitete. | Closed/Narejeno | Issue #39, `SecurityConfigTest` |
| #40 | BE/DB-07 Profile API | Lasten profil in javno branje profilov. | Profile controller/service/repository/DTO, validacije in testi. | `profile/*`, `ProfileControllerTest`, `ProfileServiceTest` | Vloga se ne spreminja prek uporabniškega profila. | Closed/Narejeno | Issue #40, PR #75 |
| #42 | BE/DB-08 Ekipe CRUD | Ustvarjanje, urejanje in javni pregled ekip. | Team controller/service/repository/DTO, slug pravila, kapetan. | `team/domain`, `team/repository`, `team/service`, `team/web` | Kapetan se vzame iz trenutnega profila. | Closed/Narejeno | Issue #42, PR #75 |
| #43 | BE/DB-09 Roster in povabila | Članstva, vloge in invitations. | Aktivni člani, deaktivacija, pending povabila, accept/decline/cancel. | `TeamRosterService`, `TeamRosterController`, `TeamInvitationRepository` | Roster je ločen od ekipe zaradi zgodovine. | Closed/Narejeno | Issue #43, PR #75 |
| #70 | BE/DB-29 Steam OpenID login | Server-side Steam OpenID flow. | Login redirect, callback, OpenID verification, SteamID64 extraction, mock testi. | `auth/steam/*`, `SteamAuthController` | Frontend Steam ID ni zaupanja vreden vir identitete. | Closed/Narejeno | Issue #70, PR #75 |
| #71 | BE/DB-30 Steam session + JWT kontekst | Združiti Steam cookie in Supabase JWT tok. | Filter podpira Bearer JWT in Steam session cookie, `AuthenticatedActor` model. | `SupabaseJwtAuthenticationFilter`, `SteamSessionTokenService`, `AuthenticatedActor` | `authUserId` je lahko `null`; `profileId` ostane ključna identiteta. | Closed/Narejeno | Issue #71, PR #75, PR #79 |
| #72 | BE/DB-31 Steam/OpenDota bootstrap | Po loginu pripraviti Steam/OpenDota podatke. | Dota account id converter, OpenDota client, async bootstrap, profilna polja. | `opendota/*`, `SteamProfileBootstrapService`, `V9__add_profile_opendota_bootstrap_fields.sql` | Zunanji API ne sme blokirati prijave. | Closed/Narejeno | Issue #72, PR #76 |
| #60 | BE/DB-06 Integracijski testi | Pokriti migracije, RLS in API tokove. | Integration profil, Postgres helperji, migration/RLS/API testi. | `backend/src/test/java/.../integration` | Realna PostgreSQL integracija je ločena od hitrega H2 test profila. | Closed/Narejeno | Issue #60, PR #68 |
| #61 | BE/DB-05 Backend CI | Test, build in migration smoke v GitHub Actions. | Backend CI z Maven testi, package in PostgreSQL service za integration teste. | `.github/workflows/backend-ci.yml` | CI mora ujeti migracijske napake pred merge. | Closed/Narejeno | Issue #61, workflow |
| PR #64 | Sprint 1 frontend design system | Osnovni UI paket in pogledi. | `dashboard`, `analitika`, `turnirji`, `ekipe`, `organizator`, data fallback. | `frontend/src/app`, `frontend/src/components`, `frontend/src/lib/data.ts` | UI in data layer pripravljena za kasnejši API priklop. | Merged | PR #64 |
| PR #74 | Role-based dashboard UI | Različni dashboard pogledi po vlogah. | Player/captain/organizer/public dashboard komponente. | `frontend/src/components/dashboard/*` | UI loči uporabniške vloge, backend role vir pride kasneje. | Merged | PR #74 |
| PR #77 | Public homepage, login/register UI | Javna stran in auth obrazci. | Public homepage, login/register strani, `frontend/src/lib/auth.ts`. | `frontend/src/components/home`, `frontend/src/components/auth`, `frontend/src/app/login`, `frontend/src/app/register` | Login/register UI je pripravljen brez razkrivanja server-side skrivnosti. | Merged | PR #77 |
| #8/#9/#10 | FE profili, ekipe in real API povezava | Frontend povezati z realnim API-jem. | Del UI in data layer je pripravljen, vendar issues ostajajo odprti. | `frontend/src/lib/api.ts`, `frontend/src/lib/data.ts`, frontend strani | Poročilo tega ne šteje kot popolnoma zaključeno integracijo. | Open/V delu | Kanban project, issues #8, #9, #10 |

# 11. Glavne tehnične odločitve

| Decision | Alternatives considered | Chosen approach | Reason | Impact on future iterations |
|---|---|---|---|---|
| PostgreSQL/Supabase kot baza | Pomnilnik, lokalni JSON, NoSQL brez relacij. | Supabase Postgres. | Relacije, transakcije, RLS, indeksi in constraints so ključni za turnirje, ekipe in rezultate. | Zanesljiva osnova za prijave, tekme, audit in analitiko. |
| Spring Boot backend | Samo Supabase client iz frontenda, lažji Node API. | Spring Boot 4, Java 21. | Potrebna so jasna poslovna pravila, validacija, varnost, transakcije in testi. | Lažje dodajanje turnirskih workflowov v service sloju. |
| Next.js frontend | Klasični React SPA ali server-rendering brez App Routerja. | Next.js App Router. | Primeren za sodoben, odziven UI in ločen API pristop. | Javni in organizatorski pogledi se lahko razvijajo neodvisno od zaledja. |
| Flyway migracije | Ročne spremembe v Supabase UI, ORM schema generation. | DB shema prek Flyway v backendu. | Shema je ponovljiva, pregledna in primerna za CI. | Ekipa lahko varno dodaja migracije za turnirje in analitiko. |
| Slojna backend arhitektura | Poslovna logika v controllerjih, neposredni SQL iz controllerjev. | Controller -> service -> repository -> domain -> DTO. | Ločitev odgovornosti in lažje testiranje. | Enak vzorec se uporablja za turnirje, registracije in rezultate. |
| DTO-based API contract | Izpostavljanje DB modela ali entitet. | Request/response DTO-ji in `ApiResponse`. | Stabilna pogodba za frontend, manj uhajanja internih polj. | API se lahko razvija brez razkritja audit/security struktur. |
| Service-level authorization | Samo `SecurityConfig` URL pravila. | Dodatni checks v service sloju. | Lastništvo ekipe ali rosterja je poslovno pravilo, ne samo URL pravilo. | Pri turnirjih bo enako mogoče preverjati owner/staff/admin. |
| `profileId` kot identiteta igralca | Samo `authUserId` iz Supabase. | `profileId` je primaren za igralce/ekipe; `authUserId` je lahko nullable. | Steam-only uporabniki niso nujno Supabase users. | Registracije, rosterji in OpenDota povezave ostanejo stabilne. |
| `JdbcTemplate` za poslovne repozitorije | JPA entitete in repositoryji. | Ročni SQL z `JdbcTemplate`. | Projekt je DB-first, migracije so vir resnice, SQL lažje sledi RLS/JSONB/enum potrebam. | Manj tveganja za nenadzorovano shemo in primernejše za kompleksne poizvedbe. |
| Zunanji API izoliran za client/service | Klici v controllerjih. | `SteamOpenIdClient`, `OpenDotaClient`, bootstrap service. | Timeouti, mock testi in failure handling so centralizirani. | Analytics in importi se lahko razširijo brez sprememb v controllerjih. |
| Testi in CI | Ročno testiranje po merge. | H2 test profil, integration profil in GitHub Actions. | Hitri lokalni testi in realnejše PostgreSQL preverjanje. | Migracije in RLS pravila se preverijo pred večjimi iteracijami. |
| RLS in DB constraints | Vsa pravila samo v aplikaciji. | Kombinacija service pravil, constraints, indeksov in RLS. | Napake se ujamejo tudi, če aplikacijski tok ni edini pisec podatkov. | Večja varnost pri kasnejših admin, staff in tournament tokovih. |

# 12. Kaj je pripravljeno za 2. iteracijo

Prva iteracija je pripravila več ključnih temeljev za turnirske funkcionalnosti:

- Profili obstajajo in imajo jasno vlogo.
- Supabase JWT in Steam session uporabniki so združeni v en varnostni model.
- `profileId` je stabilna identiteta za rosterje, ekipe in kasnejše prijave.
- Ekipe, kapetani, člani in povabila so implementirani.
- Roster ima zgodovino aktivnih/neaktivnih članov.
- Baza že vsebuje turnirje, staff, registracije, snapshot rosterja, datumske omejitve in audit osnovo.
- API uporablja enoten response/error format in paginacijo.
- Testi pokrivajo auth, profile, ekipe, rosterje, migracije in RLS predpostavke.
- Frontend ima osnovne public/organizer poglede in API wrapper z mock fallbackom.

Zato se lahko v 2. iteraciji doda življenjski cikel turnirja, prijave ekip in organizatorski tokovi na obstoječe temelje, namesto da bi bilo treba najprej reševati identiteto, lastništvo ekip ali osnovne DB constraints.

# 13. Znane omejitve in tveganja

| Tveganje ali omejitev | Dokaz oziroma razlog | Vpliv |
|---|---|---|
| Frontend issues #8, #9 in #10 so še odprti. | Kanban project jih prikazuje kot odprte oziroma v delu. | Polna frontend povezava z realnim API-jem ni zaključena za vse poglede. |
| Frontend še uporablja mock fallback. | `frontend/src/lib/api.ts` in `frontend/src/lib/data.ts`. | UI deluje tudi brez backenda, vendar lahko prikazuje demo podatke. |
| Integracijski testi so odvisni od okolja. | `@EnabledIfEnvironmentVariable` za `SUPABASE_DB_URL`. | Lokalno se preskočijo, če ni konfigurirane PostgreSQL/Supabase baze. |
| OpenDota analitika ni zaključena. | Issues #51 do #55 so odprti v kasnejših iteracijah. | Obstaja foundation, ne pa končni analytics dashboard/API. |
| Turnirski lifecycle ni del 1. iteracije. | Issue #44 je označen kot Iteracija 2. | 1. iteracija pripravi bazo in temelje, ne celotnega tournament CRUD toka. |
| Bracket, scheduling, result entry in progression so prihodnje delo. | Issues #47 do #50 so v Iteraciji 3 in odprti. | Podatkovni model obstaja, poslovna logika še ni zaključena v 1. iteraciji. |
| Team members list ni ročno dokumentiran v README. | GitHub repo metadata in `git shortlog` sta edina preverjena vira. | Seznam sodelavcev v naslovnici je označen kot GitHub evidenca. |

# 14. Zaključek

V 1. iteraciji je DotaOps iz začetne ideje prešel v strukturiran projekt z ločenim frontend in backend delom, migrirano PostgreSQL/Supabase bazo, varnostnim slojem, profilnim modelom, ekipami, rosterji, povabili, začetnim Steam/OpenDota povezovanjem, osnovnimi javnimi in organizatorskimi pogledi ter testno/CI osnovo.

Najpomembnejši rezultat iteracije ni posamezen ekran, ampak arhitekturna osnova. Projekt ima dogovorjen API format, service-level avtorizacijo, DB-first migracije, profilno identiteto, ekipno lastništvo in prve integracijske teste. To zmanjšuje tveganje pri 2. iteraciji, kjer se na obstoječe gradnike dodajajo turnirji, prijave ekip in organizatorski workflow.

Naslednji korak je nadaljevanje Iteracije 2: turnirski lifecycle, prijave ekip, check-in, staff pravice in povezava frontend obrazcev z realnimi backend endpointi.

# Viri in preverjena evidenca

| Vir | Kaj je bilo uporabljeno |
|---|---|
| `docs/code_review.md` | Backend review kriteriji za API, varnost, DB, OpenDota in teste. |
| `README.md`, `backend/README.md`, `frontend/README.md`, `supabase/README.md` | Opis projekta, tehnologije, lokalni zagon, Supabase in migracije. |
| `backend/pom.xml`, `frontend/package.json` | Dejanske verzije Spring Boot, Java, Next.js, React in TypeScript. |
| Flyway migracije `V1` do `V10` | Podatkovni model 1. iteracije in foundation za kasnejše iteracije. |
| Backend paketi `auth`, `profile`, `team`, `opendota`, `common` | Implementirana arhitektura, varnost in poslovni moduli. |
| Frontend `src/app`, `src/components`, `src/lib` | Pripravljeni pogledi, API wrapper in mock/API data layer. |
| `.github/workflows/backend-ci.yml`, `.github/workflows/frontend-ci.yml` | CI za backend, migracije in frontend. |
| GitHub issues in Kanban project | Iteracija 1 milestone, zaprte backend naloge, odprte frontend naloge, statusi. |
| PR #64, #74, #75, #76, #77, #78, #79, #80 | Implementacijska evidenca za frontend, backend, Steam/OpenDota, teste in izboljšave. |
| `git log --oneline --decorate --max-count=80` | Časovni kontekst commitov in začetek 2. iteracije. |
