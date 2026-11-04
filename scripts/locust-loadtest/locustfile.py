"""
Obremenitveni test (load test) za DotaOps backend z orodjem Locust.

Scenarij: anonimni obiskovalci brskajo po javnem delu aplikacije
(seznam turnirjev, podrobnosti turnirja, ekipe, analitika, junaki).
Vsak simuliran uporabnik med koraki "razmišlja" 1-3 s, kar ustreza
realnemu brskanju, in ne generira maksimalnega možnega prometa
(to bi bil stresni test).

Profil obremenitve (razred LoadProfile):
  0:00-1:00  postopno naraščanje do 50 sočasnih uporabnikov (1 uporabnik/s)
  1:00-9:00  stabilna pričakovana obremenitev 50 uporabnikov
  9:00-10:00 postopno zmanjševanje na 0

Zagon (iz mape scripts/locust-loadtest, backend mora teči na :8080):
  python -m locust -f locustfile.py --host http://localhost:8080 --headless ^
      --csv results/load --csv-full-history --html results/load-report.html

Skripta kliče samo DotaOps backend (ki bere iz Supabase Postgres);
OpenDota/Steam se ne kličeta.
"""

import os
import random

from locust import HttpUser, LoadTestShape, between, events, task

# Privzete vrednosti ustrezajo profilu iz poročila; z okoljskimi
# spremenljivkami jih je mogoče zmanjšati za hiter "smoke" zagon.
TARGET_USERS = int(os.environ.get("LOAD_USERS", 50))
RAMP_UP_S = int(os.environ.get("LOAD_RAMP_UP_S", 60))
STEADY_S = int(os.environ.get("LOAD_STEADY_S", 480))
RAMP_DOWN_S = int(os.environ.get("LOAD_RAMP_DOWN_S", 60))
REQUEST_TIMEOUT_S = 10

# Seznami identifikatorjev, ki jih pridobimo enkrat ob zagonu testa,
# da uporabniki odpirajo resnične turnirje in ekipe iz baze.
TOURNAMENT_IDS: list[str] = []
TOURNAMENT_SLUGS: list[str] = []
TEAM_SLUGS: list[str] = []


@events.test_start.add_listener
def load_reference_data(environment, **_kwargs):
    """Prebere ID-je turnirjev in ekip, preden se zaženejo uporabniki."""
    import requests

    host = environment.host.rstrip("/")
    # Strani so oštevilčene od 1, velikost strani je na strežniku fiksna (10).
    page, total_pages = 1, 1
    while page <= total_pages:
        body = requests.get(f"{host}/api/public/tournaments", params={"page": page}, timeout=15).json()
        TOURNAMENT_IDS.extend(item["id"] for item in body["data"]["items"])
        total_pages = body["data"]["page"]["totalPages"]
        page += 1

    body = requests.get(f"{host}/api/tournaments", timeout=15).json()
    TOURNAMENT_SLUGS.extend(item["slug"] for item in body["data"]["items"])

    body = requests.get(f"{host}/api/teams", timeout=15).json()
    TEAM_SLUGS.extend(item["slug"] for item in body["data"]["items"])

    if not (TOURNAMENT_IDS and TOURNAMENT_SLUGS and TEAM_SLUGS):
        raise RuntimeError("Baza ne vsebuje turnirjev ali ekip; obremenitveni test ni smiseln.")

    print(
        f"[setup] turnirji (public)={len(set(TOURNAMENT_IDS))}, "
        f"turnirji (slug)={len(TOURNAMENT_SLUGS)}, ekipe={len(TEAM_SLUGS)}"
    )


class PublicVisitor(HttpUser):
    """Anonimni obiskovalec javnega dela DotaOps."""

    wait_time = between(1, 3)

    def _get(self, path: str, name: str, params: dict | None = None):
        """GET z validacijo: HTTP 200 in JSON telo z ključem 'data'."""
        with self.client.get(
            path,
            params=params,
            name=name,
            timeout=REQUEST_TIMEOUT_S,
            catch_response=True,
        ) as response:
            if response.status_code != 200:
                response.failure(f"HTTP {response.status_code}")
                return None
            try:
                payload = response.json()
            except ValueError:
                response.failure("Telo odgovora ni veljaven JSON")
                return None
            if "data" not in payload:
                response.failure("Odgovor ne vsebuje polja 'data'")
                return None
            response.success()
            return payload["data"]

    @task(5)
    def browse_tournament_catalogue(self):
        # Večina obiskovalcev ostane na prvi strani kataloga.
        page = random.choice([1, 1, 1, 2, 3])
        self._get("/api/public/tournaments", "GET /api/public/tournaments", {"page": page})

    @task(4)
    def open_public_tournament(self):
        tournament_id = random.choice(TOURNAMENT_IDS)
        self._get(f"/api/public/tournaments/{tournament_id}", "GET /api/public/tournaments/{id}")
        # Uporabnik odpre en ali dva zavihka na strani turnirja.
        for tab in random.sample(["standings", "bracket", "matches", "teams"], k=random.randint(1, 2)):
            self._get(
                f"/api/public/tournaments/{tournament_id}/{tab}",
                f"GET /api/public/tournaments/{{id}}/{tab}",
            )

    @task(2)
    def open_tournament_by_slug(self):
        self._get("/api/tournaments", "GET /api/tournaments")
        self._get(f"/api/tournaments/{random.choice(TOURNAMENT_SLUGS)}", "GET /api/tournaments/{slug}")

    @task(3)
    def browse_teams(self):
        self._get("/api/teams", "GET /api/teams")
        slug = random.choice(TEAM_SLUGS)
        self._get(f"/api/teams/by-slug/{slug}", "GET /api/teams/by-slug/{slug}")
        self._get(f"/api/teams/by-slug/{slug}/members", "GET /api/teams/by-slug/{slug}/members")

    @task(2)
    def view_analytics(self):
        section = random.choice(["heroes", "teams", "players", "tournaments"])
        self._get(f"/api/public/analytics/{section}", f"GET /api/public/analytics/{section}")

    @task(1)
    def lookup_heroes(self):
        self._get("/api/lookups/heroes", "GET /api/lookups/heroes")


class LoadProfile(LoadTestShape):
    """Narašcanje -> stabilna pričakovana obremenitev -> zmanjševanje."""

    def tick(self):
        t = self.get_run_time()
        if t < RAMP_UP_S + STEADY_S:
            return TARGET_USERS, TARGET_USERS / RAMP_UP_S
        if t < RAMP_UP_S + STEADY_S + RAMP_DOWN_S:
            return 0, TARGET_USERS / RAMP_DOWN_S
        return None
