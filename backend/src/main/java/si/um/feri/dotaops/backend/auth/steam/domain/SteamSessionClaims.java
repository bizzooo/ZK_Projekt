package si.um.feri.dotaops.backend.auth.steam.domain;

import java.time.Instant;
import java.util.UUID;

public record SteamSessionClaims(
        UUID profileId,
        String steamId,
        Instant issuedAt,
        Instant expiresAt
) {
}
