package si.um.feri.dotaops.backend.tournament.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import si.um.feri.dotaops.backend.common.api.ApiResponse;
import si.um.feri.dotaops.backend.tournament.dto.CreateTournamentRegistrationRequest;
import si.um.feri.dotaops.backend.tournament.dto.ReviewTournamentRegistrationRequest;
import si.um.feri.dotaops.backend.tournament.dto.TournamentRegistrationResponse;
import si.um.feri.dotaops.backend.tournament.service.TournamentRegistrationService;

@RestController
@RequestMapping("/api")
public class TournamentRegistrationController {

    private final TournamentRegistrationService registrationService;

    public TournamentRegistrationController(TournamentRegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/tournaments/{tournamentId}/registrations")
    ResponseEntity<ApiResponse<TournamentRegistrationResponse>> registerTeam(
            @PathVariable UUID tournamentId,
            @Valid @RequestBody CreateTournamentRegistrationRequest request
    ) {
        TournamentRegistrationResponse response = registrationService.registerTeam(tournamentId, request);

        return ResponseEntity
                .created(URI.create("/api/tournaments/" + tournamentId + "/registrations/" + response.id()))
                .body(ApiResponse.of(response));
    }

    @PostMapping("/tournaments/{tournamentId}/registrations/{registrationId}/check-in")
    ApiResponse<TournamentRegistrationResponse> checkInRegistration(
            @PathVariable UUID tournamentId,
            @PathVariable UUID registrationId
    ) {
        return ApiResponse.of(registrationService.checkInRegistration(tournamentId, registrationId));
    }

    @GetMapping("/teams/{teamId}/tournament-registrations")
    ApiResponse<List<TournamentRegistrationResponse>> listTeamRegistrations(@PathVariable UUID teamId) {
        return ApiResponse.of(registrationService.listTeamRegistrations(teamId));
    }

    @GetMapping("/organizer/tournaments/{tournamentId}/registrations")
    ApiResponse<List<TournamentRegistrationResponse>> listOrganizerRegistrations(
            @PathVariable UUID tournamentId,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.of(registrationService.listOrganizerRegistrations(tournamentId, status));
    }

    @PostMapping("/organizer/tournaments/{tournamentId}/registrations/{registrationId}/approve")
    ApiResponse<TournamentRegistrationResponse> approveRegistration(
            @PathVariable UUID tournamentId,
            @PathVariable UUID registrationId,
            @Valid @RequestBody(required = false) ReviewTournamentRegistrationRequest request
    ) {
        return ApiResponse.of(registrationService.approveRegistration(tournamentId, registrationId, request));
    }

    @PostMapping("/organizer/tournaments/{tournamentId}/registrations/{registrationId}/reject")
    ApiResponse<TournamentRegistrationResponse> rejectRegistration(
            @PathVariable UUID tournamentId,
            @PathVariable UUID registrationId
    ) {
        return ApiResponse.of(registrationService.rejectRegistration(tournamentId, registrationId));
    }

    @PostMapping("/organizer/tournaments/{tournamentId}/registrations/{registrationId}/waitlist")
    ApiResponse<TournamentRegistrationResponse> waitlistRegistration(
            @PathVariable UUID tournamentId,
            @PathVariable UUID registrationId
    ) {
        return ApiResponse.of(registrationService.waitlistRegistration(tournamentId, registrationId));
    }
}
