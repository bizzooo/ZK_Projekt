package si.um.feri.dotaops.backend.config.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dotaops.steam.auth")
public record SteamAuthProperties(
        String openidEndpoint,
        String realm,
        String returnUrl,
        String webApiKey,
        String playerSummariesUrl,
        String frontendRedirectUrl,
        Duration stateTtl
) {

    public SteamAuthProperties {
        openidEndpoint = normalize(openidEndpoint, "https://steamcommunity.com/openid/login");
        realm = normalize(realm, "http://localhost:8080");
        returnUrl = normalize(returnUrl, "http://localhost:8080/api/auth/steam/callback");
        playerSummariesUrl = normalize(
                playerSummariesUrl,
                "https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/");
        stateTtl = stateTtl == null ? Duration.ofMinutes(10) : stateTtl;
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value.trim();
    }
}
