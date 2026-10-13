package si.um.feri.dotaops.backend.tournament.repository;

import java.util.UUID;

import si.um.feri.dotaops.backend.tournament.domain.MatchSlotName;
import si.um.feri.dotaops.backend.tournament.domain.MatchSlotSourceType;

public record CreateMatchSlotCommand(
        UUID matchId,
        MatchSlotName slot,
        MatchSlotSourceType sourceType,
        UUID teamId,
        UUID sourceMatchId,
        UUID sourceRegistrationId,
        Integer seedNumber,
        String displayLabel
) {
}
