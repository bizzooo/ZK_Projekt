package si.um.feri.dotaops.backend.auth.domain;

import java.util.UUID;

public record AuthenticatedProfile(
        UUID profileId,
        UUID authUserId,
        String nickname,
        ProfileRole role
) {
}
