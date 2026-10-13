package si.um.feri.dotaops.backend.team.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TeamMember(
        UUID id,
        UUID teamId,
        UUID profileId,
        String nickname,
        String displayName,
        String avatarUrl,
        TeamMemberRole role,
        boolean active,
        OffsetDateTime joinedAt,
        OffsetDateTime leftAt,
        OffsetDateTime updatedAt
) {
}
