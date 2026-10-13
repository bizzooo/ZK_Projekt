package si.um.feri.dotaops.backend.tournament.dto;

import jakarta.validation.constraints.Size;

public record GenerateBracketRequest(
        @Size(max = 80)
        String stageName,

        Boolean forceRegenerate
) {
}
