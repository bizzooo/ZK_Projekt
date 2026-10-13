package si.um.feri.dotaops.backend.team.repository;

import java.util.UUID;

import si.um.feri.dotaops.backend.team.domain.TeamMemberRole;

public record CreateTeamMemberCommand(
        UUID teamId,
        UUID profileId,
        TeamMemberRole role
) {
}
