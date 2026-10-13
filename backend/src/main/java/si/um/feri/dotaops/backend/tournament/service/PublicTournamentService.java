package si.um.feri.dotaops.backend.tournament.service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import si.um.feri.dotaops.backend.common.error.ResourceNotFoundException;
import si.um.feri.dotaops.backend.common.pagination.PageResponse;
import si.um.feri.dotaops.backend.tournament.domain.PublicTournamentMatch;
import si.um.feri.dotaops.backend.tournament.domain.Tournament;
import si.um.feri.dotaops.backend.tournament.dto.PublicBracketResponse;
import si.um.feri.dotaops.backend.tournament.dto.PublicGroupStandingResponse;
import si.um.feri.dotaops.backend.tournament.dto.PublicTeamResponse;
import si.um.feri.dotaops.backend.tournament.dto.PublicTournamentGroupResponse;
import si.um.feri.dotaops.backend.tournament.dto.PublicTournamentListItemResponse;
import si.um.feri.dotaops.backend.tournament.dto.PublicTournamentMatchResponse;
import si.um.feri.dotaops.backend.tournament.dto.PublicTournamentMetricsResponse;
import si.um.feri.dotaops.backend.tournament.dto.PublicTournamentOverviewResponse;
import si.um.feri.dotaops.backend.tournament.repository.PublicTournamentRepository;
import si.um.feri.dotaops.backend.tournament.repository.TournamentRepository;

@Service
public class PublicTournamentService {

    private static final String DEFAULT_STAGE_NAME = "Playoffs";

    private final TournamentRepository tournamentRepository;
    private final PublicTournamentRepository publicTournamentRepository;

    public PublicTournamentService(
            TournamentRepository tournamentRepository,
            PublicTournamentRepository publicTournamentRepository
    ) {
        this.tournamentRepository = tournamentRepository;
        this.publicTournamentRepository = publicTournamentRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<PublicTournamentListItemResponse> listPublicTournaments(String search, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        long offset = (long) safePage * safeSize;

        List<PublicTournamentListItemResponse> items = publicTournamentRepository
                .findPublicTournaments(search, safeSize, offset)
                .stream()
                .map(PublicTournamentListItemResponse::from)
                .toList();
        long total = publicTournamentRepository.countPublicTournaments(search);

        return PageResponse.from(new PageImpl<>(
                items,
                PageRequest.of(safePage, safeSize),
                total));
    }

    @Transactional(readOnly = true)
    public PublicTournamentOverviewResponse getOverview(UUID tournamentId) {
        Tournament tournament = findPublicTournament(tournamentId);
        List<PublicTeamResponse> teams = listTeamsForPublicTournament(tournament.id());
        List<PublicTournamentGroupResponse> groups = listGroupsForPublicTournament(tournament.id());
        List<PublicTournamentMatchResponse> matches = listMatchesForPublicTournament(tournament.id());
        PublicTournamentMetricsResponse metrics = metricsForPublicTournament(tournament.id());

        return PublicTournamentOverviewResponse.from(tournament, teams, groups, matches, metrics);
    }

    @Transactional(readOnly = true)
    public List<PublicTeamResponse> listTeams(UUID tournamentId) {
        ensurePublicTournamentExists(tournamentId);
        return listTeamsForPublicTournament(tournamentId);
    }

    @Transactional(readOnly = true)
    public List<PublicTournamentGroupResponse> listGroups(UUID tournamentId) {
        ensurePublicTournamentExists(tournamentId);
        return listGroupsForPublicTournament(tournamentId);
    }

    @Transactional(readOnly = true)
    public List<PublicGroupStandingResponse> listStandings(UUID tournamentId) {
        ensurePublicTournamentExists(tournamentId);

        return publicTournamentRepository.findStandings(tournamentId)
                .stream()
                .map(PublicGroupStandingResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PublicTournamentMatchResponse> listMatches(UUID tournamentId) {
        ensurePublicTournamentExists(tournamentId);
        return listMatchesForPublicTournament(tournamentId);
    }

    @Transactional(readOnly = true)
    public PublicBracketResponse getBracket(UUID tournamentId, String requestedStageName) {
        ensurePublicTournamentExists(tournamentId);
        String stageName = normalizeStageName(requestedStageName);
        List<PublicTournamentMatch> matches = publicTournamentRepository.findMatches(tournamentId, stageName)
                .stream()
                .sorted(Comparator
                        .comparing(PublicTournamentMatch::roundNumber)
                        .thenComparing(PublicTournamentMatch::bracketPosition, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(PublicTournamentMatch::id))
                .toList();

        return PublicBracketResponse.from(tournamentId, stageName, matches);
    }

    @Transactional(readOnly = true)
    public PublicTournamentMetricsResponse getMetrics(UUID tournamentId) {
        ensurePublicTournamentExists(tournamentId);
        return metricsForPublicTournament(tournamentId);
    }

    private Tournament findPublicTournament(UUID tournamentId) {
        return tournamentRepository.findPublicById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", "id", tournamentId));
    }

    private void ensurePublicTournamentExists(UUID tournamentId) {
        findPublicTournament(tournamentId);
    }

    private List<PublicTeamResponse> listTeamsForPublicTournament(UUID tournamentId) {
        return publicTournamentRepository.findApprovedTeams(tournamentId)
                .stream()
                .map(PublicTeamResponse::from)
                .toList();
    }

    private List<PublicTournamentGroupResponse> listGroupsForPublicTournament(UUID tournamentId) {
        return publicTournamentRepository.findGroups(tournamentId)
                .stream()
                .map(PublicTournamentGroupResponse::from)
                .toList();
    }

    private List<PublicTournamentMatchResponse> listMatchesForPublicTournament(UUID tournamentId) {
        return publicTournamentRepository.findMatches(tournamentId)
                .stream()
                .map(PublicTournamentMatchResponse::from)
                .toList();
    }

    private PublicTournamentMetricsResponse metricsForPublicTournament(UUID tournamentId) {
        return PublicTournamentMetricsResponse.from(publicTournamentRepository.findMetrics(tournamentId));
    }

    private String normalizeStageName(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_STAGE_NAME;
        }

        return value.trim();
    }
}
