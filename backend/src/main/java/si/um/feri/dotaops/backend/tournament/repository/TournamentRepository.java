package si.um.feri.dotaops.backend.tournament.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import si.um.feri.dotaops.backend.tournament.domain.Tournament;
import si.um.feri.dotaops.backend.tournament.domain.TournamentFormat;
import si.um.feri.dotaops.backend.tournament.domain.TournamentStatus;
import si.um.feri.dotaops.backend.tournament.mapper.TournamentSettingsMapper;

@Repository
public class TournamentRepository {

    private final JdbcTemplate jdbcTemplate;
    private final TournamentSettingsMapper settingsMapper;

    public TournamentRepository(
            JdbcTemplate jdbcTemplate,
            TournamentSettingsMapper settingsMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.settingsMapper = settingsMapper;
    }

    public List<Tournament> findPublicVisible(String search, int size, long offset) {
        String normalizedSearch = normalizeSearch(search);

        return jdbcTemplate.query(
                selectTournamentSql() + """
                where t.is_public = true
                  and t.status in ('registration', 'published', 'live', 'finished')
                  and (
                    cast(? as text) is null
                    or t.title ilike '%' || cast(? as text) || '%'
                    or t.slug ilike '%' || cast(? as text) || '%'
                    or t.description ilike '%' || cast(? as text) || '%'
                  )
                order by t.starts_at asc, t.created_at desc, t.id desc
                limit ? offset ?
                """,
                this::mapTournament,
                normalizedSearch,
                normalizedSearch,
                normalizedSearch,
                normalizedSearch,
                size,
                offset);
    }

    public long countPublicVisible(String search) {
        String normalizedSearch = normalizeSearch(search);
        Long count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from public.tournaments t
                where t.is_public = true
                  and t.status in ('registration', 'published', 'live', 'finished')
                  and (
                    cast(? as text) is null
                    or t.title ilike '%' || cast(? as text) || '%'
                    or t.slug ilike '%' || cast(? as text) || '%'
                    or t.description ilike '%' || cast(? as text) || '%'
                  )
                """,
                Long.class,
                normalizedSearch,
                normalizedSearch,
                normalizedSearch,
                normalizedSearch);

        return count == null ? 0 : count;
    }

    public Optional<Tournament> findPublicBySlug(String slug) {
        return jdbcTemplate.query(
                        selectTournamentSql() + """
                        where t.slug = ?
                          and t.is_public = true
                          and t.status in ('registration', 'published', 'live', 'finished')
                        limit 1
                        """,
                        this::mapTournament,
                        slug)
                .stream()
                .findFirst();
    }

    public Optional<Tournament> findPublicById(UUID tournamentId) {
        return jdbcTemplate.query(
                        selectTournamentSql() + """
                        where t.id = ?
                          and t.is_public = true
                          and t.status in ('registration', 'published', 'live', 'finished')
                        limit 1
                        """,
                        this::mapTournament,
                        tournamentId)
                .stream()
                .findFirst();
    }

    public List<Tournament> findManageable(UUID profileId, boolean admin, String search, int size, long offset) {
        String normalizedSearch = normalizeSearch(search);

        return jdbcTemplate.query(
                selectTournamentSql() + """
                where (
                    ? = true
                    or t.organizer_profile_id = ?
                    or exists (
                      select 1
                      from public.tournament_staff ts
                      where ts.tournament_id = t.id
                        and ts.profile_id = ?
                        and ts.staff_role in ('owner', 'organizer')
                    )
                  )
                  and (
                    cast(? as text) is null
                    or t.title ilike '%' || cast(? as text) || '%'
                    or t.slug ilike '%' || cast(? as text) || '%'
                    or t.description ilike '%' || cast(? as text) || '%'
                  )
                order by t.updated_at desc, t.created_at desc, t.id desc
                limit ? offset ?
                """,
                this::mapTournament,
                admin,
                profileId,
                profileId,
                normalizedSearch,
                normalizedSearch,
                normalizedSearch,
                normalizedSearch,
                size,
                offset);
    }

    public long countManageable(UUID profileId, boolean admin, String search) {
        String normalizedSearch = normalizeSearch(search);
        Long count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from public.tournaments t
                where (
                    ? = true
                    or t.organizer_profile_id = ?
                    or exists (
                      select 1
                      from public.tournament_staff ts
                      where ts.tournament_id = t.id
                        and ts.profile_id = ?
                        and ts.staff_role in ('owner', 'organizer')
                    )
                  )
                  and (
                    cast(? as text) is null
                    or t.title ilike '%' || cast(? as text) || '%'
                    or t.slug ilike '%' || cast(? as text) || '%'
                    or t.description ilike '%' || cast(? as text) || '%'
                  )
                """,
                Long.class,
                admin,
                profileId,
                profileId,
                normalizedSearch,
                normalizedSearch,
                normalizedSearch,
                normalizedSearch);

        return count == null ? 0 : count;
    }

    public Optional<Tournament> findById(UUID tournamentId) {
        return jdbcTemplate.query(
                        selectTournamentSql() + "where t.id = ? limit 1",
                        this::mapTournament,
                        tournamentId)
                .stream()
                .findFirst();
    }

    public boolean canManage(UUID tournamentId, UUID profileId, boolean admin) {
        if (admin) {
            return true;
        }

        Boolean canManage = jdbcTemplate.queryForObject(
                """
                select exists (
                  select 1
                  from public.tournaments t
                  where t.id = ?
                    and (
                      t.organizer_profile_id = ?
                      or exists (
                        select 1
                        from public.tournament_staff ts
                        where ts.tournament_id = t.id
                          and ts.profile_id = ?
                          and ts.staff_role in ('owner', 'organizer')
                      )
                    )
                )
                """,
                Boolean.class,
                tournamentId,
                profileId,
                profileId);

        return Boolean.TRUE.equals(canManage);
    }

    public Tournament create(CreateTournamentCommand command) {
        Tournament tournament = jdbcTemplate.queryForObject(
                """
                insert into public.tournaments (
                  slug,
                  title,
                  status,
                  format,
                  organizer_profile_id,
                  description,
                  rules,
                  prize_pool,
                  max_teams,
                  starts_at,
                  ends_at,
                  registration_opens_at,
                  registration_closes_at,
                  is_public,
                  created_by,
                  timezone,
                  check_in_opens_at,
                  check_in_closes_at,
                  settings
                )
                values (
                  ?,
                  ?,
                  'draft',
                  cast(? as public.dotaops_tournament_format),
                  ?,
                  ?,
                  ?,
                  ?,
                  ?,
                  ?,
                  ?,
                  ?,
                  ?,
                  false,
                  ?,
                  ?,
                  ?,
                  ?,
                  cast(? as jsonb)
                )
                """ + returningTournamentSql(),
                this::mapTournament,
                command.slug(),
                command.title(),
                command.format().databaseValue(),
                command.organizerProfileId(),
                command.description(),
                command.rules(),
                command.prizePool(),
                command.maxTeams(),
                command.startsAt(),
                command.endsAt(),
                command.registrationOpensAt(),
                command.registrationClosesAt(),
                command.createdBy(),
                command.timezone(),
                command.checkInOpensAt(),
                command.checkInClosesAt(),
                command.settingsJson());

        jdbcTemplate.update(
                """
                insert into public.tournament_staff (tournament_id, profile_id, staff_role)
                values (?, ?, 'owner')
                on conflict (tournament_id, profile_id) do nothing
                """,
                tournament.id(),
                command.organizerProfileId());

        return findById(tournament.id()).orElse(tournament);
    }

    public Optional<Tournament> update(UUID tournamentId, UpdateTournamentCommand command) {
        return jdbcTemplate.query(
                        """
                        update public.tournaments
                        set
                          title = case when ? then ? else title end,
                          slug = case when ? then ? else slug end,
                          format = case when ? then cast(? as public.dotaops_tournament_format) else format end,
                          description = case when ? then ? else description end,
                          rules = case when ? then ? else rules end,
                          prize_pool = case when ? then ? else prize_pool end,
                          max_teams = case when ? then ? else max_teams end,
                          starts_at = case when ? then ? else starts_at end,
                          ends_at = case when ? then ? else ends_at end,
                          registration_opens_at = case when ? then ? else registration_opens_at end,
                          registration_closes_at = case when ? then ? else registration_closes_at end,
                          timezone = case when ? then ? else timezone end,
                          check_in_opens_at = case when ? then ? else check_in_opens_at end,
                          check_in_closes_at = case when ? then ? else check_in_closes_at end,
                          settings = case when ? then cast(? as jsonb) else settings end,
                          updated_at = now()
                        where id = ?
                        """ + returningTournamentSql(),
                        this::mapTournament,
                        command.titlePresent(),
                        command.title(),
                        command.slugPresent(),
                        command.slug(),
                        command.formatPresent(),
                        command.format() == null ? null : command.format().databaseValue(),
                        command.descriptionPresent(),
                        command.description(),
                        command.rulesPresent(),
                        command.rules(),
                        command.prizePoolPresent(),
                        command.prizePool(),
                        command.maxTeamsPresent(),
                        command.maxTeams(),
                        command.startsAtPresent(),
                        command.startsAt(),
                        command.endsAtPresent(),
                        command.endsAt(),
                        command.registrationOpensAtPresent(),
                        command.registrationOpensAt(),
                        command.registrationClosesAtPresent(),
                        command.registrationClosesAt(),
                        command.timezonePresent(),
                        command.timezone(),
                        command.checkInOpensAtPresent(),
                        command.checkInOpensAt(),
                        command.checkInClosesAtPresent(),
                        command.checkInClosesAt(),
                        command.settingsPresent(),
                        command.settingsJson(),
                        tournamentId)
                .stream()
                .findFirst();
    }

    public Optional<Tournament> publish(UUID tournamentId) {
        return jdbcTemplate.query(
                        """
                        update public.tournaments
                        set
                          status = 'published',
                          is_public = true,
                          published_at = coalesce(published_at, now()),
                          updated_at = now()
                        where id = ?
                        """ + returningTournamentSql(),
                        this::mapTournament,
                        tournamentId)
                .stream()
                .findFirst();
    }

    public Optional<Tournament> archive(UUID tournamentId) {
        return jdbcTemplate.query(
                        """
                        update public.tournaments
                        set
                          status = 'archived',
                          is_public = false,
                          updated_at = now()
                        where id = ?
                        """ + returningTournamentSql(),
                        this::mapTournament,
                        tournamentId)
                .stream()
                .findFirst();
    }

    private String selectTournamentSql() {
        return """
                select
                  t.id,
                  t.slug,
                  t.title,
                  t.status::text as status,
                  t.format::text as format,
                  t.organizer_profile_id,
                  p.nickname as organizer_nickname,
                  t.description,
                  t.rules,
                  t.prize_pool,
                  t.max_teams,
                  t.starts_at,
                  t.ends_at,
                  t.registration_opens_at,
                  t.registration_closes_at,
                  t.is_public,
                  t.created_by,
                  t.timezone,
                  t.check_in_opens_at,
                  t.check_in_closes_at,
                  t.published_at,
                  t.settings::text as settings,
                  (
                    select count(*)
                    from public.tournament_registrations tr
                    where tr.tournament_id = t.id
                      and tr.status in ('pending', 'approved', 'waitlisted')
                  ) as registrations_count,
                  t.created_at,
                  t.updated_at
                from public.tournaments t
                left join public.profiles p on p.id = t.organizer_profile_id
                """;
    }

    private String returningTournamentSql() {
        return """
                returning
                  id,
                  slug,
                  title,
                  status::text as status,
                  format::text as format,
                  organizer_profile_id,
                  (
                    select p.nickname
                    from public.profiles p
                    where p.id = organizer_profile_id
                  ) as organizer_nickname,
                  description,
                  rules,
                  prize_pool,
                  max_teams,
                  starts_at,
                  ends_at,
                  registration_opens_at,
                  registration_closes_at,
                  is_public,
                  created_by,
                  timezone,
                  check_in_opens_at,
                  check_in_closes_at,
                  published_at,
                  settings::text as settings,
                  (
                    select count(*)
                    from public.tournament_registrations tr
                    where tr.tournament_id = public.tournaments.id
                      and tr.status in ('pending', 'approved', 'waitlisted')
                  ) as registrations_count,
                  created_at,
                  updated_at
                """;
    }

    private Tournament mapTournament(ResultSet resultSet, int rowNumber) throws SQLException {
        TournamentFormat format = TournamentFormat.fromDatabaseValue(resultSet.getString("format"));
        int maxTeams = resultSet.getInt("max_teams");

        return new Tournament(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("slug"),
                resultSet.getString("title"),
                TournamentStatus.fromDatabaseValue(resultSet.getString("status")),
                format,
                resultSet.getObject("organizer_profile_id", UUID.class),
                resultSet.getString("organizer_nickname"),
                resultSet.getString("description"),
                resultSet.getString("rules"),
                resultSet.getString("prize_pool"),
                maxTeams,
                resultSet.getObject("starts_at", OffsetDateTime.class),
                resultSet.getObject("ends_at", OffsetDateTime.class),
                resultSet.getObject("registration_opens_at", OffsetDateTime.class),
                resultSet.getObject("registration_closes_at", OffsetDateTime.class),
                resultSet.getBoolean("is_public"),
                resultSet.getObject("created_by", UUID.class),
                resultSet.getString("timezone"),
                resultSet.getObject("check_in_opens_at", OffsetDateTime.class),
                resultSet.getObject("check_in_closes_at", OffsetDateTime.class),
                resultSet.getObject("published_at", OffsetDateTime.class),
                settingsMapper.fromJson(resultSet.getString("settings"), format, maxTeams),
                resultSet.getLong("registrations_count"),
                resultSet.getObject("created_at", OffsetDateTime.class),
                resultSet.getObject("updated_at", OffsetDateTime.class));
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }

        return search.trim();
    }
}
