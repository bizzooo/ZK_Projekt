package si.um.feri.dotaops.backend.auth.steam.service;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

import si.um.feri.dotaops.backend.auth.steam.domain.SteamAuthResult;
import si.um.feri.dotaops.backend.config.properties.SteamSessionProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SteamSessionTokenServiceTest {

    private static final String SECRET = "01234567890123456789012345678901";
    private static final UUID PROFILE_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID EXTERNAL_ACCOUNT_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final String STEAM_ID = "76561198000000001";
    private static final Instant NOW = Instant.parse("2026-05-12T00:00:00Z");

    @Test
    void createsAndVerifiesSteamSessionToken() {
        SteamSessionTokenService service = serviceAt(NOW, Duration.ofHours(2), SECRET);

        String token = service.createToken(authResult());
        var claims = service.verify(token);

        assertThat(claims.profileId()).isEqualTo(PROFILE_ID);
        assertThat(claims.steamId()).isEqualTo(STEAM_ID);
        assertThat(claims.issuedAt()).isEqualTo(NOW);
        assertThat(claims.expiresAt()).isEqualTo(NOW.plus(Duration.ofHours(2)));
    }

    @Test
    void rejectsExpiredSteamSessionToken() {
        String token = serviceAt(NOW, Duration.ofMinutes(5), SECRET).createToken(authResult());
        SteamSessionTokenService verifier = serviceAt(NOW.plus(Duration.ofMinutes(6)), Duration.ofMinutes(5), SECRET);

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Steam session token has expired.");
    }

    @Test
    void rejectsTokenSignedWithDifferentSecret() {
        String token = serviceAt(NOW, Duration.ofMinutes(5), SECRET).createToken(authResult());
        SteamSessionTokenService verifier = serviceAt(NOW, Duration.ofMinutes(5), "abcdefghijklmnopqrstuvwxyz123456");

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Steam session signature is invalid.");
    }

    @Test
    void requiresConfiguredSecret() {
        SteamSessionTokenService service = serviceAt(NOW, Duration.ofMinutes(5), "");

        assertThatThrownBy(() -> service.createToken(authResult()))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Steam session JWT secret is not configured.");
    }

    private static SteamSessionTokenService serviceAt(Instant instant, Duration ttl, String secret) {
        return new SteamSessionTokenService(
                new SteamSessionProperties(
                        secret,
                        "dotaops-backend",
                        "dotaops-steam-session",
                        ttl,
                        "dotaops_steam_session",
                        "/",
                        null,
                        false,
                        "Lax"),
                Clock.fixed(instant, ZoneOffset.UTC));
    }

    private static SteamAuthResult authResult() {
        return new SteamAuthResult(
                STEAM_ID,
                PROFILE_ID,
                EXTERNAL_ACCOUNT_ID,
                true,
                true,
                "Dota Player",
                "https://cdn.example.test/avatar.png",
                "https://steamcommunity.com/profiles/" + STEAM_ID + "/",
                URI.create("http://localhost:3000/auth/steam/callback"));
    }
}
