package si.um.feri.dotaops.backend.tournament.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TournamentGroup(
        UUID id,
        UUID tournamentId,
        String name,
        int sortOrder,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
