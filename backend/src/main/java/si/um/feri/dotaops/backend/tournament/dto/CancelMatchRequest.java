package si.um.feri.dotaops.backend.tournament.dto;

import jakarta.validation.constraints.Size;

public record CancelMatchRequest(
        @Size(max = 500)
        String reason
) {
}
