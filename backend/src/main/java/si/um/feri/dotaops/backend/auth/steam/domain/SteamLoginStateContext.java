package si.um.feri.dotaops.backend.auth.steam.domain;

import java.util.UUID;

public record SteamLoginStateContext(
        UUID id,
        String returnTo,
        UUID profileId,
        UUID authUserId,
        String requestedIp,
        String userAgent
) {
}
