package si.um.feri.dotaops.backend.tournament.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import si.um.feri.dotaops.backend.tournament.domain.TournamentGroupTeam;

public record TournamentGroupTeamResponse(
        UUID id,
        UUID groupId,
        UUID tournamentId,
        UUID teamId,
        String teamName,
        String teamTag,
        String teamSlug,
        UUID registrationId,
        Integer seedNumber,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static TournamentGroupTeamResponse from(TournamentGroupTeam team) {
        return new TournamentGroupTeamResponse(
                team.id(),
                team.groupId(),
                team.tournamentId(),
                team.teamId(),
                team.teamName(),
                team.teamTag(),
                team.teamSlug(),
                team.registrationId(),
                team.seedNumber(),
                team.createdAt(),
                team.updatedAt());
    }
}
