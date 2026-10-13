# Poročilo za predmet Zagotavljanje kakovosti: DotaOps

## 1. Preverjanje in analiza dokumentacije za namestitev

Projekt DotaOps je aplikacija za organizacijo Dota 2 turnirjev, prijavo ekip, vodenje tekem in analitiko podatkov iz OpenDota. Tehnološki sklad sestavljajo Next.js 16, React 19 in TypeScript na frontend strani, Spring Boot 4 z Javo 21 na backend strani, Supabase PostgreSQL kot baza podatkov, Flyway za migracije, Docker Compose za lokalni zagon ter GitHub Actions za CI preverjanje.

Analiza je pripravljena za tri tipična razvojna okolja ekipe: Windows 11, macOS Sonoma in Ubuntu 22.04.

| Član / okolje | Opazanja pri vzpostavitvi |
|---|---|
| Član 1: Windows 11 | Glavni README vsebuje PowerShell ukaze, npr. `Copy-Item` in `.\mvnw.cmd`, kar je primerno za Windows. Backend README pa uporablja bash ukaze `./mvnw`, zato mora uporabnik na Windows uporabiti `mvnw.cmd`. Potrebna sta Node.js 20+ in Java 21. Docker zagon zahteva Docker Desktop. |
| Član 2: macOS Sonoma | Dokumentacija ne vsebuje ločenih macOS navodil. Ukaze za kopiranje `.env` datotek je treba prevesti v `cp`, Maven wrapper pa se zažene z `./mvnw`. Smiselno je dodati navodila za namestitev Jave 21 in Node.js 20 prek Homebrew ali uradnih namestitvenih paketov. |
| Član 3: Ubuntu 22.04 | CI uporablja Ubuntu, zato je okolje dobro pokrito na ravni GitHub Actions. Lokalno je treba zagotoviti izvršilne pravice za `backend/mvnw`, nameščen JDK 21, Node.js 20+, npm in po potrebi Docker Compose. Dokumentacija omenja tako `docker-compose` kot `docker compose`, zato je priporočljivo poenotiti ukaz glede na novejši Docker Compose plugin. |

Začetno stanje projekta mora vključevati Node.js 20 ali novejši, npm, Java 21, Git, Maven Wrapper, opcijsko Docker, Supabase PostgreSQL povezavo, backend `.env` in frontend `.env.local`. Backend potrebuje vrednosti `SUPABASE_DB_URL`, `SUPABASE_DB_USER`, `SUPABASE_DB_PASSWORD`, `SUPABASE_JWT_SECRET`, `SUPABASE_SERVICE_ROLE_KEY`, po potrebi `STEAM_WEB_API_KEY` in opcijsko `OPENDOTA_API_KEY`. Frontend potrebuje `NEXT_PUBLIC_API_URL`, `NEXT_PUBLIC_SUPABASE_URL`, `NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY` oziroma `NEXT_PUBLIC_SUPABASE_ANON_KEY`.

Glavna neskladja v dokumentaciji so naslednja:

- Korenski README omenja `.env.example`, vendar v pregledani korenski strukturi ta datoteka ni prisotna.
- Repozitorij je v README naveden kot `https://github.com/FilipKn/DotaOps`, pregledan repozitorij pa je `DotaOps/DotaOps`.
- Backend README opisuje starejšo delitev CI na `unit-tests` in `integration-tests`, medtem ko trenutni `backend-ci.yml` uporablja enoten `backend-checks` job z lokalnim PostgreSQL servisom.
- V repozitoriju so prisotne tudi IDE datoteke `.vs`, ki praviloma ne sodijo v verzioniran projekt.
- Navodila za različne operacijske sisteme niso enako podrobna.

Predlagane izboljšave dokumentacije so:

- Dodati dejanski korenski `.env.example`, ki vsebuje vse potrebne spremenljivke brez skrivnosti.
- Poenotiti URL repozitorija v vseh dokumentih.
- Dodati ločene razdelke za Windows, macOS in Linux.
- Jasno ločiti lokalni zagon brez Dockerja in z Dockerjem.
- Dodati preverjalni seznam pred prvim zagonom.
- Posodobiti opis CI glede na dejanske workflow datoteke.
- Iz repozitorija odstraniti lokalne IDE artefakte in jih dodati v `.gitignore`.

## 2. Standardi kodiranja in popravek datoteke

Projekt uporablja dva glavna sklopa standardov kodiranja. V Javi se uporabljajo razredi v PascalCase, metode in spremenljivke v lowerCamelCase, konstante v UPPER_SNAKE_CASE, paketi z malimi črkami ter štiripresledčno zamikanje. Spring komponente uporabljajo constructor injection, DTO/response razredi so ločeni po paketih, testi pa uporabljajo JUnit 5, AssertJ, Mockito in MockMvc.

V TypeScript/React delu so komponente poimenovane v PascalCase, funkcije in spremenljivke v lowerCamelCase, tipi in vmesniki v PascalCase, datoteke pa večinoma v kebab-case. Uporabljajo se TypeScript tipi, `eslint-config-next`, `tsc --noEmit` in strukturirana delitev na `src/app`, `src/components`, `src/lib` in `src/hooks`.

Izbrana datoteka z odstopanji: `backend/src/main/java/si/um/feri/dotaops/backend/analytics/web/PublicAnalyticsController.java`.

Specifična odstopanja:

1. Štirje endpointi imajo skoraj identičen dolg seznam parametrov.
2. Pomožna metoda `filters(...)` sprejme preveč argumentov, kar zmanjšuje berljivost.
3. Importi niso dosledno urejeni.
4. Podpora za camelCase in snake_case parametre je razpršena po vseh endpointih.
5. Koda je funkcionalna, vendar ni optimalno vzdržljiva.

Refaktorirana različica datoteke:

```java
package si.um.feri.dotaops.backend.analytics.web;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.BindParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import si.um.feri.dotaops.backend.analytics.domain.AnalyticsFilters;
import si.um.feri.dotaops.backend.analytics.service.AnalyticsQueryService;
import si.um.feri.dotaops.backend.common.api.ApiResponse;

@Validated
@RestController
@RequestMapping("/api/public/analytics")
public class PublicAnalyticsController {

    private static final int DEFAULT_LIMIT = 10;

    private final AnalyticsQueryService analyticsQueryService;

    public PublicAnalyticsController(AnalyticsQueryService analyticsQueryService) {
        this.analyticsQueryService = analyticsQueryService;
    }

    @GetMapping("/players")
    public ApiResponse<List<PlayerMetricsResponse>> playerMetrics(
            @Valid @ModelAttribute AnalyticsFilterRequest request
    ) {
        return listResponse(request, analyticsQueryService::playerMetrics);
    }

    @GetMapping("/teams")
    public ApiResponse<List<TeamMetricsResponse>> teamMetrics(
            @Valid @ModelAttribute AnalyticsFilterRequest request
    ) {
        return listResponse(request, analyticsQueryService::teamMetrics);
    }

    @GetMapping("/heroes")
    public ApiResponse<List<HeroMetricsResponse>> heroMetrics(
            @Valid @ModelAttribute AnalyticsFilterRequest request
    ) {
        return listResponse(request, analyticsQueryService::heroMetrics);
    }

    @GetMapping("/tournaments")
    public ApiResponse<List<TournamentMetricsResponse>> tournamentMetrics(
            @Valid @ModelAttribute AnalyticsFilterRequest request
    ) {
        return listResponse(request, analyticsQueryService::tournamentMetrics);
    }

    @GetMapping("/tournaments/{tournamentId}")
    public ApiResponse<TournamentMetricsResponse> tournamentMetricsById(
            @PathVariable UUID tournamentId
    ) {
        return ApiResponse.of(analyticsQueryService.tournamentMetrics(tournamentId));
    }

    private <T> ApiResponse<List<T>> listResponse(
            AnalyticsFilterRequest request,
            Function<AnalyticsFilters, List<T>> loader
    ) {
        return ApiResponse.of(loader.apply(request.toFilters()));
    }

    private record AnalyticsFilterRequest(
            UUID tournamentId,
            @BindParam("tournament_id") UUID tournamentIdSnake,
            UUID teamId,
            @BindParam("team_id") UUID teamIdSnake,
            UUID profileId,
            @BindParam("profile_id") UUID profileIdSnake,
            UUID heroId,
            @BindParam("hero_id") UUID heroIdSnake,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @Min(1) @Max(100) Integer limit
    ) {

        private AnalyticsFilters toFilters() {
            return new AnalyticsFilters(
                    firstNonNull(tournamentId, tournamentIdSnake),
                    firstNonNull(teamId, teamIdSnake),
                    firstNonNull(profileId, profileIdSnake),
                    firstNonNull(heroId, heroIdSnake),
                    from,
                    to,
                    limit == null ? DEFAULT_LIMIT : limit);
        }
    }

    private static <T> T firstNonNull(T primary, T secondary) {
        return primary == null ? secondary : primary;
    }
}
```

## 3. Stanje testiranja in zagotavljanja kakovosti

| Aktivnost | Stanje | Pokriti del rešitve | Orodja | Uporaba umetne inteligence in verifikacija |
|---|---|---|---|---|
| Backend enotski testi | Izvedeno | Servisna logika, validacija, avtorizacija, analytics filtri, ekipe, profili, OpenDota integracija | JUnit 5, AssertJ, Mockito, Spring Boot Test | V repozitoriju ni neposrednega dokaza, da so bili testi generirani z UI. Rezultati so preverljivi z `./mvnw clean test`. |
| Backend integracijski testi | Delno izvedeno | Spring kontekst, PostgreSQL/Supabase profil, Flyway migracije | Spring Boot Test, PostgreSQL, Flyway | CI uporablja lokalni PostgreSQL servis. Če se UI uporabi za dopolnitev testov, mora ekipa potrditi rezultate z zagonom integracijskih testov in pregledom migracij. |
| API in varnostni testi | Izvedeno | Javni endpointi, zaščiteni endpointi, JWT, vloge, CORS | MockMvc, Spring Security Test, JUnit | Preverjanje temelji na testih, kot je `SecurityConfigTest`. Rezultati se potrdijo z avtomatskim izvajanjem v CI. |
| Frontend statična analiza | Izvedeno | TypeScript/React koda | ESLint, `eslint-config-next`, TypeScript | Ni dokazov o UI generiranju rezultatov. Preverjanje se izvede z `npm run lint` in `npm run typecheck`. |
| Frontend produkcijska gradnja | Izvedeno | Next.js aplikacija kot celota | `next build`, GitHub Actions | Rezultat se potrdi z uspešno produkcijsko gradnjo v CI in lokalno z `npm run build`. |
| Frontend enotski ali komponentni testi | Načrtovano | UI komponente, obrazci, interakcije | Priporočeno: Vitest/Jest, React Testing Library | Trenutno ni zaznanih testnih skript ali odvisnosti za Jest/Vitest/Playwright. |
| E2E testiranje | Načrtovano | Celotni uporabniški tokovi: prijava, ustvarjanje turnirja, prijava ekipe, uvoz tekme | Priporočeno: Playwright | Če bi UI pomagala pripraviti scenarije, jih mora ekipa ročno pregledati in potrditi z dejanskim izvajanjem v brskalniku. |
| Ročno testiranje | Delno izvedeno | Lokalni zagon, health endpoint, osnovni tokovi aplikacije | Brskalnik, Postman/cURL, Docker Compose | Ročni postopki so opisani, vendar ni formalnega testnega zapisnika. Priporoča se tabela testnih primerov z odgovorno osebo, datumom in rezultatom. |

Splošno stanje QA je dobro na backend strani, kjer obstaja več testov in CI preverjanje. Frontend ima statično analizo, typecheck in produkcijski build, nima pa še formalnih komponentnih ali E2E testov. Priporočljivo je dodati Playwright scenarije za ključne tokove in merjenje pokritosti backend testov z JaCoCo.

## 4. Izjava o uporabi generativnih orodij umetne inteligence

Pri pripravi tega poročila je bilo uporabljeno generativno orodje ChatGPT/Codex kot pomoč pri analizi strukture repozitorija, dokumentacije, konfiguracijskih datotek, testov in CI nastavitev ter pri oblikovanju formalnega slovenskega besedila. Rezultati niso bili uporabljeni nekritično: trditve so bile preverjene s pregledom datotek `README.md`, `backend/README.md`, `frontend/README.md`, `supabase/README.md`, `frontend/package.json`, `backend/pom.xml`, GitHub Actions workflow datotek in izbranih testnih datotek. Pred oddajo mora ekipa poročilo še vsebinsko potrditi z lokalnim zagonom ukazov `npm run lint`, `npm run typecheck`, `npm run build` in `./mvnw clean test`.
