package si.um.feri.dotaops.backend.tournament.web;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import si.um.feri.dotaops.backend.common.api.ApiResponse;
import si.um.feri.dotaops.backend.tournament.dto.BracketResponse;
import si.um.feri.dotaops.backend.tournament.dto.GenerateBracketRequest;
import si.um.feri.dotaops.backend.tournament.service.TournamentBracketService;

@RestController
@RequestMapping("/api")
public class TournamentBracketController {

    private final TournamentBracketService bracketService;

    public TournamentBracketController(TournamentBracketService bracketService) {
        this.bracketService = bracketService;
    }

    @PostMapping("/organizer/tournaments/{tournamentId}/bracket/generate")
    ApiResponse<BracketResponse> generateBracket(
            @PathVariable UUID tournamentId,
            @Valid @RequestBody(required = false) GenerateBracketRequest request
    ) {
        return ApiResponse.of(bracketService.generateBracket(tournamentId, request));
    }

    @GetMapping("/tournaments/{tournamentId}/bracket")
    ApiResponse<BracketResponse> getPublicBracket(
            @PathVariable UUID tournamentId,
            @RequestParam(required = false) String stageName
    ) {
        return ApiResponse.of(bracketService.getPublicBracket(tournamentId, stageName));
    }

    @GetMapping("/organizer/tournaments/{tournamentId}/bracket")
    ApiResponse<BracketResponse> getOrganizerBracket(
            @PathVariable UUID tournamentId,
            @RequestParam(required = false) String stageName
    ) {
        return ApiResponse.of(bracketService.getOrganizerBracket(tournamentId, stageName));
    }
}
