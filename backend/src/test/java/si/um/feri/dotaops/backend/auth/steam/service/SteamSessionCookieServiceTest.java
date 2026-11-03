package si.um.feri.dotaops.backend.auth.steam.service;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import si.um.feri.dotaops.backend.config.properties.SteamSessionProperties;

import static org.assertj.core.api.Assertions.assertThat;

class SteamSessionCookieServiceTest {

    @Test
    void createsSessionCookieWithConfiguredAttributes() {
        SteamSessionCookieService service = service("  .example.test  ", true);

        ResponseCookie cookie = service.createSessionCookie("signed-token");

        assertThat(cookie.getName()).isEqualTo("session");
        assertThat(cookie.getValue()).isEqualTo("signed-token");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/auth");
        assertThat(cookie.getDomain()).isEqualTo(".example.test");
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void returnsConfiguredCookieName() {
        assertThat(service(null, false).cookieName()).isEqualTo("session");
    }

    @Test
    void omitsMissingOrBlankCookieDomain() {
        ResponseCookie blankDomainCookie = service("  ", false).createSessionCookie("token");
        ResponseCookie missingDomainCookie = service(null, false).createSessionCookie("token");

        assertThat(blankDomainCookie.getDomain()).isNull();
        assertThat(missingDomainCookie.getDomain()).isNull();
    }

    @Test
    void clearsSessionCookieUsingSameScopeAndZeroLifetime() {
        ResponseCookie cookie = service("example.test", true).clearSessionCookie();

        assertThat(cookie.getName()).isEqualTo("session");
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/auth");
        assertThat(cookie.getDomain()).isEqualTo("example.test");
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
        assertThat(cookie.getMaxAge()).isZero();
    }

    private static SteamSessionCookieService service(String domain, boolean secure) {
        return new SteamSessionCookieService(new SteamSessionProperties(
                "secret",
                "issuer",
                "audience",
                Duration.ofMinutes(30),
                "session",
                "/auth",
                domain,
                secure,
                "Strict"));
    }
}
