package si.um.feri.dotaops.backend.tournament.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TournamentMatch(
        UUID id,
        UUID tournamentId,
        UUID groupId,
        int roundNumber,
        Integer bracketPosition,
        String stageName,
        String roundName,
        MatchStatus status,
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
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
