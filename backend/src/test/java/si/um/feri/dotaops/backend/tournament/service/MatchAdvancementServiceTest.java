package si.um.feri.dotaops.backend.tournament.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import si.um.feri.dotaops.backend.common.error.ConflictException;
import si.um.feri.dotaops.backend.tournament.domain.MatchAdvancementSlot;
import si.um.feri.dotaops.backend.tournament.domain.MatchSlotName;
import si.um.feri.dotaops.backend.tournament.domain.MatchSlotSourceType;
import si.um.feri.dotaops.backend.tournament.domain.MatchStatus;
import si.um.feri.dotaops.backend.tournament.domain.TournamentMatch;
import si.um.feri.dotaops.backend.tournament.repository.CreateMatchAdvancementAuditCommand;
import si.um.feri.dotaops.backend.tournament.repository.MatchAdvancementRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MatchAdvancementServiceTest {

    private static final UUID SOURCE_MATCH_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID TARGET_MATCH_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID SLOT_ID = UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final UUID TOURNAMENT_ID = UUID.fromString("44444444-4444-4444-8444-444444444444");
    private static final UUID TEAM_A_ID = UUID.fromString("55555555-5555-4555-8555-555555555555");
    private static final UUID TEAM_B_ID = UUID.fromString("66666666-6666-4666-8666-666666666666");
    private static final UUID MANUAL_TEAM_ID = UUID.fromString("77777777-7777-4777-8777-777777777777");
    private static final UUID ACTOR_PROFILE_ID = UUID.fromString("88888888-8888-4888-8888-888888888888");
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-01T18:00:00Z");

    private final MatchAdvancementRepository advancementRepository = mock(MatchAdvancementRepository.class);
    private final MatchAdvancementService service = new MatchAdvancementService(advancementRepository);

    @Test
    void winnerAdvancesIntoTargetTeamASlotAndWritesAudit() {
        TournamentMatch previous = match(MatchStatus.LIVE, TEAM_A_ID, TEAM_B_ID, null);
        TournamentMatch finished = match(MatchStatus.FINISHED, TEAM_A_ID, TEAM_B_ID, TEAM_A_ID);
        when(advancementRepository.findDependentSlots(SOURCE_MATCH_ID))
                .thenReturn(List.of(slot(MatchSlotName.TEAM_A, MatchSlotSourceType.WINNER, null, false)));

        service.advanceAfterResult(previous, finished, ACTOR_PROFILE_ID);

        verify(advancementRepository).updateSlotTeam(SLOT_ID, TEAM_A_ID);
        verify(advancementRepository).updateTargetMatchTeam(TARGET_MATCH_ID, MatchSlotName.TEAM_A, TEAM_A_ID);
        CreateMatchAdvancementAuditCommand audit = capturedAudit();
        assertThat(audit.reason()).isEqualTo("AUTO_ADVANCE_WINNER");
        assertThat(audit.sourceType()).isEqualTo(MatchSlotSourceType.WINNER);
        assertThat(audit.advancedTeamId()).isEqualTo(TEAM_A_ID);
        assertThat(audit.createdBy()).isEqualTo(ACTOR_PROFILE_ID);
    }

    @Test
    void winnerAdvancesIntoTargetTeamBSlot() {
        TournamentMatch previous = match(MatchStatus.SCHEDULED, TEAM_A_ID, TEAM_B_ID, null);
        TournamentMatch finished = match(MatchStatus.FINISHED, TEAM_A_ID, TEAM_B_ID, TEAM_B_ID);
        when(advancementRepository.findDependentSlots(SOURCE_MATCH_ID))
                .thenReturn(List.of(slot(MatchSlotName.TEAM_B, MatchSlotSourceType.WINNER, null, false)));

        service.advanceAfterResult(previous, finished, ACTOR_PROFILE_ID);

        verify(advancementRepository).updateSlotTeam(SLOT_ID, TEAM_B_ID);
        verify(advancementRepository).updateTargetMatchTeam(TARGET_MATCH_ID, MatchSlotName.TEAM_B, TEAM_B_ID);
    }

    @Test
    void loserAdvancesIntoTargetTeamASlotAndWritesAudit() {
        TournamentMatch previous = match(MatchStatus.LIVE, TEAM_A_ID, TEAM_B_ID, null);
        TournamentMatch finished = match(MatchStatus.FINISHED, TEAM_A_ID, TEAM_B_ID, TEAM_A_ID);
        when(advancementRepository.findDependentSlots(SOURCE_MATCH_ID))
                .thenReturn(List.of(slot(MatchSlotName.TEAM_A, MatchSlotSourceType.LOSER, null, false)));

        service.advanceAfterResult(previous, finished, ACTOR_PROFILE_ID);

        verify(advancementRepository).updateSlotTeam(SLOT_ID, TEAM_B_ID);
        verify(advancementRepository).updateTargetMatchTeam(TARGET_MATCH_ID, MatchSlotName.TEAM_A, TEAM_B_ID);
        CreateMatchAdvancementAuditCommand audit = capturedAudit();
        assertThat(audit.reason()).isEqualTo("AUTO_ADVANCE_LOSER");
        assertThat(audit.sourceType()).isEqualTo(MatchSlotSourceType.LOSER);
        assertThat(audit.advancedTeamId()).isEqualTo(TEAM_B_ID);
    }

    @Test
    void loserAdvancesIntoTargetTeamBSlot() {
        TournamentMatch previous = match(MatchStatus.LIVE, TEAM_A_ID, TEAM_B_ID, null);
        TournamentMatch finished = match(MatchStatus.FINISHED, TEAM_A_ID, TEAM_B_ID, TEAM_B_ID);
        when(advancementRepository.findDependentSlots(SOURCE_MATCH_ID))
                .thenReturn(List.of(slot(MatchSlotName.TEAM_B, MatchSlotSourceType.LOSER, null, false)));

        service.advanceAfterResult(previous, finished, ACTOR_PROFILE_ID);

        verify(advancementRepository).updateSlotTeam(SLOT_ID, TEAM_A_ID);
        verify(advancementRepository).updateTargetMatchTeam(TARGET_MATCH_ID, MatchSlotName.TEAM_B, TEAM_A_ID);
    }

    @Test
    void byeWinnerAdvancesAndMissingLoserIsIgnored() {
        TournamentMatch bye = match(MatchStatus.FINISHED, TEAM_A_ID, null, TEAM_A_ID);
        when(advancementRepository.findDependentSlots(SOURCE_MATCH_ID)).thenReturn(List.of(
                slot(MatchSlotName.TEAM_A, MatchSlotSourceType.WINNER, null, false),
                slot(UUID.fromString("99999999-9999-4999-8999-999999999999"), MatchSlotName.TEAM_B, MatchSlotSourceType.LOSER, null, false)));

        service.advanceAfterResult(bye, bye, ACTOR_PROFILE_ID);

        verify(advancementRepository).updateSlotTeam(SLOT_ID, TEAM_A_ID);
        verify(advancementRepository).updateTargetMatchTeam(TARGET_MATCH_ID, MatchSlotName.TEAM_A, TEAM_A_ID);
    }

    @Test
    void lockedSlotIsNotOverwritten() {
        TournamentMatch finished = match(MatchStatus.FINISHED, TEAM_A_ID, TEAM_B_ID, TEAM_A_ID);
        when(advancementRepository.findDependentSlots(SOURCE_MATCH_ID))
                .thenReturn(List.of(slot(MatchSlotName.TEAM_A, MatchSlotSourceType.WINNER, null, true)));

        assertThatThrownBy(() -> service.advanceAfterResult(finished, finished, ACTOR_PROFILE_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Automatic advancement cannot write to a locked match slot.");
        verifyNoAdvancementWrites();
    }

    @Test
    void manualSlotWithExistingTeamIsNotOverwritten() {
        TournamentMatch finished = match(MatchStatus.FINISHED, TEAM_A_ID, TEAM_B_ID, TEAM_A_ID);
        when(advancementRepository.findDependentSlots(SOURCE_MATCH_ID))
                .thenReturn(List.of(slot(MatchSlotName.TEAM_A, MatchSlotSourceType.MANUAL, MANUAL_TEAM_ID, false)));

        assertThatThrownBy(() -> service.advanceAfterResult(finished, finished, ACTOR_PROFILE_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Manual match slots cannot be overwritten by automatic advancement.");
        verifyNoAdvancementWrites();
    }

    @Test
    void resultChangeUpdatesEmptyFutureMatchConsistently() {
        TournamentMatch previous = match(MatchStatus.FINISHED, TEAM_A_ID, TEAM_B_ID, TEAM_B_ID);
        TournamentMatch changed = match(MatchStatus.FINISHED, TEAM_A_ID, TEAM_B_ID, TEAM_A_ID);
        when(advancementRepository.findDependentSlots(SOURCE_MATCH_ID))
                .thenReturn(List.of(slot(MatchSlotName.TEAM_A, MatchSlotSourceType.WINNER, TEAM_B_ID, false, MatchStatus.SCHEDULED)));

        service.advanceAfterResult(previous, changed, ACTOR_PROFILE_ID);

        verify(advancementRepository).updateSlotTeam(SLOT_ID, TEAM_A_ID);
        verify(advancementRepository).updateTargetMatchTeam(TARGET_MATCH_ID, MatchSlotName.TEAM_A, TEAM_A_ID);
        assertThat(capturedAudit().reason()).isEqualTo("RESULT_CHANGE_PROPAGATION");
    }

    @Test
    void resultChangeIsRejectedWhenDownstreamMatchIsLive() {
        TournamentMatch previous = match(MatchStatus.FINISHED, TEAM_A_ID, TEAM_B_ID, TEAM_B_ID);
        TournamentMatch changed = match(MatchStatus.FINISHED, TEAM_A_ID, TEAM_B_ID, TEAM_A_ID);
        when(advancementRepository.findDependentSlots(SOURCE_MATCH_ID))
                .thenReturn(List.of(slot(MatchSlotName.TEAM_A, MatchSlotSourceType.WINNER, TEAM_B_ID, false, MatchStatus.LIVE)));

        assertThatThrownBy(() -> service.advanceAfterResult(previous, changed, ACTOR_PROFILE_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Downstream match is already affected by the previous result; result change requires confirmation.");
        verifyNoAdvancementWrites();
    }

    @Test
    void resultChangeIsRejectedWhenDownstreamMatchIsFinished() {
        TournamentMatch previous = match(MatchStatus.FINISHED, TEAM_A_ID, TEAM_B_ID, TEAM_B_ID);
        TournamentMatch changed = match(MatchStatus.FINISHED, TEAM_A_ID, TEAM_B_ID, TEAM_A_ID);
        when(advancementRepository.findDependentSlots(SOURCE_MATCH_ID))
                .thenReturn(List.of(slot(MatchSlotName.TEAM_A, MatchSlotSourceType.WINNER, TEAM_B_ID, false, MatchStatus.FINISHED)));

        assertThatThrownBy(() -> service.advanceAfterResult(previous, changed, ACTOR_PROFILE_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Downstream match is already affected by the previous result; result change requires confirmation.");
        verifyNoAdvancementWrites();
    }

    private CreateMatchAdvancementAuditCommand capturedAudit() {
        ArgumentCaptor<CreateMatchAdvancementAuditCommand> captor =
                ArgumentCaptor.forClass(CreateMatchAdvancementAuditCommand.class);
        verify(advancementRepository).insertAudit(captor.capture());
        return captor.getValue();
    }

    private void verifyNoAdvancementWrites() {
        verify(advancementRepository, never()).updateSlotTeam(any(UUID.class), any(UUID.class));
        verify(advancementRepository, never()).updateTargetMatchTeam(any(UUID.class), any(MatchSlotName.class), any(UUID.class));
        verify(advancementRepository, never()).insertAudit(any(CreateMatchAdvancementAuditCommand.class));
    }

    private static MatchAdvancementSlot slot(
            MatchSlotName slotName,
            MatchSlotSourceType sourceType,
            UUID slotTeamId,
            boolean locked
    ) {
        return slot(slotName, sourceType, slotTeamId, locked, MatchStatus.SCHEDULED);
    }

    private static MatchAdvancementSlot slot(
            MatchSlotName slotName,
            MatchSlotSourceType sourceType,
            UUID slotTeamId,
            boolean locked,
            MatchStatus targetStatus
    ) {
        return slot(SLOT_ID, slotName, sourceType, slotTeamId, locked, targetStatus);
    }

    private static MatchAdvancementSlot slot(
            UUID slotId,
            MatchSlotName slotName,
            MatchSlotSourceType sourceType,
            UUID slotTeamId,
            boolean locked
    ) {
        return slot(slotId, slotName, sourceType, slotTeamId, locked, MatchStatus.SCHEDULED);
    }

    private static MatchAdvancementSlot slot(
            UUID slotId,
            MatchSlotName slotName,
            MatchSlotSourceType sourceType,
            UUID slotTeamId,
            boolean locked,
            MatchStatus targetStatus
    ) {
        UUID targetTeamAId = slotName == MatchSlotName.TEAM_A ? slotTeamId : null;
        UUID targetTeamBId = slotName == MatchSlotName.TEAM_B ? slotTeamId : null;
        return new MatchAdvancementSlot(
                slotId,
                TARGET_MATCH_ID,
                TOURNAMENT_ID,
                slotName,
                sourceType,
                slotTeamId,
                locked,
                targetTeamAId,
                targetTeamBId,
                targetStatus,
                0,
                0,
                null,
                targetStatus == MatchStatus.LIVE ? NOW : null,
                targetStatus == MatchStatus.FINISHED ? NOW.plusHours(1) : null,
                targetStatus == MatchStatus.CANCELLED ? NOW.plusMinutes(30) : null);
    }

    private static TournamentMatch match(
            MatchStatus status,
            UUID teamAId,
            UUID teamBId,
            UUID winnerTeamId
    ) {
        return new TournamentMatch(
                SOURCE_MATCH_ID,
                TOURNAMENT_ID,
                null,
                1,
                1,
                "Playoffs",
                "Semifinal",
                status,
                teamAId,
                "Team A",
                teamBId,
                "Team B",
                winnerTeamId == null ? 0 : winnerTeamId.equals(teamAId) ? 2 : 1,
                winnerTeamId == null ? 0 : winnerTeamId.equals(teamBId) ? 2 : 1,
                winnerTeamId,
                null,
                3,
                null,
                null,
                status == MatchStatus.FINISHED ? NOW : null,
                null,
                null,
                NOW.minusDays(1),
                NOW);
    }
}
