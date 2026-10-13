package si.um.feri.dotaops.backend.config.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dotaops.steam.session")
public record SteamSessionProperties(
        String jwtSecret,
        String issuer,
        String audience,
        Duration ttl,
        String cookieName,
        String cookiePath,
        String cookieDomain,
        boolean cookieSecure,
        String cookieSameSite
) {

    public SteamSessionProperties {
        issuer = normalize(issuer, "dotaops-backend");
        audience = normalize(audience, "dotaops-steam-session");
        ttl = ttl == null ? Duration.ofDays(7) : ttl;
        cookieName = normalize(cookieName, "dotaops_steam_session");
        cookiePath = normalize(cookiePath, "/");
        cookieSameSite = normalize(cookieSameSite, "Lax");
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value.trim();
    }
}
