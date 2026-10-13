import { CalendarDays, DatabaseZap, GitBranch, Send, UsersRound } from "lucide-react";

const workflow = [
  {
    detail: "Core data, format, and start time",
    icon: CalendarDays,
    label: "Tournament"
  },
  {
    detail: "Captains, rosters, and confirmations",
    icon: UsersRound,
    label: "Registrations"
  },
  {
    detail: "Pairings, results, and advancement",
    icon: GitBranch,
    label: "Bracket"
  },
  {
    detail: "match_id, normalization, and metrics",
    icon: DatabaseZap,
    label: "OpenDota"
  },
  {
    detail: "Public view and result publishing",
    icon: Send,
    label: "Publishing"
  }
];

export function OrganizerWorkflowPanel() {
  return (
    <div className="organizer-workflow-panel">
      {workflow.map((step, index) => (
        <article key={step.label}>
          <span className="ops-mono">P{index + 1}</span>
          <step.icon size={18} />
          <div>
            <strong>{step.label}</strong>
            <p className="ops-mono">{step.detail}</p>
          </div>
        </article>
      ))}
    </div>
  );
}
