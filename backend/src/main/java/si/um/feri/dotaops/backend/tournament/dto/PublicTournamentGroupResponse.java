package si.um.feri.dotaops.backend.tournament.dto;

import java.util.List;
import java.util.UUID;

import si.um.feri.dotaops.backend.tournament.domain.PublicTournamentGroup;

public record PublicTournamentGroupResponse(
        UUID groupId,
        UUID tournamentId,
        String groupName,
        int displayOrder,
        List<PublicTeamResponse> teams
) {

    public static PublicTournamentGroupResponse from(PublicTournamentGroup group) {
        return new PublicTournamentGroupResponse(
                group.id(),
                group.tournamentId(),
                group.name(),
                group.sortOrder(),
                group.teams().stream()
                        .map(PublicTeamResponse::from)
                        .toList());
    }
}
