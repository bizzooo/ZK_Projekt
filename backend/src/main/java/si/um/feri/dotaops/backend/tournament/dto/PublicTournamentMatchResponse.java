package si.um.feri.dotaops.backend.tournament.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import si.um.feri.dotaops.backend.tournament.domain.PublicTournamentMatch;

public record PublicTournamentMatchResponse(
        UUID matchId,
        UUID tournamentId,
        UUID groupId,
        String groupName,
        int roundNumber,
        String roundName,
        Integer bracketPosition,
        String stageName,
        String status,
        int bestOf,
        PublicTeamResponse teamA,
        PublicTeamResponse teamB,
        int scoreA,
        int scoreB,
        UUID winnerTeamId,
        String winnerTeamName,
        PublicTeamResponse winnerTeam,
        OffsetDateTime scheduledAt,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        OffsetDateTime cancelledAt,
        String cancellationReason,
        List<PublicTournamentMatchSlotResponse> slots
) {

    public static PublicTournamentMatchResponse from(PublicTournamentMatch match) {
        return new PublicTournamentMatchResponse(
                match.id(),
                match.tournamentId(),
                match.groupId(),
                match.groupName(),
                match.roundNumber(),
                match.roundName(),
                match.bracketPosition(),
                match.stageName(),
                match.status().databaseValue(),
                match.bestOf(),
                PublicTeamResponse.from(match.teamA()),
                PublicTeamResponse.from(match.teamB()),
                match.scoreA(),
                match.scoreB(),
                match.winnerTeam() == null ? null : match.winnerTeam().id(),
                match.winnerTeam() == null ? null : match.winnerTeam().name(),
                PublicTeamResponse.from(match.winnerTeam()),
                match.scheduledAt(),
                match.startedAt(),
                match.finishedAt(),
                match.cancelledAt(),
                match.cancellationReason(),
                match.slots().stream()
                        .map(PublicTournamentMatchSlotResponse::from)
                        .toList());
    }
}
