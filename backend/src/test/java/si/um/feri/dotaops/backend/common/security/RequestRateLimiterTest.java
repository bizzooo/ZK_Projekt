package si.um.feri.dotaops.backend.common.security;

import java.time.Clock;
import java.time.Duration;
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

    @Test
    void matchImportCountersAreIsolatedByUserAndIpAndScope() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-12T00:00:00Z"));
        RequestRateLimiter limiter = new RequestRateLimiter(clock);
        UUID otherProfile = UUID.fromString("22222222-2222-4222-8222-222222222222");

        for (int i = 0; i < 10; i++) {
            limiter.checkMatchImport(PROFILE_ID, "198.51.100.41");
        }
        // Hitting the user limit does not consume another user's allowance.
        limiter.checkMatchImport(otherProfile, "198.51.100.42");
        for (int i = 0; i < 29; i++) {
            limiter.checkMatchImport(UUID.nameUUIDFromBytes(("profile-" + i).getBytes()), "198.51.100.42");
        }
        // The IP and user limits are separate counters, and Steam has its own scope.
        for (int i = 0; i < 20; i++) {
            limiter.checkSteamLogin("198.51.100.41");
        }

        assertThatThrownBy(() -> limiter.checkMatchImport(PROFILE_ID, "198.51.100.99"))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessage("Too many match import requests for this user. Try again later.");
        assertThatThrownBy(() -> limiter.checkMatchImport(otherProfile, "198.51.100.42"))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessage("Too many match import requests from this IP address. Try again later.");
        // A rejected user request did not poison a different user on a fresh IP.
        limiter.checkMatchImport(UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"), "198.51.100.99");
    }

    @Test
    void matchImportCountersResetAtTheNextOneMinuteWindow() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-12T00:00:30Z"));
        RequestRateLimiter limiter = new RequestRateLimiter(clock);

        for (int i = 0; i < 10; i++) {
            limiter.checkMatchImport(PROFILE_ID, "198.51.100.50");
        }
        assertThatThrownBy(() -> limiter.checkMatchImport(PROFILE_ID, "198.51.100.50"))
                .isInstanceOf(RateLimitExceededException.class);

        clock.advance(Duration.ofSeconds(30));
        limiter.checkMatchImport(PROFILE_ID, "198.51.100.50");
    }

    @Test
    void steamLoginCounterResetsAtTheNextFiveMinuteWindow() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-12T00:04:59Z"));
        RequestRateLimiter limiter = new RequestRateLimiter(clock);

        for (int i = 0; i < 20; i++) {
            limiter.checkSteamLogin("198.51.100.60");
        }
        assertThatThrownBy(() -> limiter.checkSteamLogin("198.51.100.60"))
                .isInstanceOf(RateLimitExceededException.class);

        clock.advance(Duration.ofSeconds(1));
        limiter.checkSteamLogin("198.51.100.60");
    }

    @Test
    void normalizesClientIpBeforeApplyingRateLimit() {
        for (int i = 0; i < 20; i++) {
            rateLimiter.checkSteamLogin("  198.51.100.70  ");
        }

        assertThatThrownBy(() -> rateLimiter.checkSteamLogin("198.51.100.70"))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessage("Too many Steam login attempts from this IP address. Try again later.");
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
