package si.um.feri.dotaops.backend.tournament.repository;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateBracketMatchCommand(
        UUID tournamentId,
        String stageName,
        String roundName,
        int roundNumber,
        int bracketPosition,
        String status,
        int bestOf,
        UUID teamAId,
        UUID teamBId,
        UUID winnerTeamId,
        OffsetDateTime finishedAt
) {
}
