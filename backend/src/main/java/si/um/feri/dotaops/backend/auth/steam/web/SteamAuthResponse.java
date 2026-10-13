package si.um.feri.dotaops.backend.auth.steam.web;

import java.util.UUID;

import si.um.feri.dotaops.backend.auth.steam.domain.SteamAuthResult;

public record SteamAuthResponse(
        String status,
        String steamId,
        UUID profileId,
        UUID externalAccountId,
        boolean newProfile,
        boolean newExternalAccount,
        String personaName,
        String avatarUrl,
        String profileUrl
) {

    public static SteamAuthResponse from(SteamAuthResult result) {
        return new SteamAuthResponse(
                "authenticated",
                result.steamId(),
                result.profileId(),
                result.externalAccountId(),
                result.newProfile(),
                result.newExternalAccount(),
                result.personaName(),
                result.avatarUrl(),
                result.profileUrl());
    }
}
