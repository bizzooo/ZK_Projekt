import { Activity, Database, RadioTower, ShieldCheck } from "lucide-react";

import type { Team } from "@/lib/types";
import { formatPercent } from "@/lib/utils";

interface TeamStatusPanelProps {
  teams: Team[];
}

export function TeamStatusPanel({ teams }: TeamStatusPanelProps) {
  const avgKda =
    teams.reduce((total, team) => total + team.kda, 0) / Math.max(teams.length, 1);
  const topWinRate = Math.max(...teams.map((team) => team.winRate), 0);
  const regions = new Set(teams.map((team) => team.region)).size;

  return (
    <aside className="team-status-panel ops-panel">
      <div className="team-status-heading">
        <span className="ops-label">Roster telemetry</span>
        <span className="team-active-badge ops-mono">
          <RadioTower size={13} />
          online
        </span>
      </div>

      <div className="team-status-list">
        <article>
          <ShieldCheck size={18} />
          <span className="ops-label">Top win rate</span>
          <strong className="ops-data">{formatPercent(topWinRate)}</strong>
        </article>
        <article>
          <Activity size={18} />
          <span className="ops-label">Avg KDA</span>
          <strong className="ops-data">{avgKda.toFixed(1)}</strong>
        </article>
        <article>
          <Database size={18} />
          <span className="ops-label">Regions</span>
          <strong className="ops-data">{regions}</strong>
        </article>
      </div>
    </aside>
  );
}
