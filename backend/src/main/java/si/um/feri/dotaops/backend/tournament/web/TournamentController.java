package si.um.feri.dotaops.backend.tournament.web;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import si.um.feri.dotaops.backend.common.api.ApiResponse;
import si.um.feri.dotaops.backend.common.pagination.PageResponse;
import si.um.feri.dotaops.backend.tournament.dto.CreateTournamentRequest;
import si.um.feri.dotaops.backend.tournament.dto.TournamentDetailResponse;
import si.um.feri.dotaops.backend.tournament.dto.TournamentPublicResponse;
import si.um.feri.dotaops.backend.tournament.dto.TournamentResponse;
import si.um.feri.dotaops.backend.tournament.dto.UpdateTournamentRequest;
import si.um.feri.dotaops.backend.tournament.service.TournamentService;

@Validated
@RestController
@RequestMapping("/api")
public class TournamentController {

    private final TournamentService tournamentService;

    public TournamentController(TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    @GetMapping("/tournaments")
    ApiResponse<PageResponse<TournamentPublicResponse>> listPublicTournaments(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.of(tournamentService.listPublicTournaments(search, page, size));
    }

    @GetMapping("/tournaments/{slug}")
    ApiResponse<TournamentDetailResponse> getPublicTournament(@PathVariable String slug) {
        return ApiResponse.of(tournamentService.getPublicTournament(slug));
    }

    @GetMapping("/organizer/tournaments")
    ApiResponse<PageResponse<TournamentResponse>> listOrganizerTournaments(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.of(tournamentService.listOrganizerTournaments(search, page, size));
    }

    @GetMapping("/organizer/tournaments/{tournamentId}")
    ApiResponse<TournamentResponse> getOrganizerTournament(@PathVariable UUID tournamentId) {
        return ApiResponse.of(tournamentService.getOrganizerTournament(tournamentId));
    }

    @PostMapping("/organizer/tournaments")
    ResponseEntity<ApiResponse<TournamentResponse>> createTournament(
            @Valid @RequestBody CreateTournamentRequest request
    ) {
        TournamentResponse response = tournamentService.createTournament(request);

        return ResponseEntity
                .created(URI.create("/api/organizer/tournaments/" + response.id()))
                .body(ApiResponse.of(response));
    }

    @PatchMapping("/organizer/tournaments/{tournamentId}")
    ApiResponse<TournamentResponse> updateTournament(
            @PathVariable UUID tournamentId,
            @Valid @RequestBody UpdateTournamentRequest request
    ) {
        return ApiResponse.of(tournamentService.updateTournament(tournamentId, request));
    }

    @PostMapping("/organizer/tournaments/{tournamentId}/publish")
    ApiResponse<TournamentResponse> publishTournament(@PathVariable UUID tournamentId) {
        return ApiResponse.of(tournamentService.publishTournament(tournamentId));
    }

    @PostMapping("/organizer/tournaments/{tournamentId}/archive")
    ApiResponse<TournamentResponse> archiveTournament(@PathVariable UUID tournamentId) {
        return ApiResponse.of(tournamentService.archiveTournament(tournamentId));
    }
}
