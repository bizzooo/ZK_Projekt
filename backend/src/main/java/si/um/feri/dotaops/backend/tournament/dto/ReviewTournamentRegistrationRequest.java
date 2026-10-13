package si.um.feri.dotaops.backend.tournament.dto;

import jakarta.validation.constraints.Min;

public record ReviewTournamentRegistrationRequest(
        @Min(1)
        Integer seedNumber
) {
}
