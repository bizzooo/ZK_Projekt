"""Povzame rezultate obremenitvenega testa (results/*.csv) za poročilo.

Uporaba (iz mape scripts/locust-loadtest):  python analyze.py
"""

import csv
import statistics
from pathlib import Path

RESULTS = Path(__file__).parent / "results"
RAMP_UP_S, STEADY_S = 60, 480


def read(name):
    with open(RESULTS / name, newline="", encoding="utf-8-sig") as fh:
        return list(csv.DictReader(fh))


def num(value):
    try:
        return float(value)
    except (TypeError, ValueError):
        return None


# --- 1. Skupna statistika po endpointih ---------------------------------
stats = read("load_stats.csv")
print(f"{'Endpoint':48} {'zaht.':>6} {'napak':>5} {'povp':>6} {'p50':>5} {'p95':>5} {'p99':>5} {'max':>6} {'rps':>6}")
for row in stats:
    print(
        f"{row['Name'][:48]:48} {row['Request Count']:>6} {row['Failure Count']:>5} "
        f"{num(row['Average Response Time']):6.0f} {row['50%']:>5} {row['95%']:>5} {row['99%']:>5} "
        f"{num(row['Max Response Time']):6.0f} {num(row['Requests/s']):6.2f}"
    )

agg = next(r for r in stats if r["Name"] == "Aggregated")
total, failed = int(agg["Request Count"]), int(agg["Failure Count"])
print(f"\nNapake: {failed}/{total} = {100 * failed / total:.3f} %")

# --- 2. Časovna vrsta (agregirano) --------------------------------------
history = [r for r in read("load_stats_history.csv") if r["Name"] == "Aggregated"]
t0 = int(history[0]["Timestamp"])
series = []
for r in history:
    series.append({
        "t": int(r["Timestamp"]) - t0,
        "users": int(r["User Count"]),
        "rps": num(r["Requests/s"]) or 0.0,
        "p50": num(r["50%"]),
        "p95": num(r["95%"]),
        "total": int(r["Total Request Count"]),
        "fails": int(r["Total Failure Count"]),
    })


def window(a, b):
    return [s for s in series if a <= s["t"] < b]


steady = window(RAMP_UP_S, RAMP_UP_S + STEADY_S)
steady_reqs = steady[-1]["total"] - steady[0]["total"]
steady_secs = steady[-1]["t"] - steady[0]["t"]
print(f"\nStabilna faza {RAMP_UP_S}-{RAMP_UP_S + STEADY_S} s: uporabnikov={statistics.mode(s['users'] for s in steady)}, "
      f"zahtev={steady_reqs}, prepustnost={steady_reqs / steady_secs:.2f} zaht./s")

first = window(RAMP_UP_S, RAMP_UP_S + 120)
last = window(RAMP_UP_S + STEADY_S - 120, RAMP_UP_S + STEADY_S)
for label, w in (("prvi 2 min stabilne faze", first), ("zadnji 2 min stabilne faze", last)):
    p95s = [s["p95"] for s in w if s["p95"]]
    p50s = [s["p50"] for s in w if s["p50"]]
    print(f"{label:28}: mediana(p50)={statistics.median(p50s):.0f} ms, mediana(p95)={statistics.median(p95s):.0f} ms, "
          f"povp. rps={statistics.mean(s['rps'] for s in w):.2f}")

# Vzorec časovne vrste vsakih 30 s (za tabelo/graf v poročilu).
print("\n  t[s] upor.  rps    p50   p95")
for s in series:
    if s["t"] % 30 == 0:
        print(f"{s['t']:6} {s['users']:5} {s['rps']:6.2f} {s['p50'] or 0:6.0f} {s['p95'] or 0:5.0f}")

# --- 3. Viri backend procesa --------------------------------------------
res = read("backend-resources.csv")
cpu = [num(r["cpu_percent_of_machine"]) for r in res]
ws = [num(r["working_set_mb"]) for r in res]
thr = [int(r["threads"]) for r in res]
steady_res = [r for r in res if RAMP_UP_S <= num(r["elapsed_s"]) < RAMP_UP_S + STEADY_S]
steady_cpu = [num(r["cpu_percent_of_machine"]) for r in steady_res]
print(f"\nCPU (stabilna faza): povp={statistics.mean(steady_cpu):.1f} %, max={max(steady_cpu):.1f} %  (delež celotnega računalnika)")
print(f"Pomnilnik (working set): začetek={ws[0]:.0f} MB, max={max(ws):.0f} MB, konec={ws[-1]:.0f} MB")
print(f"Niti: min={min(thr)}, max={max(thr)}")
first_ws = [num(r["working_set_mb"]) for r in steady_res[:24]]
last_ws = [num(r["working_set_mb"]) for r in steady_res[-24:]]
print(f"Working set prvi/zadnji 2 min stabilne faze: {statistics.mean(first_ws):.0f} / {statistics.mean(last_ws):.0f} MB")
