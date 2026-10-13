package si.um.feri.dotaops.backend.config.properties;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CorsPropertiesTest {

    @Test
    void defaultsStayLimitedToLocalDevelopmentOrigins() {
        CorsProperties properties = new CorsProperties(null);

        assertThat(properties.allowedOriginPatterns())
                .containsExactly("http://localhost:3000", "http://127.0.0.1:3000");
    }

    @Test
    void configuredOriginsAreTrimmedAndBlankEntriesAreIgnored() {
        CorsProperties properties = new CorsProperties(List.of(
                " https://app.example.test ",
                " ",
                "http://localhost:5173"));

        assertThat(properties.allowedOriginPatterns())
                .containsExactly("https://app.example.test", "http://localhost:5173");
    }
}
