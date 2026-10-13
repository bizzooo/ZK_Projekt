package si.um.feri.dotaops.backend.tournament.dto;

import java.util.UUID;

import si.um.feri.dotaops.backend.tournament.domain.BracketMatchSlot;

public record MatchSlotResponse(
        int slotNumber,
        String sourceType,
        UUID teamId,
        String teamName,
        Integer seedNumber,
        UUID sourceMatchId,
        boolean bye
) {

    public static MatchSlotResponse from(BracketMatchSlot slot) {
        return new MatchSlotResponse(
                slot.slotNumber(),
                slot.sourceType().databaseValue(),
                slot.teamId(),
                slot.teamName(),
                slot.seedNumber(),
                slot.sourceMatchId(),
                slot.bye());
    }
}
