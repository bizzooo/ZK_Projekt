package si.um.feri.dotaops.backend.tournament.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import si.um.feri.dotaops.backend.common.api.ApiResponse;
import si.um.feri.dotaops.backend.tournament.dto.AddTeamToGroupRequest;
import si.um.feri.dotaops.backend.tournament.dto.CreateTournamentGroupRequest;
import si.um.feri.dotaops.backend.tournament.dto.GroupStandingResponse;
import si.um.feri.dotaops.backend.tournament.dto.TournamentGroupResponse;
import si.um.feri.dotaops.backend.tournament.dto.TournamentGroupTeamResponse;
import si.um.feri.dotaops.backend.tournament.service.TournamentGroupService;

@RestController
@RequestMapping("/api")
public class TournamentGroupController {

    private final TournamentGroupService groupService;

    public TournamentGroupController(TournamentGroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping("/tournaments/{tournamentId}/groups")
    ApiResponse<List<TournamentGroupResponse>> listPublicGroups(@PathVariable UUID tournamentId) {
        return ApiResponse.of(groupService.listPublicGroups(tournamentId));
    }

    @GetMapping("/organizer/tournaments/{tournamentId}/groups")
    ApiResponse<List<TournamentGroupResponse>> listOrganizerGroups(@PathVariable UUID tournamentId) {
        return ApiResponse.of(groupService.listOrganizerGroups(tournamentId));
    }

    @GetMapping("/organizer/tournaments/{tournamentId}/standings")
    ApiResponse<List<GroupStandingResponse>> listOrganizerStandings(@PathVariable UUID tournamentId) {
        return ApiResponse.of(groupService.listOrganizerStandings(tournamentId));
    }

    @PostMapping({
            "/tournaments/{tournamentId}/groups",
            "/organizer/tournaments/{tournamentId}/groups"
    })
    ResponseEntity<ApiResponse<TournamentGroupResponse>> createGroup(
            @PathVariable UUID tournamentId,
            @Valid @RequestBody CreateTournamentGroupRequest request
    ) {
        TournamentGroupResponse response = groupService.createGroup(tournamentId, request);

        return ResponseEntity
                .created(URI.create("/api/tournaments/" + tournamentId + "/groups/" + response.id()))
                .body(ApiResponse.of(response));
    }

    @GetMapping("/tournament-groups/{groupId}/teams")
    ApiResponse<List<TournamentGroupTeamResponse>> listPublicGroupTeams(@PathVariable UUID groupId) {
        return ApiResponse.of(groupService.listPublicGroupTeams(groupId));
    }

    @GetMapping("/organizer/tournament-groups/{groupId}/teams")
    ApiResponse<List<TournamentGroupTeamResponse>> listOrganizerGroupTeams(@PathVariable UUID groupId) {
        return ApiResponse.of(groupService.listOrganizerGroupTeams(groupId));
    }

    @PostMapping({
            "/tournament-groups/{groupId}/teams",
            "/organizer/tournament-groups/{groupId}/teams"
    })
    ResponseEntity<ApiResponse<TournamentGroupTeamResponse>> addTeam(
            @PathVariable UUID groupId,
            @Valid @RequestBody AddTeamToGroupRequest request
    ) {
        TournamentGroupTeamResponse response = groupService.addTeam(groupId, request);

        return ResponseEntity
                .created(URI.create("/api/tournament-groups/" + groupId + "/teams/" + response.teamId()))
                .body(ApiResponse.of(response));
    }

    @DeleteMapping({
            "/tournament-groups/{groupId}/teams/{teamId}",
            "/organizer/tournament-groups/{groupId}/teams/{teamId}"
    })
    ResponseEntity<Void> removeTeam(
            @PathVariable UUID groupId,
            @PathVariable UUID teamId
    ) {
        groupService.removeTeam(groupId, teamId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping({
            "/public/tournament-groups/{groupId}/standings",
            "/tournament-groups/{groupId}/standings"
    })
    ApiResponse<List<GroupStandingResponse>> getPublicStandings(@PathVariable UUID groupId) {
        return ApiResponse.of(groupService.getPublicStandings(groupId));
    }
}
