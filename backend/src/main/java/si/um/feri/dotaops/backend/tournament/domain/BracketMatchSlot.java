package si.um.feri.dotaops.backend.tournament.domain;

import java.util.UUID;

public record BracketMatchSlot(
        UUID id,
        UUID matchId,
        int slotNumber,
        MatchSlotSourceType sourceType,
        UUID teamId,
        String teamName,
        Integer seedNumber,
        UUID sourceMatchId,
        boolean bye
) {
}
