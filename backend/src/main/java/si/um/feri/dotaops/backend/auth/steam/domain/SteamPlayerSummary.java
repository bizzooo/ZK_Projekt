package si.um.feri.dotaops.backend.auth.steam.domain;

public record SteamPlayerSummary(
        String steamId,
        String personaName,
        String avatarUrl,
        String profileUrl
) {
}
