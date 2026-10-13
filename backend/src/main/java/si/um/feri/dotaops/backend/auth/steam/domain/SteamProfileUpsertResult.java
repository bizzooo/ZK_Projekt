package si.um.feri.dotaops.backend.auth.steam.domain;

import java.util.UUID;

public record SteamProfileUpsertResult(
        UUID profileId,
        UUID externalAccountId,
        boolean newProfile,
        boolean newExternalAccount
) {
}
