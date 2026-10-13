package si.um.feri.dotaops.backend.tournament.repository;

import java.time.OffsetDateTime;
import java.util.UUID;

import si.um.feri.dotaops.backend.tournament.domain.TournamentFormat;

public record CreateTournamentCommand(
        String slug,
        String title,
        TournamentFormat format,
        UUID organizerProfileId,
        String description,
        String rules,
        String prizePool,
        int maxTeams,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        OffsetDateTime registrationOpensAt,
        OffsetDateTime registrationClosesAt,
        UUID createdBy,
        String timezone,
        OffsetDateTime checkInOpensAt,
        OffsetDateTime checkInClosesAt,
        String settingsJson
) {
}
