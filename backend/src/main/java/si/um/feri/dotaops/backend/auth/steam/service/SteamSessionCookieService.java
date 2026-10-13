package si.um.feri.dotaops.backend.auth.steam.service;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import si.um.feri.dotaops.backend.config.properties.SteamSessionProperties;

@Service
public class SteamSessionCookieService {

    private final SteamSessionProperties properties;

    public SteamSessionCookieService(SteamSessionProperties properties) {
        this.properties = properties;
    }

    public String cookieName() {
        return properties.cookieName();
    }

    public ResponseCookie createSessionCookie(String token) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie
                .from(properties.cookieName(), token)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .path(properties.cookiePath())
                .sameSite(properties.cookieSameSite())
                .maxAge(properties.ttl());

        if (StringUtils.hasText(properties.cookieDomain())) {
            builder.domain(properties.cookieDomain().trim());
        }

        return builder.build();
    }

    public ResponseCookie clearSessionCookie() {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie
                .from(properties.cookieName(), "")
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .path(properties.cookiePath())
                .sameSite(properties.cookieSameSite())
                .maxAge(0);

        if (StringUtils.hasText(properties.cookieDomain())) {
            builder.domain(properties.cookieDomain().trim());
        }

        return builder.build();
    }
}
