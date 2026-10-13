package si.um.feri.dotaops.backend.profile.repository;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import si.um.feri.dotaops.backend.auth.steam.domain.SteamPlayerSummary;
import si.um.feri.dotaops.backend.opendota.domain.OpenDotaPlayerProfile;

@Repository
public class ProfileBootstrapRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProfileBootstrapRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void markSteamProfileSynced(UUID profileId, long opendotaAccountId, SteamPlayerSummary summary) {
        jdbcTemplate.update(
                """
                update public.profiles
                set
                  opendota_account_id = ?,
                  display_name = coalesce(nullif(?, ''), display_name),
                  avatar_url = coalesce(nullif(?, ''), avatar_url),
                  steam_profile_synced_at = now(),
                  updated_at = now()
                where id = ?
                """,
                opendotaAccountId,
                summary.personaName(),
                summary.avatarUrl(),
                profileId);
    }

    public void markOpenDotaProfileSynced(UUID profileId, OpenDotaPlayerProfile playerProfile) {
        jdbcTemplate.update(
                """
                update public.profiles
                set
                  opendota_account_id = ?,
                  display_name = coalesce(nullif(display_name, ''), nullif(?, '')),
                  avatar_url = coalesce(nullif(avatar_url, ''), nullif(?, '')),
                  opendota_profile_synced_at = now(),
                  opendota_last_failure_at = null,
                  updated_at = now()
                where id = ?
                """,
                playerProfile.accountId(),
                playerProfile.personaName(),
                playerProfile.avatarUrl(),
                profileId);
    }

    public void markOpenDotaProfileSyncFailed(UUID profileId, long opendotaAccountId) {
        jdbcTemplate.update(
                """
                update public.profiles
                set
                  opendota_account_id = ?,
                  opendota_last_failure_at = now(),
                  updated_at = now()
                where id = ?
                """,
                opendotaAccountId,
                profileId);
    }
}
