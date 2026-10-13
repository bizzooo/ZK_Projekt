import { Activity, GitBranch, ListOrdered, ShieldCheck, UsersRound } from "lucide-react";
import type { LucideIcon } from "lucide-react";
import type { ReactNode } from "react";

import { SectionHeader } from "@/components/section-header";
import type {
  GroupStanding,
  PublicTournamentGroup,
  TournamentGroup
} from "@/lib/tournament-group-data";
import { classNames } from "@/lib/utils";

type DisplayGroup = PublicTournamentGroup | TournamentGroup;

interface GroupsStandingsPanelProps {
  error?: string | null;
  groups: DisplayGroup[];
  isLoading?: boolean;
  managementAction?: ReactNode;
  mode?: "public" | "organizer";
  standings: GroupStanding[];
}

const tieBreakRule = "Points -> Match wins -> Game diff -> Game wins";

function groupId(group: DisplayGroup) {
  return "groupId" in group ? group.groupId : group.id;
}

function groupName(group: DisplayGroup) {
  return "groupName" in group ? group.groupName : group.name;
}

function groupOrder(group: DisplayGroup) {
  return "displayOrder" in group ? group.displayOrder : group.sortOrder;
}

function groupTeams(group: DisplayGroup) {
  return group.teams;
}

function teamDisplayName(team: DisplayGroup["teams"][number]) {
  return "teamName" in team ? team.teamName : team.name;
}

function teamTag(team: DisplayGroup["teams"][number]) {
  return "tag" in team ? team.tag : team.teamTag;
}

function teamSeed(team: DisplayGroup["teams"][number]) {
  return team.seedNumber;
}

function standingsByGroup(standings: GroupStanding[]) {
  return standings.reduce<Record<string, GroupStanding[]>>((accumulator, standing) => {
    const rows = accumulator[standing.groupId] ?? [];
    rows.push(standing);
    accumulator[standing.groupId] = rows;
    return accumulator;
  }, {});
}

function finishedSeries(standings: GroupStanding[]) {
  const playedSlots = standings.reduce((total, standing) => total + standing.matchesPlayed, 0);
  return Math.floor(playedSlots / 2);
}

export function GroupsStandingsPanel({
  error,
  groups,
  isLoading = false,
  managementAction,
  mode = "public",
  standings
}: GroupsStandingsPanelProps) {
  const orderedGroups = [...groups].sort((left, right) => groupOrder(left) - groupOrder(right));
  const tableRows = standingsByGroup(standings);
  const assignedTeams = orderedGroups.reduce((total, group) => total + groupTeams(group).length, 0);
  const hasGroups = orderedGroups.length > 0;

  return (
    <section className={classNames("groups-standings-panel ops-panel", mode === "organizer" && "is-organizer")}>
      <SectionHeader
        eyebrow={mode === "organizer" ? "Group stage control" : "Group stage"}
        title="Groups & Standings"
        description="Group stage overview based on official match results."
        action={
          <span className="ops-badge">
            <ShieldCheck size={14} />
            Backend calculated
          </span>
        }
      />

      <div className="groups-standings-summary">
        <SummaryMetric icon={GitBranch} label="Groups" value={String(orderedGroups.length)} />
        <SummaryMetric icon={UsersRound} label="Assigned Teams" value={String(assignedTeams)} />
        <SummaryMetric icon={Activity} label="Finished Series" value={String(finishedSeries(standings))} />
        <SummaryMetric icon={ListOrdered} label="Points Rule" value="3/1/0" />
      </div>

      <div className="groups-standings-rule">
        <span>Tie-break rule</span>
        <strong>{tieBreakRule}</strong>
      </div>

      {isLoading ? (
        <div className="groups-standings-state">
          <h3>Loading groups and standings...</h3>
          <p>Connecting to the official tournament group API.</p>
        </div>
      ) : error ? (
        <div className="groups-standings-state is-error">
          <h3>Group standings are unavailable.</h3>
          <p>{error}</p>
        </div>
      ) : !hasGroups ? (
        <div className="groups-standings-state">
          <h3>Groups are not published yet.</h3>
          <p>Official standings will appear here once tournament groups are available.</p>
          {managementAction}
        </div>
      ) : (
        <div className="groups-standings-layout">
          <div className="groups-standings-groups">
            {orderedGroups.map((group) => {
              const teams = groupTeams(group);
              const rows = tableRows[groupId(group)] ?? [];

              return (
                <article className="groups-standings-card ops-card" key={groupId(group)}>
                  <div className="groups-standings-card-head">
                    <div>
                      <span className="ops-label">Group {groupOrder(group) || "-"}</span>
                      <h3>{groupName(group)}</h3>
                    </div>
                    <span className="ops-badge">{rows.length > 0 ? "Active" : "Setup"}</span>
                  </div>
                  <div className="groups-standings-team-list">
                    {teams.length === 0 ? (
                      <span className="groups-standings-empty-chip">No teams assigned</span>
                    ) : (
                      teams.map((team) => (
                        <span className="groups-standings-team-chip" key={"teamId" in team ? team.teamId : team.id}>
                          {teamSeed(team) ? <em>#{teamSeed(team)}</em> : null}
                          <strong>{teamDisplayName(team)}</strong>
                          {teamTag(team) ? <small>{teamTag(team)}</small> : null}
                        </span>
                      ))
                    )}
                  </div>
                  <p>{teams.length} teams assigned</p>
                </article>
              );
            })}
          </div>

          <div className="groups-standings-tables">
            {orderedGroups.map((group) => {
              const rows = [...(tableRows[groupId(group)] ?? [])].sort((left, right) => left.rank - right.rank);

              return (
                <article className="groups-standings-table-panel ops-card" key={`${groupId(group)}-standings`}>
                  <div className="groups-standings-table-head">
                    <div>
                      <span className="ops-label">Standings</span>
                      <h3>{groupName(group)}</h3>
                    </div>
                    <span className="ops-badge">{rows.length} rows</span>
                  </div>
                  {rows.length === 0 ? (
                    <p className="groups-standings-muted">No official standings rows are available for this group yet.</p>
                  ) : (
                    <div className="groups-standings-table-wrap">
                      <table className="groups-standings-table">
                        <thead>
                          <tr>
                            <th>Rank</th>
                            <th>Team</th>
                            <th>Played</th>
                            <th>W-L-D</th>
                            <th>Games</th>
                            <th>Diff</th>
                            <th>Points</th>
                            <th>Tie-break</th>
                          </tr>
                        </thead>
                        <tbody>
                          {rows.map((standing) => (
                            <tr key={standing.teamId}>
                              <td>#{standing.rank}</td>
                              <td>
                                <strong>{standing.teamName}</strong>
                              </td>
                              <td>{standing.matchesPlayed}</td>
                              <td>{standing.matchWins}-{standing.matchLosses}-{standing.matchDraws}</td>
                              <td>{standing.gameWins}-{standing.gameLosses}</td>
                              <td>{standing.gameDiff > 0 ? `+${standing.gameDiff}` : standing.gameDiff}</td>
                              <td>{standing.points}</td>
                              <td>Rule order</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </article>
              );
            })}
          </div>
        </div>
      )}
    </section>
  );
}

function SummaryMetric({
  icon: Icon,
  label,
  value
}: {
  icon: LucideIcon;
  label: string;
  value: string;
}) {
  return (
    <article className="groups-standings-summary-card ops-card">
      <Icon size={18} />
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  );
}
