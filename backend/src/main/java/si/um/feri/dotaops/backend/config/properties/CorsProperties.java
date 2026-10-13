package si.um.feri.dotaops.backend.config.properties;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dotaops.cors")
public record CorsProperties(
        List<String> allowedOriginPatterns
) {

    public CorsProperties {
        if (allowedOriginPatterns == null || allowedOriginPatterns.isEmpty()) {
            allowedOriginPatterns = defaultAllowedOriginPatterns();
        } else {
            allowedOriginPatterns = allowedOriginPatterns.stream()
                    .map(String::trim)
                    .filter(pattern -> !pattern.isBlank())
                    .toList();
            if (allowedOriginPatterns.isEmpty()) {
                allowedOriginPatterns = defaultAllowedOriginPatterns();
            }
        }
    }

    private static List<String> defaultAllowedOriginPatterns() {
        return List.of(
                "http://localhost:3000",
                "http://127.0.0.1:3000");
    }
}
