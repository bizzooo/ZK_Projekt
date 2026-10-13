package si.um.feri.dotaops.backend.auth.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import si.um.feri.dotaops.backend.auth.domain.AuthenticatedProfile;
import si.um.feri.dotaops.backend.auth.domain.ProfileRole;

@Repository
public class AuthenticatedProfileRepository {

    private final JdbcTemplate jdbcTemplate;

    public AuthenticatedProfileRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<AuthenticatedProfile> findByAuthUserId(UUID authUserId) {
        return jdbcTemplate.query(
                        """
                        select id, auth_user_id, nickname, role::text as role
                        from public.profiles
                        where auth_user_id = ?
                        limit 1
                        """,
                        this::mapProfile,
                        authUserId)
                .stream()
                .findFirst();
    }

    public Optional<AuthenticatedProfile> findByProfileId(UUID profileId) {
        return jdbcTemplate.query(
                        """
                        select id, auth_user_id, nickname, role::text as role
                        from public.profiles
                        where id = ?
                        limit 1
                        """,
                        this::mapProfile,
                        profileId)
                .stream()
                .findFirst();
    }

    private AuthenticatedProfile mapProfile(ResultSet resultSet, int rowNumber) throws SQLException {
        return new AuthenticatedProfile(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("auth_user_id", UUID.class),
                resultSet.getString("nickname"),
                ProfileRole.fromDatabaseValue(resultSet.getString("role")));
    }
}
