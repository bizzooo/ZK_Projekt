import { Activity, CheckCircle2, DatabaseZap, GitBranch, UsersRound } from "lucide-react";

import type { Match, Tournament } from "@/lib/types";

interface OrganizerStatusPanelProps {
  matches: Match[];
  tournaments: Tournament[];
}

export function OrganizerStatusPanel({
  matches,
  tournaments
}: OrganizerStatusPanelProps) {
  const liveMatches = matches.filter((match) => match.status === "live").length;
  const finishedMatches = matches.filter((match) => match.status === "finished").length;
  const importedMatches = matches.filter((match) => match.dotaMatchId).length;
  const registrations = tournaments.reduce(
    (total, tournament) => total + tournament.registrationsCount,
    0
  );

  return (
    <aside className="organizer-status-panel ops-panel">
      <div className="organizer-status-heading">
        <span className="ops-label">Operations status</span>
        <span className="organizer-online-badge ops-mono">
          <Activity size={13} />
          online
        </span>
      </div>

      <div className="organizer-status-list">
        <article>
          <UsersRound size={18} />
          <span className="ops-label">Team Registrations</span>
          <strong className="ops-data">{registrations}</strong>
        </article>
        <article>
          <GitBranch size={18} />
          <span className="ops-label">Live Matches</span>
          <strong className="ops-data">{liveMatches}</strong>
        </article>
        <article>
          <CheckCircle2 size={18} />
          <span className="ops-label">Results</span>
          <strong className="ops-data">{finishedMatches}</strong>
        </article>
        <article>
          <DatabaseZap size={18} />
          <span className="ops-label">OpenDota linked</span>
          <strong className="ops-data">{importedMatches}</strong>
        </article>
      </div>
    </aside>
  );
}
