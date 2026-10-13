import { UserRound } from "lucide-react";

import type { Team } from "@/lib/types";
import { formatPercent } from "@/lib/utils";

interface TeamRosterGridProps {
  teams: Team[];
}

export function TeamRosterGrid({ teams }: TeamRosterGridProps) {
  return (
    <div className="team-roster-grid">
      {teams.map((team) => (
        <section className="team-roster-panel ops-panel" key={team.id}>
          <div className="team-roster-heading">
            <div>
              <p className="ops-label">{team.region}</p>
              <h3>{team.name}</h3>
            </div>
            <span className="ops-badge ops-mono">{formatPercent(team.winRate)}</span>
          </div>

          <div className="team-roster-list">
            {team.roster.map((player) => (
              <article key={player.id}>
                <UserRound size={17} />
                <div>
                  <strong>{player.nickname}</strong>
                  <p className="ops-mono">
                    {player.role} / {player.favoriteHero}
                  </p>
                </div>
                <span className="ops-data">{player.kda.toFixed(1)}</span>
              </article>
            ))}
          </div>
        </section>
      ))}
    </div>
  );
}
