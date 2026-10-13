package si.um.feri.dotaops.backend.tournament.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SubmitMatchResultRequest(
        @NotNull
        @Min(0)
        Integer scoreA,

        @NotNull
        @Min(0)
        Integer scoreB,

        @NotNull
        UUID winnerTeamId
) {
}
