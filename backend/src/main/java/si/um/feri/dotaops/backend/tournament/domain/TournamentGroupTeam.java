package si.um.feri.dotaops.backend.tournament.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TournamentGroupTeam(
        UUID id,
        UUID groupId,
        UUID tournamentId,
        UUID teamId,
        String teamName,
        String teamTag,
        String teamSlug,
        UUID registrationId,
        Integer seedNumber,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
