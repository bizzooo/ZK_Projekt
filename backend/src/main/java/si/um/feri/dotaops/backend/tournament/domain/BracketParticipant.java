package si.um.feri.dotaops.backend.tournament.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BracketParticipant(
        UUID registrationId,
        UUID teamId,
        String teamName,
        Integer seedNumber,
        OffsetDateTime registeredAt
) {
}
