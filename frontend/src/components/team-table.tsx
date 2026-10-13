import type { Team } from "@/lib/types";
import { formatPercent } from "@/lib/utils";

export function TeamTable({ teams }: { teams: Team[] }) {
  return (
    <div className="table-wrap">
      <table className="team-table">
        <thead>
          <tr>
            <th>Team</th>
            <th>Captain</th>
            <th>Region</th>
            <th>Win rate</th>
            <th>KDA</th>
            <th>Heroes</th>
          </tr>
        </thead>
        <tbody>
          {teams.map((team) => (
            <tr key={team.id}>
              <td>
                <strong>{team.name}</strong>
                <span>{team.lastFive.join(" ")}</span>
              </td>
              <td>{team.captain}</td>
              <td>{team.region}</td>
              <td>{formatPercent(team.winRate)}</td>
              <td>{team.kda.toFixed(1)}</td>
              <td>{team.favoriteHeroes.join(", ")}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
