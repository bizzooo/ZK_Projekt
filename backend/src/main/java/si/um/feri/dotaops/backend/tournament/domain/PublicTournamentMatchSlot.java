package si.um.feri.dotaops.backend.tournament.domain;

import java.util.UUID;

public record PublicTournamentMatchSlot(
        UUID id,
        UUID matchId,
        int slotNumber,
        MatchSlotSourceType sourceType,
        PublicTournamentTeam team,
        Integer seedNumber,
        UUID sourceMatchId,
        boolean bye
) {
}
