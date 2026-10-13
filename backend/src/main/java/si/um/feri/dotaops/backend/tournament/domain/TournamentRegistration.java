package si.um.feri.dotaops.backend.tournament.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TournamentRegistration(
        UUID id,
        UUID tournamentId,
        String tournamentSlug,
        String tournamentTitle,
        UUID teamId,
        String teamName,
        String teamTag,
        String teamSlug,
        UUID captainProfileId,
        String captainNickname,
        TournamentRegistrationStatus status,
        String message,
        UUID reviewedBy,
        String reviewedByNickname,
        OffsetDateTime reviewedAt,
        Integer seedNumber,
        OffsetDateTime checkedInAt,
        String contactEmail,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
