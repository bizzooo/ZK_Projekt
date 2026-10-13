package si.um.feri.dotaops.backend.tournament.dto;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTournamentRegistrationRequest(
        @NotNull
        UUID teamId,

        @Size(max = 1000)
        String message,

        @Email
        @Size(max = 254)
        String contactEmail
) {
}
