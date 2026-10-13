package si.um.feri.dotaops.backend.config.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dotaops.supabase.auth")
public record SupabaseAuthProperties(
        String jwtSecret,
        String issuer,
        String audience,
        Duration clockSkew,
        String supabaseUrl,
        String jwksUri
) {

    public SupabaseAuthProperties {
        jwtSecret = normalizeBlank(jwtSecret);
        issuer = normalizeUrl(issuer);
        audience = audience == null || audience.isBlank() ? "authenticated" : audience;
        clockSkew = clockSkew == null ? Duration.ofSeconds(30) : clockSkew;
        supabaseUrl = normalizeUrl(supabaseUrl);
        jwksUri = normalizeBlank(jwksUri);
    }

    private static String normalizeUrl(String value) {
        String normalized = normalizeBlank(value);
        if (normalized == null) {
            return null;
        }

        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String normalizeBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
