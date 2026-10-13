package si.um.feri.dotaops.backend.tournament.dto;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotNull;

public record ScheduleMatchRequest(
        @NotNull
        OffsetDateTime scheduledAt
) {
}
