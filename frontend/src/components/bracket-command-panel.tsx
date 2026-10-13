import { BracketView } from "@/components/bracket-view";
import { SectionHeader } from "@/components/section-header";
import type { Match } from "@/lib/types";

interface BracketCommandPanelProps {
  matches: Match[];
}

export function BracketCommandPanel({ matches }: BracketCommandPanelProps) {
  return (
    <section className="bracket-command-panel ops-panel">
      <SectionHeader
        eyebrow="Bracket control"
        title="Team Advancement"
        description="Visual overview of rounds, results, and open slots in the tournament flow."
      />
      <BracketView matches={matches} />
    </section>
  );
}
