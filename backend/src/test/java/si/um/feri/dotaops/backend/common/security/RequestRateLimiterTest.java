package si.um.feri.dotaops.backend.common.security;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import si.um.feri.dotaops.backend.common.error.RateLimitExceededException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestRateLimiterTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-12T00:00:00Z"), ZoneOffset.UTC);
    private static final UUID PROFILE_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");

    private final RequestRateLimiter rateLimiter = new RequestRateLimiter(CLOCK);

    @Test
    void rejectsMatchImportAfterPerUserLimit() {
        for (int i = 0; i < 10; i++) {
            rateLimiter.checkMatchImport(PROFILE_ID, "198.51.100.10");
        }

        assertThatThrownBy(() -> rateLimiter.checkMatchImport(PROFILE_ID, "198.51.100.10"))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessage("Too many match import requests for this user. Try again later.");
    }

    @Test
    void rejectsMatchImportAfterPerIpLimit() {
        for (int i = 0; i < 30; i++) {
            rateLimiter.checkMatchImport(UUID.randomUUID(), "198.51.100.20");
        }

        assertThatThrownBy(() -> rateLimiter.checkMatchImport(UUID.randomUUID(), "198.51.100.20"))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessage("Too many match import requests from this IP address. Try again later.");
    }

    @Test
    void rejectsSteamLoginAfterPerIpLimit() {
        for (int i = 0; i < 20; i++) {
            rateLimiter.checkSteamLogin("198.51.100.30");
        }

        assertThatThrownBy(() -> rateLimiter.checkSteamLogin("198.51.100.30"))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessage("Too many Steam login attempts from this IP address. Try again later.");
    }
}
