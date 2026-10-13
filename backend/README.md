# DotaOps Backend

Spring Boot API za upravljanje turnirjev, prijav ekip, in uvoz podatkov.

## Zagon (lokalno)

1. Nazadnje poskrbite, da imate v korenu projekta datoteko `.env` (kopirajte iz `.env.example`) in nastavite Supabase povezavo:

```
SUPABASE_DB_URL=jdbc:postgresql://...:5432/postgres
SUPABASE_DB_USER=postgres
SUPABASE_DB_PASSWORD=your_password
```

2. Iz terminala v mapi `backend/` zaženite aplikacijo z Maven wrapperjem:

```bash
./mvnw clean package -DskipTests
java -jar target/*.jar
```

Ali za razvoj (live reload):

```bash
./mvnw spring-boot:run
```

3. Aplikacija bo privzeto na `http://localhost:8080` (nastavite `SERVER_PORT` v `.env`, če želite drugače).

## Zagon z Dockerjem

Za gradnjo slike in zagon lokalno:

```bash
docker build -t dotaops-backend ./backend
docker run --env-file .env -p 8080:8080 dotaops-backend
```

Ali uporabite `docker-compose up --build` iz korena projekta (priporočeno, če imate tudi frontend):

```bash
docker-compose up --build
```

## Baza podatkov

Projekt uporablja Supabase Postgres; Flyway migracije se sprožijo ob zagonu aplikacije. Poskrbite, da so v `.env` pravilne vrednosti za `SUPABASE_DB_URL`, `SUPABASE_DB_USER` in `SUPABASE_DB_PASSWORD`.

## Testiranje

Projekt ima dve vrsti testov, ki preverjata različne nivoje aplikacije:

### Lokalni zagon testov

Zaženite vse teste lokalno:

```bash
./mvnw clean test
```

To privzeto zažene samo **unit teste** (profil `test`), ker integration testi zahtevajo Supabase dostop.

### Vrste testov

#### 1. **Unit testi** (profil `test` - H2 baza)

- **Datoteka**: `src/test/java/si/um/feri/dotaops/backend/BasicContextTest.java`
- **Namen**: Preverjajo delovanje aplikacije v izolaciji brez zunanje baze
- **Baza**: H2 (v-memorijska baza, ustvarjena ob zagonu, izbrisana ob zaključku)
- **Trajanje**: Hitra (<<1s)
- **Zagon lokalno**: `./mvnw clean test` (privzeto)
- **Kaj je testirano**: Spring Boot kontekst se uspešno naložen; osnovne komponentne so dostopne

#### 2. **Integracyjski testi** (profil `integration` - Supabase PostgreSQL)

- **Datoteka**: `src/test/java/si/um/feri/dotaops/backend/SupabaseIntegrationTest.java`
- **Namen**: Preverjajo povezavo z resnično Supabase podatkovno bazo in pravilno izvajanje Flyway migracij
- **Baza**: Supabase PostgreSQL (prava baza - **spremembe se shranijo!**)
- **Trajanje**: Počasnejša (~5-10s, odvisno od mrežne povezave)
- **Zagon lokalno**: Avtomatski preskoči, ker ni `SUPABASE_DB_URL` okoljske spremenljivke
- **Zagon z Supabase dostopom**: 
  ```bash
  export SUPABASE_DB_URL=jdbc:postgresql://...
  export SUPABASE_DB_USER=postgres
  export SUPABASE_DB_PASSWORD=...
  ./mvnw clean test -Dspring.profiles.active=integration
  ```
- **Kaj je testirano**: 
  - Povezava do Supabase se uspe vzpostavi
  - Datasource je pravilno konfiguriran
  - Flyway migracije se izvedejo brez napak
  - Spring Boot kontekst se naloži z integracijanim profilom

### Testni profili (application-*.properties)

- **application-test.properties**: H2 v-memorijska baza, Flyway disabled (ker H2 ne potrebuje zunaj), avtomatsko brise tabele
- **application-integration.properties**: Supabase PostgreSQL, Flyway enabled (izvaja migracije), tabele ostanejo za pregled

### Uporaba v CI/CD (GitHub Actions)

V GitHub Actions (`backend-ci.yml`) tečeta dve povsem loči delovni opravili:

1. **unit-tests (vedno zažene)**
   - Potek: `./mvnw clean test` z `test` profilom
   - H2 baza, ni potrebnih skrivnosti
   - Mora uspeti za merge v main vejo

2. **integration-tests (samo s Supabase skrivnostmi)**
   - Potek: `./mvnw clean test -Dspring.profiles.active=integration` z integration profilom
   - Potrebuje: SUPABASE_DB_URL, SUPABASE_DB_USER, SUPABASE_DB_PASSWORD (iz GitHub Actions skrivnosti)
   - Avtomatski preskoči, če skrivnosti niso nastavljene (lokalno razvojno okolje)
   - Mora uspeti za merge v main vejo

### Hitri pregled kaj se dogaja

```
./mvnw clean test
    ├─ BasicContextTest (H2) ✅ PASS (vedno)
    └─ SupabaseIntegrationTest (Supabase) ⏭️ SKIP (brez SUPABASE_DB_URL)

GitHub Actions CI:
    ├─ unit-tests job
    │   ├─ BasicContextTest (H2) ✅ PASS
    │   └─ SupabaseIntegrationTest (Supabase) ⏭️ SKIP
    └─ integration-tests job (s skrivnostmi)
        └─ SupabaseIntegrationTest (Supabase) ✅ PASS (z Supabase dostopom)
```

## Napotki

- Če uporabljate Supabase v oblaku, uporabite povezavo iz Supabase dashboarda.
- Za razvoj je priporočljivo uporabljati lokalno `.env` datoteko in preveriti, da baza dovoljuje povezave od vaše okolice.
