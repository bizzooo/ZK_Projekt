package si.um.feri.dotaops.backend.profile.web;

import java.time.OffsetDateTime;
import java.util.UUID;

import si.um.feri.dotaops.backend.profile.domain.Profile;

public record ProfileResponse(
        UUID id,
        String nickname,
        String displayName,
        String steamId,
        String steamId64,
        Long opendotaAccountId,
        String role,
        String avatarUrl,
        String bio,
        String countryCode,
        OffsetDateTime steamProfileSyncedAt,
        OffsetDateTime opendotaProfileSyncedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static ProfileResponse from(Profile profile) {
        return new ProfileResponse(
                profile.id(),
                profile.nickname(),
                profile.displayName(),
                profile.steamId(),
                profile.steamId(),
                profile.opendotaAccountId(),
                profile.role().databaseValue(),
                profile.avatarUrl(),
                profile.bio(),
                profile.countryCode(),
                profile.steamProfileSyncedAt(),
                profile.opendotaProfileSyncedAt(),
                profile.createdAt(),
                profile.updatedAt());
    }
}
