package si.um.feri.dotaops.backend.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.List;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import si.um.feri.dotaops.backend.auth.domain.SupabaseJwtClaims;
import si.um.feri.dotaops.backend.config.properties.SupabaseAuthProperties;

@Component
public class SupabaseJwtVerifier {

    private final SupabaseAuthProperties properties;
    private final Clock clock;
    private final JwtDecoder hmacDecoder;
    private final JwtDecoder jwksDecoder;
    private final String expectedIssuer;

    @Autowired
    public SupabaseJwtVerifier(SupabaseAuthProperties properties) {
        this(properties, Clock.systemUTC());
    }

    SupabaseJwtVerifier(SupabaseAuthProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.expectedIssuer = resolveExpectedIssuer(properties);
        this.hmacDecoder = createHmacDecoder(properties.jwtSecret());
        this.jwksDecoder = createJwksDecoder(resolveJwksUri(properties, expectedIssuer));
    }

    public SupabaseJwtClaims verify(String token) {
        Jwt jwt = decode(token);

        validateSubject(jwt);
        validateIssuer(jwt);
        validateAudience(jwt);
        validateTimestamps(jwt);

        return new SupabaseJwtClaims(
                UUID.fromString(jwt.getSubject()),
                jwt.getClaimAsString("email"),
                jwt.getIssuer() == null ? null : jwt.getIssuer().toString(),
                jwt.getAudience(),
                jwt.getExpiresAt(),
                jwt.getIssuedAt(),
                jwt.getClaims());
    }

    private Jwt decode(String token) {
        String algorithm = tokenAlgorithm(token);
        try {
            return decoderFor(algorithm).decode(token);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BadCredentialsException("Invalid Supabase access token.", exception);
        }
    }

    private JwtDecoder decoderFor(String algorithm) {
        if (JWSAlgorithm.HS256.getName().equals(algorithm)) {
            if (hmacDecoder == null) {
                throw new BadCredentialsException("Supabase JWT secret is not configured for HS256 access tokens.");
            }
            return hmacDecoder;
        }

        if (JWSAlgorithm.RS256.getName().equals(algorithm) || JWSAlgorithm.ES256.getName().equals(algorithm)) {
            if (jwksDecoder == null) {
                throw new BadCredentialsException("Supabase JWKS URI is not configured for asymmetric access tokens.");
            }
            return jwksDecoder;
        }

        throw new BadCredentialsException("Unsupported Supabase access token algorithm.");
    }

    private String tokenAlgorithm(String token) {
        try {
            return SignedJWT.parse(token).getHeader().getAlgorithm().getName();
        } catch (ParseException | RuntimeException exception) {
            throw new BadCredentialsException("Invalid Supabase access token.", exception);
        }
    }

    private void validateSubject(Jwt jwt) {
        try {
            UUID.fromString(jwt.getSubject());
        } catch (RuntimeException exception) {
            throw new BadCredentialsException("Supabase access token subject must be a user UUID.", exception);
        }
    }

    private void validateIssuer(Jwt jwt) {
        if (!StringUtils.hasText(expectedIssuer)) {
            return;
        }

        String actualIssuer = jwt.getIssuer() == null ? null : removeTrailingSlash(jwt.getIssuer().toString());
        if (!expectedIssuer.equals(actualIssuer)) {
            throw new BadCredentialsException("Supabase access token issuer is invalid.");
        }
    }

    private void validateAudience(Jwt jwt) {
        List<String> audience = jwt.getAudience();

        if (!audience.contains(properties.audience())) {
            throw new BadCredentialsException("Supabase access token audience is invalid.");
        }
    }

    private void validateTimestamps(Jwt jwt) {
        Instant now = clock.instant();
        Instant expiresAt = jwt.getExpiresAt();

        if (expiresAt == null || now.minus(properties.clockSkew()).isAfter(expiresAt)) {
            throw new BadCredentialsException("Supabase access token has expired.");
        }

        Instant notBefore = jwt.getNotBefore();
        if (notBefore != null && now.plus(properties.clockSkew()).isBefore(notBefore)) {
            throw new BadCredentialsException("Supabase access token is not active yet.");
        }
    }

    private static JwtDecoder createHmacDecoder(String jwtSecret) {
        if (!StringUtils.hasText(jwtSecret)) {
            return null;
        }

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(new SecretKeySpec(
                        jwtSecret.getBytes(StandardCharsets.UTF_8),
                        "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(jwt -> OAuth2TokenValidatorResult.success());
        return decoder;
    }

    private static JwtDecoder createJwksDecoder(String jwksUri) {
        if (!StringUtils.hasText(jwksUri)) {
            return null;
        }

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri(jwksUri)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .jwsAlgorithm(SignatureAlgorithm.ES256)
                .build();
        decoder.setJwtValidator(jwt -> OAuth2TokenValidatorResult.success());
        return decoder;
    }

    private static String resolveExpectedIssuer(SupabaseAuthProperties properties) {
        if (StringUtils.hasText(properties.issuer())) {
            return removeTrailingSlash(properties.issuer());
        }

        if (StringUtils.hasText(properties.supabaseUrl())) {
            return removeTrailingSlash(properties.supabaseUrl()) + "/auth/v1";
        }

        return null;
    }

    private static String resolveJwksUri(SupabaseAuthProperties properties, String expectedIssuer) {
        if (StringUtils.hasText(properties.jwksUri())) {
            return properties.jwksUri();
        }

        if (StringUtils.hasText(expectedIssuer)) {
            return expectedIssuer + "/.well-known/jwks.json";
        }

        return null;
    }

    private static String removeTrailingSlash(String value) {
        String normalized = value;
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
