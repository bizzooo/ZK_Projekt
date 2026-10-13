package si.um.feri.dotaops.backend.team.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TeamInvitation(
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
}
