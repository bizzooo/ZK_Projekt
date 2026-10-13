import type { LucideIcon } from "lucide-react";

interface TournamentMetaItem {
  icon: LucideIcon;
  label: string;
  value: string;
  detail?: string;
  tone?: "red" | "gold" | "cyan" | "green";
}

interface TournamentMetaGridProps {
  items: TournamentMetaItem[];
}

export function TournamentMetaGrid({ items }: TournamentMetaGridProps) {
  return (
    <div className="tournament-meta-grid">
      {items.map((item) => (
        <article className={`tournament-meta-card tournament-meta-${item.tone ?? "cyan"}`} key={item.label}>
          <item.icon size={18} />
          <span className="ops-label">{item.label}</span>
          <strong className="ops-data">{item.value}</strong>
          {item.detail ? <p className="ops-mono">{item.detail}</p> : null}
        </article>
      ))}
    </div>
  );
}
