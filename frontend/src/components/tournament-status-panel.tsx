import { Activity, DatabaseZap, GitBranch, UsersRound } from "lucide-react";

import { StatusBadge } from "@/components/status-badge";
import type { Match, Tournament } from "@/lib/types";

interface TournamentStatusPanelProps {
  tournament: Tournament;
  matches: Match[];
}

export function TournamentStatusPanel({
  tournament,
  matches
}: TournamentStatusPanelProps) {
  const importedMatches = matches.filter((match) => match.dotaMatchId).length;
  const finishedMatches = matches.filter((match) => match.status === "finished").length;
  const liveMatches = matches.filter((match) => match.status === "live").length;

  return (
    <aside className="tournament-status-panel ops-panel">
      <div className="tournament-status-heading">
        <span className="ops-label">Control status</span>
        <StatusBadge status={tournament.status} />
      </div>

      <div className="tournament-status-list">
        <article>
          <UsersRound size={18} />
          <span className="ops-label">Registrations</span>
          <strong className="ops-data">
            {tournament.registrationsCount}/{tournament.teamsCount}
          </strong>
        </article>
        <article>
          <GitBranch size={18} />
          <span className="ops-label">Matches</span>
          <strong className="ops-data">
            {finishedMatches}/{matches.length}
          </strong>
        </article>
        <article>
          <Activity size={18} />
          <span className="ops-label">Live slots</span>
          <strong className="ops-data">{liveMatches}</strong>
        </article>
        <article>
          <DatabaseZap size={18} />
          <span className="ops-label">Match_id</span>
          <strong className="ops-data">{importedMatches}</strong>
        </article>
      </div>
    </aside>
  );
}
