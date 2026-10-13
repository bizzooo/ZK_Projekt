package si.um.feri.dotaops.backend.tournament.service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import si.um.feri.dotaops.backend.common.error.ConflictException;
import si.um.feri.dotaops.backend.tournament.domain.MatchAdvancementSlot;
import si.um.feri.dotaops.backend.tournament.domain.MatchSlotSourceType;
import si.um.feri.dotaops.backend.tournament.domain.MatchStatus;
import si.um.feri.dotaops.backend.tournament.domain.TournamentMatch;
import si.um.feri.dotaops.backend.tournament.repository.CreateMatchAdvancementAuditCommand;
import si.um.feri.dotaops.backend.tournament.repository.MatchAdvancementRepository;

@Service
public class MatchAdvancementService {

    private static final String AUTO_ADVANCE_WINNER = "AUTO_ADVANCE_WINNER";
    private static final String AUTO_ADVANCE_LOSER = "AUTO_ADVANCE_LOSER";
    private static final String RESULT_CHANGE_PROPAGATION = "RESULT_CHANGE_PROPAGATION";

    private final MatchAdvancementRepository advancementRepository;

    public MatchAdvancementService(MatchAdvancementRepository advancementRepository) {
        this.advancementRepository = advancementRepository;
    }

    @Transactional
    public void advanceAfterResult(
            TournamentMatch previousMatch,
            TournamentMatch finishedMatch,
            UUID actorProfileId
    ) {
        if (finishedMatch.status() != MatchStatus.FINISHED) {
            throw new ConflictException("Only finished matches can advance teams.");
        }

        UUID winnerTeamId = validateWinner(finishedMatch);
        UUID loserTeamId = loserTeamId(finishedMatch, winnerTeamId);
        UUID previousWinnerTeamId = validPreviousWinner(previousMatch);
        UUID previousLoserTeamId = previousWinnerTeamId == null ? null : loserTeamId(previousMatch, previousWinnerTeamId);
        List<MatchAdvancementSlot> dependentSlots = advancementRepository.findDependentSlots(finishedMatch.id());

        for (MatchAdvancementSlot slot : dependentSlots) {
            advanceSlot(
                    previousMatch,
                    finishedMatch,
                    slot,
                    winnerTeamId,
                    loserTeamId,
                    previousWinnerTeamId,
                    previousLoserTeamId,
                    actorProfileId);
        }
    }

    private void advanceSlot(
            TournamentMatch previousMatch,
            TournamentMatch finishedMatch,
            MatchAdvancementSlot slot,
            UUID winnerTeamId,
            UUID loserTeamId,
            UUID previousWinnerTeamId,
            UUID previousLoserTeamId,
            UUID actorProfileId
    ) {
        if (slot.sourceType() == MatchSlotSourceType.MANUAL && slot.slotTeamId() != null) {
            throw new ConflictException("Manual match slots cannot be overwritten by automatic advancement.");
        }

        UUID advancedTeamId = advancingTeam(slot.sourceType(), winnerTeamId, loserTeamId);
        if (advancedTeamId == null) {
            return;
        }

        UUID previousAdvancedTeamId = advancingTeam(slot.sourceType(), previousWinnerTeamId, previousLoserTeamId);
        UUID currentTeamId = currentTeamId(slot);
        boolean teamWouldChange = currentTeamId != null && !currentTeamId.equals(advancedTeamId);
        boolean currentTeamCameFromPreviousResult = previousAdvancedTeamId != null
                && previousAdvancedTeamId.equals(currentTeamId);

        if (slot.locked()) {
            throw new ConflictException("Automatic advancement cannot write to a locked match slot.");
        }

        if (teamWouldChange && !currentTeamCameFromPreviousResult) {
            throw new ConflictException("Automatic advancement cannot overwrite a manually assigned match slot.");
        }

        if (teamWouldChange && slot.targetHasStartedOrResult()) {
            throw new ConflictException(
                    "Downstream match is already affected by the previous result; result change requires confirmation.");
        }

        boolean slotNeedsUpdate = !Objects.equals(slot.slotTeamId(), advancedTeamId);
        boolean matchNeedsUpdate = !Objects.equals(slot.targetTeamId(), advancedTeamId);
        if (!slotNeedsUpdate && !matchNeedsUpdate) {
            return;
        }

        advancementRepository.updateSlotTeam(slot.slotId(), advancedTeamId);
        advancementRepository.updateTargetMatchTeam(slot.targetMatchId(), slot.slot(), advancedTeamId);
        advancementRepository.insertAudit(new CreateMatchAdvancementAuditCommand(
                finishedMatch.tournamentId(),
                finishedMatch.id(),
                slot.targetMatchId(),
                slot.slot(),
                slot.sourceType(),
                advancedTeamId,
                currentTeamId,
                auditReason(slot.sourceType(), previousMatch, previousAdvancedTeamId, advancedTeamId),
                auditMessage(finishedMatch.id(), slot, advancedTeamId),
                actorProfileId));
    }

    private UUID validateWinner(TournamentMatch match) {
        UUID winnerTeamId = match.winnerTeamId();
        if (winnerTeamId == null) {
            throw new ConflictException("Finished match must have a winner before advancement.");
        }

        if (!winnerTeamId.equals(match.teamAId()) && !winnerTeamId.equals(match.teamBId())) {
            throw new ConflictException("Finished match winner must be one of the match teams before advancement.");
        }

        return winnerTeamId;
    }

    private UUID validPreviousWinner(TournamentMatch match) {
        if (match == null || match.winnerTeamId() == null) {
            return null;
        }

        if (!match.winnerTeamId().equals(match.teamAId()) && !match.winnerTeamId().equals(match.teamBId())) {
            return null;
        }

        return match.winnerTeamId();
    }

    private UUID loserTeamId(TournamentMatch match, UUID winnerTeamId) {
        if (match.teamAId() == null || match.teamBId() == null) {
            return null;
        }

        return winnerTeamId.equals(match.teamAId()) ? match.teamBId() : match.teamAId();
    }

    private UUID advancingTeam(MatchSlotSourceType sourceType, UUID winnerTeamId, UUID loserTeamId) {
        return switch (sourceType) {
            case WINNER -> winnerTeamId;
            case LOSER -> loserTeamId;
            case MANUAL, SEED, BYE -> null;
        };
    }

    private UUID currentTeamId(MatchAdvancementSlot slot) {
        return slot.slotTeamId() == null ? slot.targetTeamId() : slot.slotTeamId();
    }

    private String auditReason(
            MatchSlotSourceType sourceType,
            TournamentMatch previousMatch,
            UUID previousAdvancedTeamId,
            UUID advancedTeamId
    ) {
        if (previousMatch != null
                && previousMatch.status() == MatchStatus.FINISHED
                && previousAdvancedTeamId != null
                && !previousAdvancedTeamId.equals(advancedTeamId)) {
            return RESULT_CHANGE_PROPAGATION;
        }

        return sourceType == MatchSlotSourceType.LOSER ? AUTO_ADVANCE_LOSER : AUTO_ADVANCE_WINNER;
    }

    private String auditMessage(UUID sourceMatchId, MatchAdvancementSlot slot, UUID advancedTeamId) {
        return "Advanced team %s from match %s to match %s slot %s as %s."
                .formatted(
                        advancedTeamId,
                        sourceMatchId,
                        slot.targetMatchId(),
                        slot.slot().databaseValue(),
                        slot.sourceType().databaseValue());
    }
}
