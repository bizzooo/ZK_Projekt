package si.um.feri.dotaops.backend.tournament.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTournamentGroupRequest(
        @NotBlank
        @Size(max = 80)
        String name,

        @Min(1)
        Integer sortOrder
) {
}
