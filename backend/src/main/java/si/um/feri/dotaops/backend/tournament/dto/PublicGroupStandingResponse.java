package si.um.feri.dotaops.backend.tournament.dto;

import java.util.UUID;

import si.um.feri.dotaops.backend.tournament.domain.PublicGroupStanding;

public record PublicGroupStandingResponse(
        UUID groupId,
        String groupName,
        UUID tournamentId,
        UUID teamId,
        String teamName,
        int rank,
        int matchesPlayed,
        int matchWins,
        int matchLosses,
        int matchDraws,
        int gameWins,
        int gameLosses,
        int gameDiff,
        int points
) {

    public static PublicGroupStandingResponse from(PublicGroupStanding standing) {
        return new PublicGroupStandingResponse(
                standing.groupId(),
                standing.groupName(),
                standing.tournamentId(),
                standing.teamId(),
                standing.teamName(),
                standing.rank(),
                standing.matchesPlayed(),
                standing.matchWins(),
                standing.matchLosses(),
                standing.matchDraws(),
                standing.gameWins(),
                standing.gameLosses(),
                standing.gameDiff(),
                standing.points());
    }
}
