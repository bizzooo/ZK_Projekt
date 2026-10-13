package si.um.feri.dotaops.backend.auth.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SupabaseJwtClaims(
        UUID authUserId,
        String email,
        String issuer,
        List<String> audience,
        Instant expiresAt,
        Instant issuedAt,
        Map<String, Object> claims
) {

    public SupabaseJwtClaims {
        audience = audience == null ? List.of() : List.copyOf(audience);
        claims = claims == null ? Map.of() : Map.copyOf(claims);
    }
}
