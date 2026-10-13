import { CheckCircle2, CircleDot, Circle } from "lucide-react";

import type { RoadmapItem } from "@/lib/types";

const icons = {
  done: CheckCircle2,
  active: CircleDot,
  planned: Circle
};

export function PriorityRoadmap({ items }: { items: RoadmapItem[] }) {
  return (
    <div className="roadmap-list">
      {items.map((item) => {
        const Icon = icons[item.status];

        return (
          <article className={`roadmap-item roadmap-${item.status}`} key={item.iteration}>
            <div className="roadmap-icon" aria-hidden="true">
              <Icon size={18} />
            </div>
            <div>
              <p className="eyebrow">{item.iteration}</p>
              <h3>{item.title}</h3>
              <ul>
                {item.items.map((entry) => (
                  <li key={entry}>{entry}</li>
                ))}
              </ul>
            </div>
          </article>
        );
      })}
    </div>
  );
}
