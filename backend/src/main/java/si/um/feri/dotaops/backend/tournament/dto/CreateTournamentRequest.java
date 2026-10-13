package si.um.feri.dotaops.backend.tournament.dto;

import java.time.OffsetDateTime;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import si.um.feri.dotaops.backend.common.validation.ValidationMessages;
import si.um.feri.dotaops.backend.tournament.domain.TournamentFormat;

public record CreateTournamentRequest(
        @NotBlank(message = ValidationMessages.REQUIRED)
        @Size(min = 2, max = 120)
        String title,

        @Size(max = 80)
        @Pattern(regexp = "^[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*$", message = ValidationMessages.SLUG)
        String slug,

        TournamentFormat format,

        @Size(max = 2000)
        String description,

        @Size(max = 5000)
        String rules,

        @Size(max = 120)
        String prizePool,

        @Min(2)
        @Max(128)
        Integer maxTeams,

        OffsetDateTime startsAt,

        OffsetDateTime endsAt,

        OffsetDateTime registrationOpensAt,

        OffsetDateTime registrationClosesAt,

        @Size(min = 1, max = 80)
        String timezone,

        OffsetDateTime checkInOpensAt,

        OffsetDateTime checkInClosesAt,

        @Valid
        TournamentSettingsDto settings
) {
}
