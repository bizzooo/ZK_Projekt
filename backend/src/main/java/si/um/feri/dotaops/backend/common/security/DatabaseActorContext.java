package si.um.feri.dotaops.backend.common.security;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import si.um.feri.dotaops.backend.auth.domain.AuthenticatedActor;

@Component
public class DatabaseActorContext {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseActorContext(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void apply(AuthenticatedActor actor) {
        setLocal("request.jwt.claim.sub", actor.optionalAuthUserId().map(UUID::toString).orElse(""));
        setLocal("request.dotaops.auth_user_id", actor.optionalAuthUserId().map(UUID::toString).orElse(""));
        setLocal("request.dotaops.profile_id", actor.optionalProfileId().map(UUID::toString).orElse(""));
    }

    private void setLocal(String key, String value) {
        jdbcTemplate.queryForObject(
                "select set_config(?, ?, true)",
                String.class,
                key,
                value);
    }
}
