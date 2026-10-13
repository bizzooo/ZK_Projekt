package si.um.feri.dotaops.backend.tournament.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import si.um.feri.dotaops.backend.tournament.domain.TournamentGroup;

public record TournamentGroupResponse(
        UUID id,
        UUID tournamentId,
        String name,
        int sortOrder,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static TournamentGroupResponse from(TournamentGroup group) {
        return new TournamentGroupResponse(
                group.id(),
                group.tournamentId(),
                group.name(),
                group.sortOrder(),
                group.createdAt(),
                group.updatedAt());
    }
}
