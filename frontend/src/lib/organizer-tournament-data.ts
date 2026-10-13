import {
  getApiAuthenticated,
  patchApiAuthenticated,
  postApiAuthenticated
} from "@/lib/api";

export type OrganizerTournamentStatus =
  | "draft"
  | "registration"
  | "published"
  | "live"
  | "finished"
  | "archived";

export type OrganizerTournamentFormat =
  | "single_elimination"
  | "groups_playoff"
  | "round_robin"
  | "best_of_three_playoff";

export type TournamentRegistrationStatus =
  | "pending"
  | "approved"
  | "rejected"
  | "waitlisted"
  | "checked_in"
  | string;

export interface OrganizerTournamentSettings {
  allowSubstitutes: boolean;
  bestOf: number;
  checkInEnabled: boolean;
  format: OrganizerTournamentFormat;
  maxTeams: number;
  minTeams: number;
  teamSize: number;
}

export interface OrganizerTournament {
  checkInClosesAt: string | null;
  checkInOpensAt: string | null;
  createdAt: string | null;
  description: string | null;
  endsAt: string | null;
  format: OrganizerTournamentFormat;
  id: string;
  maxTeams: number;
  organizerNickname: string | null;
  organizerProfileId: string | null;
  prizePool: string | null;
  publicVisible: boolean;
  publishedAt: string | null;
  registrationClosesAt: string | null;
  registrationOpensAt: string | null;
  registrationsCount: number;
  rules: string | null;
  settings: OrganizerTournamentSettings;
  slug: string;
  startsAt: string | null;
  status: OrganizerTournamentStatus;
  teamsCount: number;
  timezone: string;
  title: string;
  updatedAt: string | null;
}

export interface OrganizerTournamentRegistration {
  captainNickname: string | null;
  checkedInAt: string | null;
  contactEmail: string | null;
  createdAt: string | null;
  id: string;
  message: string | null;
  reviewedAt: string | null;
  reviewedByNickname: string | null;
  seedNumber: number | null;
  status: TournamentRegistrationStatus;
  teamId: string;
  teamName: string;
  teamSlug: string | null;
  teamTag: string | null;
  tournamentId: string;
  updatedAt: string | null;
}

export interface OrganizerTournamentPayload {
  checkInClosesAt?: string | null;
  checkInOpensAt?: string | null;
  description?: string | null;
  endsAt?: string | null;
  format?: OrganizerTournamentFormat;
  maxTeams?: number;
  prizePool?: string | null;
  registrationClosesAt?: string | null;
  registrationOpensAt?: string | null;
  rules?: string | null;
  settings?: {
    allowSubstitutes?: boolean;
    bestOf?: number;
    checkInEnabled?: boolean;
    format?: OrganizerTournamentFormat;
    maxTeams?: number;
    minTeams?: number;
    teamSize?: number;
  };
  slug?: string | null;
  startsAt?: string | null;
  timezone?: string | null;
  title?: string;
}

interface OrganizerTournamentDto {
  checkInClosesAt?: string | null;
  checkInOpensAt?: string | null;
  createdAt?: string | null;
  description?: string | null;
  endsAt?: string | null;
  format?: string | null;
  id: string;
  maxTeams?: number | null;
  organizerNickname?: string | null;
  organizerProfileId?: string | null;
  prizePool?: string | null;
  publicVisible?: boolean | null;
  publishedAt?: string | null;
  registrationClosesAt?: string | null;
  registrationOpensAt?: string | null;
  registrationsCount?: number | null;
  rules?: string | null;
  settings?: OrganizerTournamentSettingsDto | null;
  slug?: string | null;
  startsAt?: string | null;
  status?: string | null;
  teamsCount?: number | null;
  timezone?: string | null;
  title?: string | null;
  updatedAt?: string | null;
}

interface OrganizerTournamentSettingsDto {
  allowSubstitutes?: boolean | null;
  bestOf?: number | null;
  checkInEnabled?: boolean | null;
  format?: string | null;
  maxTeams?: number | null;
  minTeams?: number | null;
  teamSize?: number | null;
}

interface OrganizerTournamentRegistrationDto {
  captainNickname?: string | null;
  checkedInAt?: string | null;
  contactEmail?: string | null;
  createdAt?: string | null;
  id: string;
  message?: string | null;
  reviewedAt?: string | null;
  reviewedByNickname?: string | null;
  seedNumber?: number | null;
  status?: string | null;
  teamId: string;
  teamName?: string | null;
  teamSlug?: string | null;
  teamTag?: string | null;
  tournamentId: string;
  updatedAt?: string | null;
}

const statuses: OrganizerTournamentStatus[] = [
  "draft",
  "registration",
  "published",
  "live",
  "finished",
  "archived"
];

const formats: OrganizerTournamentFormat[] = [
  "single_elimination",
  "groups_playoff",
  "round_robin",
  "best_of_three_playoff"
];

function normalizeStatus(value?: string | null): OrganizerTournamentStatus {
  return statuses.includes(value as OrganizerTournamentStatus)
    ? (value as OrganizerTournamentStatus)
    : "draft";
}

function normalizeFormat(value?: string | null): OrganizerTournamentFormat {
  return formats.includes(value as OrganizerTournamentFormat)
    ? (value as OrganizerTournamentFormat)
    : "single_elimination";
}

function fallbackSlug(value: OrganizerTournamentDto) {
  if (value.slug) {
    return value.slug;
  }

  return (value.title ?? value.id)
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/(^-|-$)/g, "");
}

function mapSettings(
  settings: OrganizerTournamentSettingsDto | null | undefined,
  format: OrganizerTournamentFormat,
  maxTeams: number
): OrganizerTournamentSettings {
  return {
    allowSubstitutes: settings?.allowSubstitutes ?? true,
    bestOf: settings?.bestOf ?? 3,
    checkInEnabled: settings?.checkInEnabled ?? false,
    format: normalizeFormat(settings?.format ?? format),
    maxTeams: settings?.maxTeams ?? maxTeams,
    minTeams: settings?.minTeams ?? Math.min(2, maxTeams),
    teamSize: settings?.teamSize ?? 5
  };
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === "object" && !Array.isArray(value);
}

function asTournament(value: unknown): OrganizerTournament {
  if (!isRecord(value) || typeof value.id !== "string") {
    throw new Error("Organizer tournament API returned an unexpected tournament shape.");
  }

  const dto = value as unknown as OrganizerTournamentDto;
  const format = normalizeFormat(dto.format);
  const maxTeams = dto.maxTeams ?? dto.teamsCount ?? dto.settings?.maxTeams ?? 8;

  return {
    checkInClosesAt: dto.checkInClosesAt ?? null,
    checkInOpensAt: dto.checkInOpensAt ?? null,
    createdAt: dto.createdAt ?? null,
    description: dto.description ?? null,
    endsAt: dto.endsAt ?? null,
    format,
    id: dto.id,
    maxTeams,
    organizerNickname: dto.organizerNickname ?? null,
    organizerProfileId: dto.organizerProfileId ?? null,
    prizePool: dto.prizePool ?? null,
    publicVisible: dto.publicVisible ?? false,
    publishedAt: dto.publishedAt ?? null,
    registrationClosesAt: dto.registrationClosesAt ?? null,
    registrationOpensAt: dto.registrationOpensAt ?? null,
    registrationsCount: dto.registrationsCount ?? 0,
    rules: dto.rules ?? null,
    settings: mapSettings(dto.settings, format, maxTeams),
    slug: fallbackSlug(dto),
    startsAt: dto.startsAt ?? null,
    status: normalizeStatus(dto.status),
    teamsCount: dto.teamsCount ?? maxTeams,
    timezone: dto.timezone ?? "UTC",
    title: dto.title ?? "Untitled Tournament",
    updatedAt: dto.updatedAt ?? null
  };
}

function asTournamentList(value: unknown): OrganizerTournament[] {
  if (!Array.isArray(value)) {
    throw new Error("Organizer tournament API returned an unexpected list shape.");
  }

  return value.map(asTournament);
}

function asRegistration(value: unknown): OrganizerTournamentRegistration {
  if (!isRecord(value) || typeof value.id !== "string" || typeof value.teamId !== "string" || typeof value.tournamentId !== "string") {
    throw new Error("Organizer registration API returned an unexpected registration shape.");
  }

  const dto = value as unknown as OrganizerTournamentRegistrationDto;

  return {
    captainNickname: dto.captainNickname ?? null,
    checkedInAt: dto.checkedInAt ?? null,
    contactEmail: dto.contactEmail ?? null,
    createdAt: dto.createdAt ?? null,
    id: dto.id,
    message: dto.message ?? null,
    reviewedAt: dto.reviewedAt ?? null,
    reviewedByNickname: dto.reviewedByNickname ?? null,
    seedNumber: dto.seedNumber ?? null,
    status: dto.status ?? "pending",
    teamId: dto.teamId,
    teamName: dto.teamName ?? "Unknown team",
    teamSlug: dto.teamSlug ?? null,
    teamTag: dto.teamTag ?? null,
    tournamentId: dto.tournamentId,
    updatedAt: dto.updatedAt ?? null
  };
}

function asRegistrationList(value: unknown): OrganizerTournamentRegistration[] {
  if (!Array.isArray(value)) {
    throw new Error("Organizer registrations API returned an unexpected list shape.");
  }

  return value.map(asRegistration);
}

export async function listOrganizerTournaments() {
  return asTournamentList(await getApiAuthenticated<unknown>("/organizer/tournaments"));
}

export async function getOrganizerTournament(tournamentId: string) {
  return asTournament(
    await getApiAuthenticated<unknown>(`/organizer/tournaments/${tournamentId}`)
  );
}

export async function createOrganizerTournament(input: OrganizerTournamentPayload) {
  return asTournament(
    await postApiAuthenticated<unknown>("/organizer/tournaments", input)
  );
}

export async function updateOrganizerTournament(
  tournamentId: string,
  input: OrganizerTournamentPayload
) {
  return asTournament(
    await patchApiAuthenticated<unknown>(`/organizer/tournaments/${tournamentId}`, input)
  );
}

export async function publishOrganizerTournament(tournamentId: string) {
  return asTournament(
    await postApiAuthenticated<unknown>(`/organizer/tournaments/${tournamentId}/publish`, {})
  );
}

export async function archiveOrganizerTournament(tournamentId: string) {
  return asTournament(
    await postApiAuthenticated<unknown>(`/organizer/tournaments/${tournamentId}/archive`, {})
  );
}

export async function listOrganizerTournamentRegistrations(tournamentId: string) {
  return asRegistrationList(
    await getApiAuthenticated<unknown>(`/organizer/tournaments/${tournamentId}/registrations`)
  );
}

export async function approveOrganizerRegistration(
  tournamentId: string,
  registrationId: string,
  seedNumber?: number | null
) {
  return asRegistration(
    await postApiAuthenticated<unknown>(
      `/organizer/tournaments/${tournamentId}/registrations/${registrationId}/approve`,
      seedNumber ? { seedNumber } : {}
    )
  );
}

export async function rejectOrganizerRegistration(tournamentId: string, registrationId: string) {
  return asRegistration(
    await postApiAuthenticated<unknown>(
      `/organizer/tournaments/${tournamentId}/registrations/${registrationId}/reject`,
      {}
    )
  );
}

export async function waitlistOrganizerRegistration(tournamentId: string, registrationId: string) {
  return asRegistration(
    await postApiAuthenticated<unknown>(
      `/organizer/tournaments/${tournamentId}/registrations/${registrationId}/waitlist`,
      {}
    )
  );
}
