package si.um.feri.dotaops.backend.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dotaops.opendota.api")
public record OpenDotaProperties(
        String baseUrl,
        String apiKey
) {

    public OpenDotaProperties {
        baseUrl = normalize(baseUrl, "https://api.opendota.com/api");
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value.trim();
    }
}
