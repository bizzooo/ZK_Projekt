import type { LucideIcon } from "lucide-react";

interface OrganizerAction {
  icon: LucideIcon;
  label: string;
  value: string;
  detail: string;
  tone?: "red" | "gold" | "cyan" | "green";
}

interface OrganizerActionGridProps {
  actions: OrganizerAction[];
}

export function OrganizerActionGrid({ actions }: OrganizerActionGridProps) {
  return (
    <div className="organizer-action-grid">
      {actions.map((action) => (
        <article className={`organizer-action-card organizer-action-${action.tone ?? "cyan"}`} key={action.label}>
          <action.icon size={18} />
          <span className="ops-label">{action.label}</span>
          <strong className="ops-data">{action.value}</strong>
          <p className="ops-mono">{action.detail}</p>
        </article>
      ))}
    </div>
  );
}
