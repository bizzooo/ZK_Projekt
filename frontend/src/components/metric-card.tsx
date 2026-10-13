import type { LucideIcon } from "lucide-react";

interface MetricCardProps {
  label: string;
  value: string;
  trend: string;
  icon: LucideIcon;
  tone?: "red" | "teal" | "amber" | "green";
}

export function MetricCard({
  label,
  value,
  trend,
  icon: Icon,
  tone = "red"
}: MetricCardProps) {
  return (
    <article className={`metric-card metric-${tone} ops-card`}>
      <div className="metric-icon" aria-hidden="true">
        <Icon size={20} />
      </div>
      <div>
        <p className="ops-label">{label}</p>
        <strong className="ops-data">{value}</strong>
        <span className="ops-mono">{trend}</span>
      </div>
    </article>
  );
}
