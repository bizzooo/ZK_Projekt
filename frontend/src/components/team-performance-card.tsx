import { Crown, ShieldCheck, Swords, UsersRound } from "lucide-react";

import type { Team } from "@/lib/types";
import { formatPercent } from "@/lib/utils";

interface TeamPerformanceCardProps {
  team: Team;
  rank: number;
}

export function TeamPerformanceCard({ team, rank }: TeamPerformanceCardProps) {
  return (
    <article className="team-performance-card ops-panel">
      <div className="team-performance-topline">
        <span className="ops-mono">#{rank}</span>
        <span className="team-active-badge ops-mono">
          <ShieldCheck size={13} />
          active
        </span>
      </div>

      <div className="team-performance-title">
        <div>
          <p className="ops-label">{team.region}</p>
          <h3>{team.name}</h3>
        </div>
        <Crown size={22} />
      </div>

      <div className="team-performance-metrics">
        <span>
          <strong className="ops-data">{formatPercent(team.winRate)}</strong>
          <em>Win rate</em>
        </span>
        <span>
          <strong className="ops-data">{team.kda.toFixed(1)}</strong>
          <em>KDA</em>
        </span>
        <span>
          <strong className="ops-data">{team.roster.length}</strong>
          <em>Members</em>
        </span>
      </div>

      <div className="team-captain-line">
        <UsersRound size={16} />
        <span className="ops-label">Captain</span>
        <strong>{team.captain}</strong>
      </div>

      <div className="team-hero-pool">
        {team.favoriteHeroes.map((hero) => (
          <span className="ops-mono" key={hero}>
            {hero}
          </span>
        ))}
      </div>

      <div className="team-form-line" aria-label="Last five matches">
        <Swords size={16} />
        {team.lastFive.map((result, index) => (
          <span className={`team-form-pill team-form-${result.toLowerCase()}`} key={`${team.id}-${index}`}>
            {result}
          </span>
        ))}
      </div>
    </article>
  );
}
