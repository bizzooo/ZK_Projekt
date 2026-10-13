import type { LucideIcon } from "lucide-react";

interface TelemetryCardProps {
  icon: LucideIcon;
  label: string;
  value: string;
  delta: string;
  tone?: "red" | "cyan" | "gold" | "green";
}

export function TelemetryCard({
  icon: Icon,
  label,
  value,
  delta,
  tone = "cyan"
}: TelemetryCardProps) {
  return (
    <article className={`telemetry-card telemetry-${tone} ops-card`}>
      <div>
        <span className="ops-label">{label}</span>
        <strong className="ops-data">{value}</strong>
      </div>
      <div className="telemetry-card-footer">
        <Icon size={17} />
        <span className="ops-mono">{delta}</span>
      </div>
    </article>
  );
}
