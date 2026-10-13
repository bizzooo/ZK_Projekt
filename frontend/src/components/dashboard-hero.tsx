import { Activity, Brackets, DatabaseZap, Play, Trophy } from "lucide-react";
import Link from "next/link";

import { StatusBadge } from "@/components/status-badge";

export function DashboardHero() {
  return (
    <section className="dashboard-hero ops-panel ops-command-grid">
      <div>
        <p className="ops-label">DotaOps Terminal</p>
        <h1>Command Center</h1>
        <p className="dashboard-hero-status ops-mono">
          <span />
          Tournament flow and recent match analysis are ready.
        </p>
      </div>

      <div className="dashboard-hero-actions">
        <Link className="button ops-button-primary" href="/organizator">
          <Play size={17} />
          <span>Manage Tournament</span>
        </Link>
        <Link className="button ops-button-secondary" href="/analitika">
          <Activity size={17} />
          <span>Global Insights</span>
        </Link>
      </div>

      <div className="dashboard-hero-strip">
        <article>
          <Trophy size={18} />
          <span className="ops-label">Tournaments</span>
          <strong className="ops-data">3 active</strong>
        </article>
        <article>
          <Brackets size={18} />
          <span className="ops-label">Bracket</span>
          <strong className="ops-data">live</strong>
        </article>
        <article>
          <DatabaseZap size={18} />
          <span className="ops-label">OpenDota</span>
          <StatusBadge status="processing" />
        </article>
      </div>
    </section>
  );
}
