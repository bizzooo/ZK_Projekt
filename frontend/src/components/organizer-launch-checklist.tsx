import { CheckCircle2, CircleDot, Circle } from "lucide-react";

const launchChecklist = [
  { label: "Tournament core data configured", status: "ready" },
  { label: "Team registration open", status: "active" },
  { label: "Participants confirmed", status: "ready" },
  { label: "Bracket generated", status: "pending" },
  { label: "Match schedule published", status: "pending" },
  { label: "Results entered", status: "pending" },
  { label: "Match data imported", status: "active" },
  { label: "Public view published", status: "pending" }
];

const icons = {
  active: CircleDot,
  pending: Circle,
  ready: CheckCircle2
};

export function OrganizerLaunchChecklist() {
  return (
    <div className="organizer-launch-checklist">
      {launchChecklist.map((item, index) => {
        const Icon = icons[item.status as keyof typeof icons];

        return (
          <article className={`organizer-check-item organizer-check-${item.status}`} key={item.label}>
            <Icon size={17} />
            <span className="ops-mono">L{String(index + 1).padStart(2, "0")}</span>
            <strong>{item.label}</strong>
          </article>
        );
      })}
    </div>
  );
}
