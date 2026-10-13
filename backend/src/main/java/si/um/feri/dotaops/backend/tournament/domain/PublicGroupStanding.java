package si.um.feri.dotaops.backend.tournament.domain;

import java.util.UUID;

public record PublicGroupStanding(
        UUID groupId,
        String groupName,
        UUID tournamentId,
        UUID teamId,
        String teamName,
        int matchesPlayed,
        int matchWins,
        int matchLosses,
        int matchDraws,
        int gameWins,
        int gameLosses,
        int gameDiff,
        int points,
        int rank
) {
}
