package si.um.feri.dotaops.backend.common.security;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import si.um.feri.dotaops.backend.common.error.RateLimitExceededException;

@Component
public class RequestRateLimiter {

    private static final int MATCH_IMPORT_USER_LIMIT = 10;
    private static final int MATCH_IMPORT_IP_LIMIT = 30;
    private static final Duration MATCH_IMPORT_WINDOW = Duration.ofMinutes(1);
    private static final int STEAM_LOGIN_IP_LIMIT = 20;
    private static final Duration STEAM_LOGIN_WINDOW = Duration.ofMinutes(5);
    private static final int MAX_COUNTERS_BEFORE_CLEANUP = 10_000;
    private static final Duration COUNTER_RETENTION = Duration.ofMinutes(10);

    private final Clock clock;
    private final ConcurrentMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public RequestRateLimiter() {
        this(Clock.systemUTC());
    }

    RequestRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public void checkMatchImport(UUID profileId, String clientIp) {
        check(
                "match-import:ip",
                clientIp,
                MATCH_IMPORT_IP_LIMIT,
                MATCH_IMPORT_WINDOW,
                "Too many match import requests from this IP address. Try again later.");
        check(
                "match-import:user",
                profileId.toString(),
                MATCH_IMPORT_USER_LIMIT,
                MATCH_IMPORT_WINDOW,
                "Too many match import requests for this user. Try again later.");
    }

    public void checkSteamLogin(String clientIp) {
        check(
                "steam-login:ip",
                clientIp,
                STEAM_LOGIN_IP_LIMIT,
                STEAM_LOGIN_WINDOW,
                "Too many Steam login attempts from this IP address. Try again later.");
    }

    private void check(String scope, String subject, int maxRequests, Duration window, String message) {
        long now = clock.millis();
        long windowMillis = window.toMillis();
        long windowStart = Math.floorDiv(now, windowMillis) * windowMillis;
        String key = scope + ":" + normalizeSubject(subject);

        WindowCounter counter = counters.compute(key, (ignored, existing) -> {
            if (existing == null || existing.windowStartMillis() != windowStart) {
                return new WindowCounter(windowStart, 1);
            }

            return new WindowCounter(existing.windowStartMillis(), existing.count() + 1);
        });

        if (counters.size() > MAX_COUNTERS_BEFORE_CLEANUP) {
            cleanup(now);
        }

        if (counter.count() > maxRequests) {
            throw new RateLimitExceededException(message);
        }
    }

    private void cleanup(long now) {
        long oldestWindowToKeep = now - COUNTER_RETENTION.toMillis();
        counters.entrySet().removeIf(entry -> entry.getValue().windowStartMillis() < oldestWindowToKeep);
    }

    private String normalizeSubject(String subject) {
        if (!StringUtils.hasText(subject)) {
            return "unknown";
        }

        return subject.trim().toLowerCase(Locale.ROOT);
    }

    private record WindowCounter(long windowStartMillis, int count) {
    }
}
