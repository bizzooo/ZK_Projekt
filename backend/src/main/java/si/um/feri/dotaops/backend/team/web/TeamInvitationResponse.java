package si.um.feri.dotaops.backend.team.web;

import java.time.OffsetDateTime;
import java.util.UUID;

import si.um.feri.dotaops.backend.team.domain.TeamInvitation;
import si.um.feri.dotaops.backend.team.domain.TeamInvitationStatus;
import si.um.feri.dotaops.backend.team.domain.TeamMemberRole;

public record TeamInvitationResponse(
        UUID id,
        UUID teamId,
        String teamName,
        String teamSlug,
        UUID inviterProfileId,
        String inviterNickname,
        UUID inviteeProfileId,
        String inviteeNickname,
        String inviteeEmail,
        TeamMemberRole proposedRole,
        TeamInvitationStatus status,
        OffsetDateTime expiresAt,
        OffsetDateTime acceptedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static TeamInvitationResponse from(TeamInvitation invitation) {
        return new TeamInvitationResponse(
                invitation.id(),
                invitation.teamId(),
                invitation.teamName(),
                invitation.teamSlug(),
                invitation.inviterProfileId(),
                invitation.inviterNickname(),
                invitation.inviteeProfileId(),
                invitation.inviteeNickname(),
                invitation.inviteeEmail(),
                invitation.proposedRole(),
                invitation.status(),
                invitation.expiresAt(),
                invitation.acceptedAt(),
                invitation.createdAt(),
                invitation.updatedAt());
    }
}
