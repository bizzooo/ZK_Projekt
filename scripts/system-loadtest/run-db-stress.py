#!/usr/bin/env python3
"""Staged concurrent load against DotaOps' JDBC-backed hero lookup endpoint."""
from __future__ import annotations

import argparse
import concurrent.futures
import csv
import json
import math
import threading
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path


def percentile(values: list[float], p: float) -> float | None:
    if not values:
        return None
    ordered = sorted(values)
    return ordered[max(0, math.ceil(p * len(ordered)) - 1)]


def worker(worker_id: int, users: int, duration: float, barrier: threading.Barrier,
           url: str, timeout: float) -> list[dict]:
    barrier.wait()
    deadline = time.monotonic() + duration
    rows: list[dict] = []
    while time.monotonic() < deadline:
        started = time.monotonic()
        code = ""
        error = ""
        try:
            request = urllib.request.Request(url, headers={"Accept": "application/json"})
            with urllib.request.urlopen(request, timeout=timeout) as response:
                code = response.status
                body = json.loads(response.read())
                if code != 200:
                    error = f"unexpected HTTP {code}"
                elif not isinstance(body.get("data"), list) or len(body["data"]) != 100:
                    error = "response data was not a list of 100 heroes"
        except urllib.error.HTTPError as exc:
            code = exc.code
            error = f"HTTP {exc.code}: {exc.reason}"
        except Exception as exc:  # connection/timeouts/invalid JSON are failures too
            error = f"{type(exc).__name__}: {exc}"
        ended = time.monotonic()
        rows.append({
            "timestamp_utc": datetime.now(timezone.utc).isoformat(),
            "stage_users": users,
            "worker_id": worker_id,
            "http_status": code,
            "latency_ms": round((ended - started) * 1000, 3),
            "error": error,
        })
    return rows


def main() -> None:
    run_started_utc = datetime.now(timezone.utc).isoformat()
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", default="http://127.0.0.1:18080/api/lookups/heroes?limit=100")
    parser.add_argument("--stages", default="10:15,25:15,50:15,100:15",
                        help="comma-separated users:seconds stages")
    parser.add_argument("--timeout", type=float, default=5.0)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()
    stages = [(int(users), int(seconds)) for users, seconds in
              (piece.split(":", 1) for piece in args.stages.split(","))]
    if not stages or any(users < 1 or seconds < 1 for users, seconds in stages):
        parser.error("stages must contain positive users and seconds")

    args.out.mkdir(parents=True, exist_ok=True)
    stage_summaries: list[dict] = []
    all_rows: list[dict] = []
    for stage_no, (users, duration) in enumerate(stages, 1):
        barrier = threading.Barrier(users + 1)
        started = time.monotonic()
        with concurrent.futures.ThreadPoolExecutor(max_workers=users) as pool:
            futures = [pool.submit(worker, worker_id, users, duration, barrier,
                                   args.url, args.timeout)
                       for worker_id in range(1, users + 1)]
            barrier.wait()
            rows = [row for future in futures for row in future.result()]
        elapsed = time.monotonic() - started
        all_rows.extend(rows)
        latencies = [row["latency_ms"] for row in rows]
        failures = [row for row in rows if row["error"]]
        stage_summaries.append({
            "stage": stage_no,
            "concurrent_users": users,
            "planned_seconds": duration,
            "actual_seconds": round(elapsed, 3),
            "requests": len(rows),
            "failures": len(failures),
            "failure_rate_percent": round(100 * len(failures) / len(rows), 3) if rows else 100.0,
            "throughput_rps": round(len(rows) / elapsed, 3) if elapsed else 0,
            "latency_ms": {
                "min": round(min(latencies), 3) if latencies else None,
                "mean": round(sum(latencies) / len(latencies), 3) if latencies else None,
                "p50": round(percentile(latencies, .50), 3) if latencies else None,
                "p95": round(percentile(latencies, .95), 3) if latencies else None,
                "p99": round(percentile(latencies, .99), 3) if latencies else None,
                "max": round(max(latencies), 3) if latencies else None,
            },
        })
        print(json.dumps(stage_summaries[-1], ensure_ascii=False), flush=True)

    with (args.out / "requests.csv").open("w", newline="", encoding="utf-8") as stream:
        writer = csv.DictWriter(stream, fieldnames=["timestamp_utc", "stage_users", "worker_id",
                                                   "http_status", "latency_ms", "error"])
        writer.writeheader()
        writer.writerows(all_rows)
    summary = {
        "started_utc": run_started_utc,
        "target": args.url,
        "stages": stage_summaries,
        "total_requests": len(all_rows),
        "total_failures": sum(bool(row["error"]) for row in all_rows),
        "acceptance_criteria": {
            "request_failure_rate_percent_max": 1.0,
            "stage_p95_latency_ms_max": 2000,
            "application_remains_available": True,
        },
    }
    (args.out / "summary.json").write_text(json.dumps(summary, indent=2), encoding="utf-8")
    print(f"Saved {args.out / 'requests.csv'} and {args.out / 'summary.json'}", flush=True)


if __name__ == "__main__":
    main()
