package si.um.feri.dotaops.backend.auth.steam.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import si.um.feri.dotaops.backend.auth.steam.domain.SteamLoginStateContext;

@Repository
public class SteamLoginStateRepository {

    private final JdbcTemplate jdbcTemplate;

    public SteamLoginStateRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UUID create(
            String stateHash,
            String returnTo,
            UUID profileId,
            UUID authUserId,
            String requestedIp,
            String userAgent,
            OffsetDateTime expiresAt
    ) {
        return jdbcTemplate.queryForObject(
                """
                select private.create_steam_login_state(
                  ?,
                  ?,
                  ?,
                  ?,
                  cast(? as inet),
                  ?,
                  ?
                )
                """,
                UUID.class,
                stateHash,
                returnTo,
                profileId,
                authUserId,
                requestedIp,
                userAgent,
                expiresAt);
    }

    public Optional<SteamLoginStateContext> consume(String stateHash) {
        return jdbcTemplate.query(
                        """
                        select
                          out_id,
                          out_return_to,
                          out_profile_id,
                          out_auth_user_id,
                          out_requested_ip::text as out_requested_ip,
                          out_user_agent
                        from private.consume_steam_login_state(?)
                        """,
                        this::mapStateContext,
                        stateHash)
                .stream()
                .findFirst();
    }

    private SteamLoginStateContext mapStateContext(ResultSet resultSet, int rowNumber) throws SQLException {
        return new SteamLoginStateContext(
                resultSet.getObject("out_id", UUID.class),
                resultSet.getString("out_return_to"),
                resultSet.getObject("out_profile_id", UUID.class),
                resultSet.getObject("out_auth_user_id", UUID.class),
                resultSet.getString("out_requested_ip"),
                resultSet.getString("out_user_agent"));
    }
}
