package si.um.feri.dotaops.backend.tournament.repository;

import java.util.UUID;

import si.um.feri.dotaops.backend.tournament.domain.MatchSlotName;
import si.um.feri.dotaops.backend.tournament.domain.MatchSlotSourceType;

public record CreateMatchAdvancementAuditCommand(
        UUID tournamentId,
        UUID sourceMatchId,
        UUID targetMatchId,
        MatchSlotName targetSlot,
        MatchSlotSourceType sourceType,
        UUID advancedTeamId,
        UUID previousTeamId,
        String reason,
        String message,
        UUID createdBy
) {
}
