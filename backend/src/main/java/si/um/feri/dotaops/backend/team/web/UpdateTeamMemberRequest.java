package si.um.feri.dotaops.backend.team.web;

import jakarta.validation.constraints.NotNull;

import si.um.feri.dotaops.backend.team.domain.TeamMemberRole;

public record UpdateTeamMemberRequest(
        @NotNull
        TeamMemberRole role
) {
}
