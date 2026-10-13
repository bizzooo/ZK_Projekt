package si.um.feri.dotaops.backend.team.web;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import si.um.feri.dotaops.backend.team.domain.TeamMemberRole;

public record AddTeamMemberRequest(
        @NotNull
        UUID profileId,

        TeamMemberRole role
) {
}
