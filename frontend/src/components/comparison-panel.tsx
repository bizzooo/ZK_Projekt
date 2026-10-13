import type { Team } from "@/lib/types";
import { formatPercent } from "@/lib/utils";

interface ComparisonPanelProps {
  teams: Team[];
}

export function ComparisonPanel({ teams }: ComparisonPanelProps) {
  const rankedTeams = [...teams].sort((a, b) => b.winRate - a.winRate);

  return (
    <div className="comparison-panel">
      {rankedTeams.slice(0, 4).map((team, index) => (
        <article className="comparison-row" key={team.id}>
          <span className="ops-mono">#{index + 1}</span>
          <div>
            <strong>{team.name}</strong>
            <p className="ops-mono">
              {team.region} / {team.captain}
            </p>
          </div>
          <div className="comparison-metrics">
            <span>
              <em>WR</em>
              <strong>{formatPercent(team.winRate)}</strong>
            </span>
            <span>
              <em>KDA</em>
              <strong>{team.kda.toFixed(1)}</strong>
            </span>
          </div>
        </article>
      ))}
    </div>
  );
}
