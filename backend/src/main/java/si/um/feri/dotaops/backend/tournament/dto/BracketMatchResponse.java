package si.um.feri.dotaops.backend.tournament.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import si.um.feri.dotaops.backend.tournament.domain.BracketMatch;

public record BracketMatchResponse(
        UUID matchId,
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
        List<MatchSlotResponse> slots
) {

    public static BracketMatchResponse from(BracketMatch match) {
        return new BracketMatchResponse(
                match.id(),
                match.tournamentId(),
                match.groupId(),
                match.roundNumber(),
                match.bracketPosition(),
                match.stageName(),
                match.roundName(),
                match.status(),
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
                match.slots().stream()
                        .map(MatchSlotResponse::from)
                        .toList());
    }
}
