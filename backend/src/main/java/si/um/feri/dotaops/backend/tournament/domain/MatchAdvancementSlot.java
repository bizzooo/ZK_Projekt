package si.um.feri.dotaops.backend.tournament.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MatchAdvancementSlot(
        UUID slotId,
        UUID targetMatchId,
        UUID tournamentId,
        MatchSlotName slot,
        MatchSlotSourceType sourceType,
        UUID slotTeamId,
        boolean locked,
        UUID targetTeamAId,
        UUID targetTeamBId,
        MatchStatus targetStatus,
        int targetScoreA,
        int targetScoreB,
        UUID targetWinnerTeamId,
        OffsetDateTime targetStartedAt,
        OffsetDateTime targetFinishedAt,
        OffsetDateTime targetCancelledAt
) {

    public UUID targetTeamId() {
        return slot == MatchSlotName.TEAM_A ? targetTeamAId : targetTeamBId;
    }

    public boolean targetHasStartedOrResult() {
        return targetStatus == MatchStatus.LIVE
                || targetStatus == MatchStatus.FINISHED
                || targetStatus == MatchStatus.CANCELLED
                || targetStartedAt != null
                || targetFinishedAt != null
                || targetCancelledAt != null
                || targetScoreA != 0
                || targetScoreB != 0
                || targetWinnerTeamId != null;
    }
}
