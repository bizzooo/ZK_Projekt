package si.um.feri.dotaops.backend.tournament.web;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import si.um.feri.dotaops.backend.common.api.ApiResponse;
import si.um.feri.dotaops.backend.common.pagination.PageResponse;
import si.um.feri.dotaops.backend.tournament.dto.PublicBracketResponse;
import si.um.feri.dotaops.backend.tournament.dto.PublicGroupStandingResponse;
import si.um.feri.dotaops.backend.tournament.dto.PublicTeamResponse;
import si.um.feri.dotaops.backend.tournament.dto.PublicTournamentGroupResponse;
import si.um.feri.dotaops.backend.tournament.dto.PublicTournamentListItemResponse;
import si.um.feri.dotaops.backend.tournament.dto.PublicTournamentMatchResponse;
import si.um.feri.dotaops.backend.tournament.dto.PublicTournamentMetricsResponse;
import si.um.feri.dotaops.backend.tournament.dto.PublicTournamentOverviewResponse;
import si.um.feri.dotaops.backend.tournament.service.PublicTournamentService;

@Validated
@RestController
@RequestMapping("/api/public/tournaments")
public class PublicTournamentController {

    private final PublicTournamentService publicTournamentService;

    public PublicTournamentController(PublicTournamentService publicTournamentService) {
        this.publicTournamentService = publicTournamentService;
    }

    @GetMapping
    ApiResponse<PageResponse<PublicTournamentListItemResponse>> listPublicTournaments(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.of(publicTournamentService.listPublicTournaments(search, page, size));
    }

    @GetMapping("/{tournamentId}")
    ApiResponse<PublicTournamentOverviewResponse> getOverview(@PathVariable UUID tournamentId) {
        return ApiResponse.of(publicTournamentService.getOverview(tournamentId));
    }

    @GetMapping("/{tournamentId}/teams")
    ApiResponse<List<PublicTeamResponse>> listTeams(@PathVariable UUID tournamentId) {
        return ApiResponse.of(publicTournamentService.listTeams(tournamentId));
    }

    @GetMapping("/{tournamentId}/groups")
    ApiResponse<List<PublicTournamentGroupResponse>> listGroups(@PathVariable UUID tournamentId) {
        return ApiResponse.of(publicTournamentService.listGroups(tournamentId));
    }

    @GetMapping("/{tournamentId}/standings")
    ApiResponse<List<PublicGroupStandingResponse>> listStandings(@PathVariable UUID tournamentId) {
        return ApiResponse.of(publicTournamentService.listStandings(tournamentId));
    }

    @GetMapping("/{tournamentId}/matches")
    ApiResponse<List<PublicTournamentMatchResponse>> listMatches(@PathVariable UUID tournamentId) {
        return ApiResponse.of(publicTournamentService.listMatches(tournamentId));
    }

    @GetMapping("/{tournamentId}/bracket")
    ApiResponse<PublicBracketResponse> getBracket(
            @PathVariable UUID tournamentId,
            @RequestParam(required = false) String stageName
    ) {
        return ApiResponse.of(publicTournamentService.getBracket(tournamentId, stageName));
    }

    @GetMapping("/{tournamentId}/metrics")
    ApiResponse<PublicTournamentMetricsResponse> getMetrics(@PathVariable UUID tournamentId) {
        return ApiResponse.of(publicTournamentService.getMetrics(tournamentId));
    }
}
