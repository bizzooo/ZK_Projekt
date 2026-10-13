import { getApiAuthenticated } from "@/lib/api";

export type OrganizerMatchStatus =
  | "scheduled"
  | "ready"
  | "live"
  | "finished"
  | "cancelled"
  | string;

export interface OrganizerMatch {
  bestOf: number;
  bracketPosition: number | null;
  cancellationReason: string | null;
  cancelledAt: string | null;
  createdAt: string | null;
  finishedAt: string | null;
  groupId: string | null;
  id: string;
  roundName: string | null;
  roundNumber: number;
  scheduledAt: string | null;
  scoreA: number;
  scoreB: number;
  stageName: string | null;
  startedAt: string | null;
  status: OrganizerMatchStatus;
  teamAId: string | null;
  teamAName: string | null;
  teamBId: string | null;
  teamBName: string | null;
  tournamentId: string;
  updatedAt: string | null;
  winnerTeamId: string | null;
  winnerTeamName: string | null;
}

interface OrganizerMatchDto {
  bestOf?: number | null;
  bracketPosition?: number | null;
  cancellationReason?: string | null;
  cancelledAt?: string | null;
  createdAt?: string | null;
  finishedAt?: string | null;
  groupId?: string | null;
  id: string;
  roundName?: string | null;
  roundNumber?: number | null;
  scheduledAt?: string | null;
  scoreA?: number | null;
  scoreB?: number | null;
  stageName?: string | null;
  startedAt?: string | null;
  status?: string | null;
  teamAId?: string | null;
  teamAName?: string | null;
  teamBId?: string | null;
  teamBName?: string | null;
  tournamentId: string;
  updatedAt?: string | null;
  winnerTeamId?: string | null;
  winnerTeamName?: string | null;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === "object" && !Array.isArray(value);
}

function asMatch(value: unknown): OrganizerMatch {
  if (!isRecord(value) || typeof value.id !== "string" || typeof value.tournamentId !== "string") {
    throw new Error("Organizer match API returned an unexpected match shape.");
  }

  const dto = value as unknown as OrganizerMatchDto;

  return {
    bestOf: dto.bestOf ?? 1,
    bracketPosition: dto.bracketPosition ?? null,
    cancellationReason: dto.cancellationReason ?? null,
    cancelledAt: dto.cancelledAt ?? null,
    createdAt: dto.createdAt ?? null,
    finishedAt: dto.finishedAt ?? null,
    groupId: dto.groupId ?? null,
    id: dto.id,
    roundName: dto.roundName ?? null,
    roundNumber: dto.roundNumber ?? 0,
    scheduledAt: dto.scheduledAt ?? null,
    scoreA: dto.scoreA ?? 0,
    scoreB: dto.scoreB ?? 0,
    stageName: dto.stageName ?? null,
    startedAt: dto.startedAt ?? null,
    status: dto.status ?? "scheduled",
    teamAId: dto.teamAId ?? null,
    teamAName: dto.teamAName ?? null,
    teamBId: dto.teamBId ?? null,
    teamBName: dto.teamBName ?? null,
    tournamentId: dto.tournamentId,
    updatedAt: dto.updatedAt ?? null,
    winnerTeamId: dto.winnerTeamId ?? null,
    winnerTeamName: dto.winnerTeamName ?? null
  };
}

function asMatchList(value: unknown): OrganizerMatch[] {
  if (!Array.isArray(value)) {
    throw new Error("Organizer match API returned an unexpected list shape.");
  }

  return value.map(asMatch);
}

export async function listOrganizerTournamentMatches(tournamentId: string) {
  return asMatchList(
    await getApiAuthenticated<unknown>(`/organizer/tournaments/${tournamentId}/matches`)
  );
}
