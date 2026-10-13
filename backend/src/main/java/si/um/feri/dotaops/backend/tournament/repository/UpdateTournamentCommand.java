package si.um.feri.dotaops.backend.tournament.repository;

import java.time.OffsetDateTime;

import si.um.feri.dotaops.backend.tournament.domain.TournamentFormat;

public record UpdateTournamentCommand(
        boolean titlePresent,
        String title,
        boolean slugPresent,
        String slug,
        boolean formatPresent,
        TournamentFormat format,
        boolean descriptionPresent,
        String description,
        boolean rulesPresent,
        String rules,
        boolean prizePoolPresent,
        String prizePool,
        boolean maxTeamsPresent,
        Integer maxTeams,
        boolean startsAtPresent,
        OffsetDateTime startsAt,
        boolean endsAtPresent,
        OffsetDateTime endsAt,
        boolean registrationOpensAtPresent,
        OffsetDateTime registrationOpensAt,
        boolean registrationClosesAtPresent,
        OffsetDateTime registrationClosesAt,
        boolean timezonePresent,
        String timezone,
        boolean checkInOpensAtPresent,
        OffsetDateTime checkInOpensAt,
        boolean checkInClosesAtPresent,
        OffsetDateTime checkInClosesAt,
        boolean settingsPresent,
        String settingsJson
) {
}
