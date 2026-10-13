package si.um.feri.dotaops.backend.tournament.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Tournament(
        UUID id,
        String slug,
        String title,
        TournamentStatus status,
        TournamentFormat format,
        UUID organizerProfileId,
        String organizerNickname,
        String description,
        String rules,
        String prizePool,
        int maxTeams,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        OffsetDateTime registrationOpensAt,
        OffsetDateTime registrationClosesAt,
        boolean publicVisible,
        UUID createdBy,
        String timezone,
        OffsetDateTime checkInOpensAt,
        OffsetDateTime checkInClosesAt,
        OffsetDateTime publishedAt,
        TournamentSettings settings,
        long registrationsCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
