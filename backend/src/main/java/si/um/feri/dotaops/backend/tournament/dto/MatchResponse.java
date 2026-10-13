package si.um.feri.dotaops.backend.tournament.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import si.um.feri.dotaops.backend.tournament.domain.TournamentMatch;

public record MatchResponse(
        UUID id,
        UUID tournamentId,
        UUID groupId,
        String status,
        UUID teamAId,
        String teamAName,
        UUID teamBId,
        String teamBName,
        int scoreA,
        int scoreB,
        UUID winnerTeamId,
        String winnerTeamName,
        int bestOf,
        OffsetDateTime scheduledAt,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        OffsetDateTime cancelledAt,
        String cancellationReason,
        int roundNumber,
        Integer bracketPosition,
        String stageName,
        String roundName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static MatchResponse from(TournamentMatch match) {
        return new MatchResponse(
                match.id(),
                match.tournamentId(),
                match.groupId(),
                match.status().databaseValue(),
                match.teamAId(),
                match.teamAName(),
                match.teamBId(),
                match.teamBName(),
                match.scoreA(),
                match.scoreB(),
                match.winnerTeamId(),
                match.winnerTeamName(),
                match.bestOf(),
                match.scheduledAt(),
                match.startedAt(),
                match.finishedAt(),
                match.cancelledAt(),
                match.cancellationReason(),
                match.roundNumber(),
                match.bracketPosition(),
                match.stageName(),
                match.roundName(),
                match.createdAt(),
                match.updatedAt());
    }
}
