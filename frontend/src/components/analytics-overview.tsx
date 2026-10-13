import type { CSSProperties } from "react";

import type { HeroMetric, Team } from "@/lib/types";
import { formatPercent } from "@/lib/utils";

interface AnalyticsOverviewProps {
  heroes: HeroMetric[];
  teams: Team[];
}

export function AnalyticsOverview({ heroes, teams }: AnalyticsOverviewProps) {
  const topTeams = [...teams].sort((a, b) => b.winRate - a.winRate).slice(0, 3);

  return (
    <div className="analytics-grid">
      <section className="analytics-panel">
        <h3>Top Performing Heroes</h3>
        <div className="bar-list">
          {heroes.map((hero) => (
            <div className="bar-row" key={hero.hero}>
              <div>
                <strong>{hero.hero}</strong>
                <span>KDA {hero.avgKda.toFixed(1)}</span>
              </div>
              <div
                className="bar-track"
                style={{ "--bar": `${hero.winRate}%` } as CSSProperties}
              >
                <span />
              </div>
              <em>{formatPercent(hero.winRate)}</em>
            </div>
          ))}
        </div>
      </section>

      <section className="analytics-panel">
        <h3>Team Comparison</h3>
        <div className="team-rank-list">
          {topTeams.map((team, index) => (
            <article className="team-rank" key={team.id}>
              <span>{index + 1}</span>
              <div>
                <strong>{team.name}</strong>
                <p>
                  {formatPercent(team.winRate)} win rate - KDA{" "}
                  {team.kda.toFixed(1)}
                </p>
              </div>
            </article>
          ))}
        </div>
      </section>
    </div>
  );
}

