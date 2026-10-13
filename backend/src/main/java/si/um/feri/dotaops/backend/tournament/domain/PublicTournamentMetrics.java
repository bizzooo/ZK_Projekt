package si.um.feri.dotaops.backend.tournament.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PublicTournamentMetrics(
        UUID tournamentId,
        int teamCount,
        int approvedTeamCount,
        int groupCount,
        int matchCount,
        int scheduledMatchCount,
        int liveMatchCount,
        int finishedMatchCount,
        int cancelledMatchCount,
        int totalGamesPlayed,
        int totalSeriesPlayed,
        BigDecimal averageGamesPerFinishedMatch,
        OffsetDateTime nextScheduledMatchAt,
        OffsetDateTime lastResultAt
) {
}
