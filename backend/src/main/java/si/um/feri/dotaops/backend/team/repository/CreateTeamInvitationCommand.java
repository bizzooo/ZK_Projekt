package si.um.feri.dotaops.backend.team.repository;

import java.time.OffsetDateTime;
import java.util.UUID;

import si.um.feri.dotaops.backend.team.domain.TeamMemberRole;

public record CreateTeamInvitationCommand(
        UUID teamId,
        UUID inviterProfileId,
        UUID inviteeProfileId,
        String inviteeEmail,
        TeamMemberRole proposedRole,
        OffsetDateTime expiresAt
) {
}
