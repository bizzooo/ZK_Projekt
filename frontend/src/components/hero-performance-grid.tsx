import type { HeroMetric } from "@/lib/types";
import { formatPercent } from "@/lib/utils";

interface HeroPerformanceGridProps {
  heroes: HeroMetric[];
}

export function HeroPerformanceGrid({ heroes }: HeroPerformanceGridProps) {
  return (
    <div className="hero-performance-grid">
      {heroes.map((hero, index) => (
        <article className="hero-performance-card ops-panel" key={hero.hero}>
          <div className="hero-performance-visual">
            <span className="ops-mono">H{String(index + 1).padStart(2, "0")}</span>
            <strong>{hero.hero}</strong>
          </div>
          <div className="hero-performance-data">
            <span className="ops-label">Win rate</span>
            <strong className="ops-data">{formatPercent(hero.winRate)}</strong>
          </div>
          <div className="hero-performance-data">
            <span className="ops-label">Pick rate</span>
            <strong className="ops-data">{formatPercent(hero.pickRate)}</strong>
          </div>
          <div className="hero-performance-data">
            <span className="ops-label">Avg KDA</span>
            <strong className="ops-data">{hero.avgKda.toFixed(1)}</strong>
          </div>
        </article>
      ))}
    </div>
  );
}
