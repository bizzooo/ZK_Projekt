package si.um.feri.dotaops.backend.tournament.dto;

import java.util.UUID;

import si.um.feri.dotaops.backend.tournament.domain.GroupStanding;

public record GroupStandingResponse(
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

    public static GroupStandingResponse from(GroupStanding standing) {
        return new GroupStandingResponse(
                standing.groupId(),
                standing.groupName(),
                standing.tournamentId(),
                standing.teamId(),
                standing.teamName(),
                standing.matchesPlayed(),
                standing.matchWins(),
                standing.matchLosses(),
                standing.matchDraws(),
                standing.gameWins(),
                standing.gameLosses(),
                standing.gameDiff(),
                standing.points(),
                standing.rank());
    }
}
