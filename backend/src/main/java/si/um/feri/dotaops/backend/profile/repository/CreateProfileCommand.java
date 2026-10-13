package si.um.feri.dotaops.backend.profile.repository;

import java.util.UUID;

import si.um.feri.dotaops.backend.auth.domain.ProfileRole;

public record CreateProfileCommand(
        UUID authUserId,
        String nickname,
        String displayName,
        String avatarUrl,
        String bio,
        String countryCode,
        ProfileRole role
) {
}
