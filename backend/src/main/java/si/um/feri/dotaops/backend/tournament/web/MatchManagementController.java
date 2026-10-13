package si.um.feri.dotaops.backend.tournament.web;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import si.um.feri.dotaops.backend.common.api.ApiResponse;
import si.um.feri.dotaops.backend.tournament.dto.CancelMatchRequest;
import si.um.feri.dotaops.backend.tournament.dto.MatchResponse;
import si.um.feri.dotaops.backend.tournament.dto.ScheduleMatchRequest;
import si.um.feri.dotaops.backend.tournament.dto.SubmitMatchResultRequest;
import si.um.feri.dotaops.backend.tournament.service.MatchManagementService;

@RestController
@RequestMapping("/api")
public class MatchManagementController {

    private final MatchManagementService matchManagementService;

    public MatchManagementController(MatchManagementService matchManagementService) {
        this.matchManagementService = matchManagementService;
    }

    @GetMapping("/matches/{matchId}")
    ApiResponse<MatchResponse> getPublicMatch(@PathVariable UUID matchId) {
        return ApiResponse.of(matchManagementService.getPublicMatch(matchId));
    }

    @GetMapping("/tournaments/{tournamentId}/matches")
    ApiResponse<List<MatchResponse>> listPublicTournamentMatches(@PathVariable UUID tournamentId) {
        return ApiResponse.of(matchManagementService.listPublicTournamentMatches(tournamentId));
    }

    @GetMapping("/organizer/tournaments/{tournamentId}/matches")
    ApiResponse<List<MatchResponse>> listOrganizerTournamentMatches(@PathVariable UUID tournamentId) {
        return ApiResponse.of(matchManagementService.listOrganizerTournamentMatches(tournamentId));
    }

    @PatchMapping("/organizer/matches/{matchId}/schedule")
    ApiResponse<MatchResponse> scheduleMatch(
            @PathVariable UUID matchId,
            @Valid @RequestBody ScheduleMatchRequest request
    ) {
        return ApiResponse.of(matchManagementService.scheduleMatch(matchId, request));
    }

    @PostMapping("/organizer/matches/{matchId}/start")
    ApiResponse<MatchResponse> startMatch(@PathVariable UUID matchId) {
        return ApiResponse.of(matchManagementService.startMatch(matchId));
    }

    @PostMapping("/organizer/matches/{matchId}/cancel")
    ApiResponse<MatchResponse> cancelMatch(
            @PathVariable UUID matchId,
            @Valid @RequestBody(required = false) CancelMatchRequest request
    ) {
        return ApiResponse.of(matchManagementService.cancelMatch(matchId, request));
    }

    @PostMapping("/organizer/matches/{matchId}/finish")
    ApiResponse<MatchResponse> finishMatch(@PathVariable UUID matchId) {
        return ApiResponse.of(matchManagementService.finishMatch(matchId));
    }

    @PatchMapping("/organizer/matches/{matchId}/result")
    ApiResponse<MatchResponse> submitResult(
            @PathVariable UUID matchId,
            @Valid @RequestBody SubmitMatchResultRequest request
    ) {
        return ApiResponse.of(matchManagementService.submitResult(matchId, request));
    }
}
