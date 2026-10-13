package si.um.feri.dotaops.backend.tournament.dto;

import java.util.UUID;

import si.um.feri.dotaops.backend.tournament.domain.PublicTournamentMatchSlot;

public record PublicTournamentMatchSlotResponse(
        int slotNumber,
        String sourceType,
        UUID sourceMatchId,
        PublicTeamResponse team,
        Integer seedNumber,
        boolean bye
) {

    public static PublicTournamentMatchSlotResponse from(PublicTournamentMatchSlot slot) {
        return new PublicTournamentMatchSlotResponse(
                slot.slotNumber(),
                slot.sourceType().databaseValue(),
                slot.sourceMatchId(),
                PublicTeamResponse.from(slot.team()),
                slot.seedNumber(),
                slot.bye());
    }
}
