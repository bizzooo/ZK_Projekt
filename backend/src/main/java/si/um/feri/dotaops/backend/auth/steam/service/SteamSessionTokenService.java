package si.um.feri.dotaops.backend.auth.steam.service;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import si.um.feri.dotaops.backend.auth.steam.domain.SteamAuthResult;
import si.um.feri.dotaops.backend.auth.steam.domain.SteamSessionClaims;
import si.um.feri.dotaops.backend.config.properties.SteamSessionProperties;

@Service
public class SteamSessionTokenService {

    private static final String TOKEN_TYPE = "steam_player_session";

    private final SteamSessionProperties properties;
    private final Clock clock;

    @Autowired
    public SteamSessionTokenService(SteamSessionProperties properties) {
        this(properties, Clock.systemUTC());
    }

    SteamSessionTokenService(SteamSessionProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public String createToken(SteamAuthResult result) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.ttl());

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(properties.issuer())
                .audience(properties.audience())
                .subject(result.profileId().toString())
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .claim("typ", TOKEN_TYPE)
                .claim("steam_id", result.steamId())
                .build();

        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(secretKey()));
            return jwt.serialize();
        } catch (JOSEException exception) {
            throw new IllegalStateException("Could not sign Steam session token.", exception);
        }
    }

    public SteamSessionClaims verify(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!jwt.verify(new MACVerifier(secretKey()))) {
                throw new BadCredentialsException("Steam session signature is invalid.");
            }

            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            validateClaims(claims);
            String steamId = claims.getStringClaim("steam_id");

            return new SteamSessionClaims(
                    UUID.fromString(claims.getSubject()),
                    steamId,
                    claims.getIssueTime().toInstant(),
                    claims.getExpirationTime().toInstant());
        } catch (BadCredentialsException exception) {
            throw exception;
        } catch (ParseException | JOSEException | IllegalArgumentException exception) {
            throw new BadCredentialsException("Steam session token is invalid.", exception);
        }
    }

    private void validateClaims(JWTClaimsSet claims) throws ParseException {
        if (!TOKEN_TYPE.equals(claims.getStringClaim("typ"))) {
            throw new BadCredentialsException("Steam session token type is invalid.");
        }

        if (!properties.issuer().equals(claims.getIssuer())) {
            throw new BadCredentialsException("Steam session issuer is invalid.");
        }

        List<String> audience = claims.getAudience();
        if (audience == null || !audience.contains(properties.audience())) {
            throw new BadCredentialsException("Steam session audience is invalid.");
        }

        if (!StringUtils.hasText(claims.getSubject())) {
            throw new BadCredentialsException("Steam session subject is missing.");
        }

        Instant now = clock.instant();
        Date expiresAt = claims.getExpirationTime();
        if (expiresAt == null || !expiresAt.toInstant().isAfter(now)) {
            throw new BadCredentialsException("Steam session token has expired.");
        }

        if (!StringUtils.hasText(claims.getStringClaim("steam_id"))) {
            throw new BadCredentialsException("Steam session SteamID is missing.");
        }
    }

    private SecretKeySpec secretKey() {
        String secret = properties.jwtSecret();
        if (!StringUtils.hasText(secret) || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new BadCredentialsException("Steam session JWT secret is not configured.");
        }

        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
