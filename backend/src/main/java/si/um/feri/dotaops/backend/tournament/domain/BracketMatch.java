package si.um.feri.dotaops.backend.tournament.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record BracketMatch(
        UUID id,
        UUID tournamentId,
        UUID groupId,
        int roundNumber,
        int bracketPosition,
        String stageName,
        String roundName,
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
        List<BracketMatchSlot> slots
) {
}
