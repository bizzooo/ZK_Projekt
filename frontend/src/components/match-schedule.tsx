import { Clock, Hash } from "lucide-react";

import { StatusBadge } from "@/components/status-badge";
import type { Match } from "@/lib/types";
import { formatDateTime } from "@/lib/utils";

export function MatchSchedule({ matches }: { matches: Match[] }) {
  return (
    <div className="match-list">
      {matches.map((match) => (
        <article className="match-row ops-card" key={match.id}>
          <div>
            <p className="eyebrow ops-label">{match.round}</p>
            <h3>
              {match.teamA} <span>vs</span> {match.teamB}
            </h3>
            <div className="match-meta">
              <span className="ops-mono">
                <Clock size={15} />
                {formatDateTime(match.startsAt)}
              </span>
              {match.dotaMatchId ? (
                <span className="ops-mono">
                  <Hash size={15} />
                  {match.dotaMatchId}
                </span>
              ) : null}
            </div>
          </div>
          <div className="match-result">
            {typeof match.scoreA === "number" && typeof match.scoreB === "number" ? (
              <strong className="ops-data">
                {match.scoreA}:{match.scoreB}
              </strong>
            ) : (
              <strong className="ops-data">TBD</strong>
            )}
            <StatusBadge status={match.status} />
          </div>
        </article>
      ))}
    </div>
  );
}
