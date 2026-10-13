import type { Match } from "@/lib/types";

export function BracketView({ matches }: { matches: Match[] }) {
  const rounds = Array.from(new Set(matches.map((match) => match.round)));

  return (
    <div className="bracket" aria-label="Tournament bracket">
      {rounds.map((round) => (
        <section className="bracket-column" key={round}>
          <h3>{round}</h3>
          {matches
            .filter((match) => match.round === round)
            .map((match) => (
              <article className="bracket-match" key={match.id}>
                <div className="bracket-team">
                  <span>{match.teamA}</span>
                  <strong>{match.scoreA ?? "-"}</strong>
                </div>
                <div className="bracket-team">
                  <span>{match.teamB}</span>
                  <strong>{match.scoreB ?? "-"}</strong>
                </div>
              </article>
            ))}
        </section>
      ))}
    </div>
  );
}
