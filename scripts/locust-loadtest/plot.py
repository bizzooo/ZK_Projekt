"""Izriše graf poteka obremenitvenega testa v results/load-timeline.png."""

import csv
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt  # noqa: E402

RESULTS = Path(__file__).parent / "results"

with open(RESULTS / "load_stats_history.csv", newline="", encoding="utf-8-sig") as fh:
    rows = [r for r in csv.DictReader(fh) if r["Name"] == "Aggregated"]

t0 = int(rows[0]["Timestamp"])
t = [(int(r["Timestamp"]) - t0) / 60 for r in rows]
users = [int(r["User Count"]) for r in rows]
rps = [float(r["Requests/s"] or 0) for r in rows]
p50 = [float(r["50%"]) if r["50%"] not in ("", "N/A") else None for r in rows]
p95 = [float(r["95%"]) if r["95%"] not in ("", "N/A") else None for r in rows]

fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(10, 6.5), sharex=True)

ax1.plot(t, users, color="#7a7a7a", label="sočasni uporabniki")
ax1.set_ylabel("uporabniki")
ax1b = ax1.twinx()
ax1b.plot(t, rps, color="#2f6db5", label="zahtev/s")
ax1b.set_ylabel("zahtev/s")
ax1.set_title("DotaOps – obremenitveni test (Locust)")
lines = ax1.get_lines() + ax1b.get_lines()
ax1.legend(lines, [line.get_label() for line in lines], loc="lower center")

ax2.plot(t, p50, color="#2f9e6e", label="p50")
ax2.plot(t, p95, color="#d0602b", label="p95")
ax2.axhline(1000, color="#b33", linestyle="--", linewidth=1, label="kriterij p95 = 1000 ms")
ax2.set_ylabel("odzivni čas [ms]")
ax2.set_xlabel("čas testa [min]")
ax2.legend(loc="upper right")

for ax in (ax1, ax2):
    ax.axvspan(1, 9, color="#f0f0f0", zorder=0)
    ax.grid(alpha=0.3)

fig.tight_layout()
fig.savefig(RESULTS / "load-timeline.png", dpi=130)
print("Shranjeno:", RESULTS / "load-timeline.png")
