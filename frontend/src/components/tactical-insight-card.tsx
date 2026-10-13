import type { LucideIcon } from "lucide-react";
import type { ReactNode } from "react";

interface TacticalInsightCardProps {
  icon: LucideIcon;
  tone?: "primary" | "cyan" | "gold";
  title: string;
  children: ReactNode;
}

export function TacticalInsightCard({
  icon: Icon,
  tone = "primary",
  title,
  children
}: TacticalInsightCardProps) {
  return (
    <article className={`tactical-insight tactical-insight-${tone}`}>
      <div>
        <Icon size={18} />
        <span className="ops-label">{title}</span>
      </div>
      <p>{children}</p>
    </article>
  );
}
