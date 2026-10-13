package si.um.feri.dotaops.backend.tournament.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddTeamToGroupRequest(
        @NotNull
        UUID teamId,

        @Min(1)
        Integer seedNumber
) {
}
