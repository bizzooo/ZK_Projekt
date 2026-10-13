package si.um.feri.dotaops.backend.auth.service;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import com.nimbusds.jose.jwk.RSAKey;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

import si.um.feri.dotaops.backend.auth.domain.SupabaseJwtClaims;
import si.um.feri.dotaops.backend.config.properties.SupabaseAuthProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SupabaseJwtVerifierTest {

    private static final Instant NOW = Instant.parse("2026-05-11T18:00:00Z");
    private static final UUID AUTH_USER_ID = UUID.fromString("33333333-3333-4333-8333-333333333333");

    private final SupabaseJwtVerifier verifier = new SupabaseJwtVerifier(
            new SupabaseAuthProperties(
                    SupabaseJwtTestSupport.SECRET,
                    SupabaseJwtTestSupport.ISSUER,
                    SupabaseJwtTestSupport.AUDIENCE,
                    Duration.ofSeconds(30),
                    null,
                    null),
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void verifiesSignedTokenAndExtractsCoreClaims() throws Exception {
        SupabaseJwtClaims claims = verifier.verify(SupabaseJwtTestSupport.token(AUTH_USER_ID, NOW));

        assertThat(claims.authUserId()).isEqualTo(AUTH_USER_ID);
        assertThat(claims.issuer()).isEqualTo(SupabaseJwtTestSupport.ISSUER);
        assertThat(claims.audience()).contains(SupabaseJwtTestSupport.AUDIENCE);
        assertThat(claims.email()).isEqualTo("player@example.com");
    }

    @Test
    void rejectsWrongAudience() throws Exception {
        String token = SupabaseJwtTestSupport.token(
                AUTH_USER_ID,
                NOW,
                SupabaseJwtTestSupport.ISSUER,
                "anon",
                SupabaseJwtTestSupport.SECRET);

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("audience");
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        assertThatThrownBy(() -> verifier.verify(SupabaseJwtTestSupport.expiredToken(AUTH_USER_ID, NOW)))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void verifiesRs256TokenWithDerivedSupabaseJwksEndpoint() throws Exception {
        RSAKey rsaKey = SupabaseJwtTestSupport.rsaKey("test-key-1");

        try (JwksServer jwksServer = JwksServer.start("/auth/v1/.well-known/jwks.json",
                SupabaseJwtTestSupport.jwksJson(rsaKey))) {
            String issuer = jwksServer.baseUrl() + "/auth/v1";
            SupabaseJwtVerifier rs256Verifier = new SupabaseJwtVerifier(
                    new SupabaseAuthProperties(
                            null,
                            issuer,
                            SupabaseJwtTestSupport.AUDIENCE,
                            Duration.ofSeconds(30),
                            null,
                            null),
                    Clock.fixed(NOW, ZoneOffset.UTC));

            SupabaseJwtClaims claims = rs256Verifier.verify(SupabaseJwtTestSupport.rsaToken(
                    AUTH_USER_ID,
                    NOW,
                    issuer,
                    SupabaseJwtTestSupport.AUDIENCE,
                    rsaKey));

            assertThat(claims.authUserId()).isEqualTo(AUTH_USER_ID);
            assertThat(claims.issuer()).isEqualTo(issuer);
            assertThat(claims.audience()).contains(SupabaseJwtTestSupport.AUDIENCE);
            assertThat(claims.email()).isEqualTo("player@example.com");
        }
    }

    @Test
    void rejectsAsymmetricTokenWhenJwksEndpointCannotBeResolved() throws Exception {
        RSAKey rsaKey = SupabaseJwtTestSupport.rsaKey("test-key-2");
        SupabaseJwtVerifier verifierWithoutJwks = new SupabaseJwtVerifier(
                new SupabaseAuthProperties(
                        null,
                        null,
                        SupabaseJwtTestSupport.AUDIENCE,
                        Duration.ofSeconds(30),
                        null,
                        null),
                Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> verifierWithoutJwks.verify(SupabaseJwtTestSupport.rsaToken(
                AUTH_USER_ID,
                NOW,
                SupabaseJwtTestSupport.ISSUER,
                SupabaseJwtTestSupport.AUDIENCE,
                rsaKey)))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("JWKS URI");
    }

    private record JwksServer(HttpServer server, String baseUrl) implements AutoCloseable {

        static JwksServer start(String path, String jwksJson) throws Exception {
            HttpServer server = HttpServer.create(
                    new InetSocketAddress(InetAddress.getLoopbackAddress(), 0),
                    0);
            server.createContext(path, exchange -> {
                byte[] body = jwksJson.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();

            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            return new JwksServer(server, baseUrl);
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
