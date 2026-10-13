import type { ImportStatus, MatchStatus, TournamentStatus } from "@/lib/types";
import { classNames } from "@/lib/utils";

type Status = TournamentStatus | MatchStatus | ImportStatus;

const labels: Record<Status, string> = {
  draft: "Draft",
  archived: "Archived",
  registration: "Registration",
  published: "Published",
  live: "Live",
  finished: "Completed",
  scheduled: "Scheduled",
  queued: "Queued",
  ready: "Ready",
  processing: "Processing",
  error: "Error",
  idle: "Idle"
};

export function StatusBadge({ status }: { status: Status }) {
  return (
    <span className={classNames("status-badge ops-badge ops-signal", `status-${status}`)}>
      {labels[status]}
    </span>
  );
}
