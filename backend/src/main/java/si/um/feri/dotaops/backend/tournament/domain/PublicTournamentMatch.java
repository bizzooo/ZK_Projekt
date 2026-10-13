package si.um.feri.dotaops.backend.tournament.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PublicTournamentMatch(
        UUID id,
        UUID tournamentId,
        UUID groupId,
        String groupName,
        int roundNumber,
        Integer bracketPosition,
        String stageName,
        String roundName,
        MatchStatus status,
        int bestOf,
        PublicTournamentTeam teamA,
        PublicTournamentTeam teamB,
        int scoreA,
        int scoreB,
        PublicTournamentTeam winnerTeam,
        OffsetDateTime scheduledAt,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        OffsetDateTime cancelledAt,
        String cancellationReason,
        List<PublicTournamentMatchSlot> slots
) {
}
