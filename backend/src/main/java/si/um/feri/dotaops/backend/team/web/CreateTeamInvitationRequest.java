package si.um.feri.dotaops.backend.team.web;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;

import si.um.feri.dotaops.backend.team.domain.TeamMemberRole;

public record CreateTeamInvitationRequest(
        UUID inviteeProfileId,

        @Email
        @Size(max = 254)
        String inviteeEmail,

        TeamMemberRole proposedRole,

        @Future
        OffsetDateTime expiresAt
) {
}
