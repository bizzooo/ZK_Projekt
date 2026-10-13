package si.um.feri.dotaops.backend.tournament.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import si.um.feri.dotaops.backend.tournament.domain.PublicTournamentMetrics;

public record PublicTournamentMetricsResponse(
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

    public static PublicTournamentMetricsResponse from(PublicTournamentMetrics metrics) {
        return new PublicTournamentMetricsResponse(
                metrics.tournamentId(),
                metrics.teamCount(),
                metrics.approvedTeamCount(),
                metrics.groupCount(),
                metrics.matchCount(),
                metrics.scheduledMatchCount(),
                metrics.liveMatchCount(),
                metrics.finishedMatchCount(),
                metrics.cancelledMatchCount(),
                metrics.totalGamesPlayed(),
                metrics.totalSeriesPlayed(),
                metrics.averageGamesPerFinishedMatch(),
                metrics.nextScheduledMatchAt(),
                metrics.lastResultAt());
    }
}
