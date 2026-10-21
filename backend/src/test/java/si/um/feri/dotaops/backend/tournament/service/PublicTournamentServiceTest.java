package si.um.feri.dotaops.backend.tournament.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import si.um.feri.dotaops.backend.common.error.ResourceNotFoundException;
import si.um.feri.dotaops.backend.tournament.domain.MatchStatus;
import si.um.feri.dotaops.backend.tournament.domain.PublicTournamentMatch;
import si.um.feri.dotaops.backend.tournament.domain.Tournament;
import si.um.feri.dotaops.backend.tournament.repository.PublicTournamentRepository;
import si.um.feri.dotaops.backend.tournament.repository.TournamentRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicTournamentServiceTest {

    private static final UUID TOURNAMENT_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID FIRST_MATCH_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID SECOND_MATCH_ID = UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final UUID THIRD_MATCH_ID = UUID.fromString("44444444-4444-4444-8444-444444444444");
    private static final UUID FOURTH_MATCH_ID = UUID.fromString("55555555-5555-4555-8555-555555555555");

    private final TournamentRepository tournamentRepository = mock(TournamentRepository.class);
    private final PublicTournamentRepository publicTournamentRepository = mock(PublicTournamentRepository.class);
    private final PublicTournamentService service = new PublicTournamentService(
            tournamentRepository,
            publicTournamentRepository);

    @Test
    void publicTournamentListClampsPageAndSizeToSupportedRange() {
        when(publicTournamentRepository.findPublicTournaments(null, 100, 300))
                .thenReturn(List.of());
        when(publicTournamentRepository.countPublicTournaments(null)).thenReturn(0L);

        var response = service.listPublicTournaments(null, 3, 150);

        assertThat(response.page().page()).isEqualTo(3);
        assertThat(response.page().size()).isEqualTo(100);
        verify(publicTournamentRepository).findPublicTournaments(null, 100, 300);
    }

    @Test
    void publicTournamentListUsesFirstPageAndMinimumSizeForNegativeInputs() {
        when(publicTournamentRepository.findPublicTournaments("final", 1, 0))
                .thenReturn(List.of());
        when(publicTournamentRepository.countPublicTournaments("final")).thenReturn(0L);

        var response = service.listPublicTournaments("final", -1, 0);

        assertThat(response.page().page()).isZero();
        assertThat(response.page().size()).isOne();
        verify(publicTournamentRepository).findPublicTournaments("final", 1, 0);
    }

    @Test
    void bracketUsesDefaultStageWhenRequestedStageIsBlank() {
        when(tournamentRepository.findPublicById(TOURNAMENT_ID)).thenReturn(Optional.of(mock(Tournament.class)));
        when(publicTournamentRepository.findMatches(TOURNAMENT_ID, "Playoffs"))
                .thenReturn(List.of());

        var bracket = service.getBracket(TOURNAMENT_ID, "  ");

        assertThat(bracket.stageName()).isEqualTo("Playoffs");
        verify(publicTournamentRepository).findMatches(TOURNAMENT_ID, "Playoffs");
    }

    @Test
    void bracketSortsRoundsAndMatchesByPositionThenId() {
        when(tournamentRepository.findPublicById(TOURNAMENT_ID)).thenReturn(Optional.of(mock(Tournament.class)));
        when(publicTournamentRepository.findMatches(TOURNAMENT_ID, "Finals"))
                .thenReturn(List.of(
                        match(SECOND_MATCH_ID, 2, 1),
                        match(FOURTH_MATCH_ID, 1, null),
                        match(THIRD_MATCH_ID, 1, 1),
                        match(FIRST_MATCH_ID, 1, 1)));

        var bracket = service.getBracket(TOURNAMENT_ID, " Finals ");

        assertThat(bracket.stageName()).isEqualTo("Finals");
        assertThat(bracket.rounds()).extracting(round -> round.roundNumber()).containsExactly(1, 2);
        assertThat(bracket.rounds().getFirst().matches())
                .extracting(match -> match.matchId())
                .containsExactly(FIRST_MATCH_ID, THIRD_MATCH_ID, FOURTH_MATCH_ID);
        assertThat(bracket.rounds().getLast().matches())
                .extracting(match -> match.matchId())
                .containsExactly(SECOND_MATCH_ID);
        verify(publicTournamentRepository).findMatches(TOURNAMENT_ID, "Finals");
    }

    @Test
    void inaccessibleTournamentIsReportedAsNotFound() {
        when(tournamentRepository.findPublicById(TOURNAMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listTeams(TOURNAMENT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(TOURNAMENT_ID.toString());
        verify(publicTournamentRepository, org.mockito.Mockito.never()).findApprovedTeams(TOURNAMENT_ID);
    }

    private static PublicTournamentMatch match(UUID id, int round, Integer position) {
        return new PublicTournamentMatch(
                id,
                TOURNAMENT_ID,
                null,
                null,
                round,
                position,
                "Playoffs",
                "Round " + round,
                MatchStatus.SCHEDULED,
                1,
                null,
                null,
                0,
                0,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of());
    }
}
