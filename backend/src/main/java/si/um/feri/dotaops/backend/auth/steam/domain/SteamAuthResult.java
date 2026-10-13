package si.um.feri.dotaops.backend.auth.steam.domain;

import java.net.URI;
import java.util.UUID;

public record SteamAuthResult(
        String steamId,
        UUID profileId,
        UUID externalAccountId,
        boolean newProfile,
        boolean newExternalAccount,
        String personaName,
        String avatarUrl,
        String profileUrl,
        URI redirectUri
) {
}
