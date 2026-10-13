package si.um.feri.dotaops.backend.tournament.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import si.um.feri.dotaops.backend.tournament.domain.GroupStanding;
import si.um.feri.dotaops.backend.tournament.domain.TournamentGroup;
import si.um.feri.dotaops.backend.tournament.domain.TournamentGroupTeam;

@Repository
public class TournamentGroupRepository {

    private final JdbcTemplate jdbcTemplate;

    public TournamentGroupRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TournamentGroup> findByTournamentId(UUID tournamentId) {
        return jdbcTemplate.query(
                selectGroupSql() + """
                where tg.tournament_id = ?
                order by tg.sort_order asc, tg.name asc, tg.id asc
                """,
                this::mapGroup,
                tournamentId);
    }

    public List<TournamentGroup> findPublicByTournamentId(UUID tournamentId) {
        return jdbcTemplate.query(
                selectGroupSql() + """
                join public.tournaments t on t.id = tg.tournament_id
                where tg.tournament_id = ?
                  and t.is_public = true
                  and t.status in ('registration', 'published', 'live', 'finished')
                order by tg.sort_order asc, tg.name asc, tg.id asc
                """,
                this::mapGroup,
                tournamentId);
    }

    public Optional<TournamentGroup> findById(UUID groupId) {
        return jdbcTemplate.query(
                        selectGroupSql() + """
                        where tg.id = ?
                        limit 1
                        """,
                        this::mapGroup,
                        groupId)
                .stream()
                .findFirst();
    }

    public boolean publicTournamentExists(UUID tournamentId) {
        Boolean exists = jdbcTemplate.queryForObject(
                """
                select exists (
                  select 1
                  from public.tournaments t
                  where t.id = ?
                    and t.is_public = true
                    and t.status in ('registration', 'published', 'live', 'finished')
                )
                """,
                Boolean.class,
                tournamentId);

        return Boolean.TRUE.equals(exists);
    }

    public boolean publicGroupExists(UUID groupId) {
        Boolean exists = jdbcTemplate.queryForObject(
                """
                select exists (
                  select 1
                  from public.tournament_groups tg
                  join public.tournaments t on t.id = tg.tournament_id
                  where tg.id = ?
                    and t.is_public = true
                    and t.status in ('registration', 'published', 'live', 'finished')
                )
                """,
                Boolean.class,
                groupId);

        return Boolean.TRUE.equals(exists);
    }

    public int nextSortOrder(UUID tournamentId) {
        Integer nextSortOrder = jdbcTemplate.queryForObject(
                """
                select coalesce(max(sort_order), 0) + 1
                from public.tournament_groups
                where tournament_id = ?
                """,
                Integer.class,
                tournamentId);

        return nextSortOrder == null ? 1 : nextSortOrder;
    }

    public TournamentGroup create(CreateTournamentGroupCommand command) {
        return jdbcTemplate.queryForObject(
                """
                insert into public.tournament_groups (
                  tournament_id,
                  name,
                  sort_order
                )
                values (?, ?, ?)
                returning
                  id,
                  tournament_id,
                  name,
                  sort_order,
                  created_at,
                  updated_at
                """,
                this::mapGroup,
                command.tournamentId(),
                command.name(),
                command.sortOrder());
    }

    public List<TournamentGroupTeam> findTeamsByGroupId(UUID groupId) {
        return jdbcTemplate.query(
                selectGroupTeamSql() + """
                where tgt.group_id = ?
                order by tgt.seed_number nulls last, tm.name asc, tgt.created_at asc, tgt.id asc
                """,
                this::mapGroupTeam,
                groupId);
    }

    public Optional<TournamentGroupTeam> findAssignmentByTournamentAndTeam(UUID tournamentId, UUID teamId) {
        return jdbcTemplate.query(
                        selectGroupTeamSql() + """
                        where tg.tournament_id = ?
                          and tgt.team_id = ?
                        order by tgt.created_at asc, tgt.id asc
                        limit 1
                        """,
                        this::mapGroupTeam,
                        tournamentId,
                        teamId)
                .stream()
                .findFirst();
    }

    public Optional<UUID> findApprovedRegistrationId(UUID tournamentId, UUID teamId) {
        return jdbcTemplate.query(
                        """
                        select tr.id
                        from public.tournament_registrations tr
                        where tr.tournament_id = ?
                          and tr.team_id = ?
                          and tr.status = 'approved'
                        limit 1
                        """,
                        (resultSet, rowNumber) -> resultSet.getObject("id", UUID.class),
                        tournamentId,
                        teamId)
                .stream()
                .findFirst();
    }

    public TournamentGroupTeam addTeam(AddTournamentGroupTeamCommand command) {
        UUID assignmentId = jdbcTemplate.queryForObject(
                """
                insert into public.tournament_group_teams (
                  group_id,
                  team_id,
                  registration_id,
                  seed_number
                )
                values (?, ?, ?, ?)
                returning id
                """,
                UUID.class,
                command.groupId(),
                command.teamId(),
                command.registrationId(),
                command.seedNumber());

        return findGroupTeamById(assignmentId).orElseThrow();
    }

    public Optional<TournamentGroupTeam> removeTeam(UUID groupId, UUID teamId) {
        Optional<TournamentGroupTeam> existing = findTeamInGroup(groupId, teamId);
        existing.ifPresent(assignment -> jdbcTemplate.update(
                """
                delete from public.tournament_group_teams
                where id = ?
                """,
                assignment.id()));

        return existing;
    }

    public List<GroupStanding> findStandingsByGroupId(UUID groupId) {
        return jdbcTemplate.query(
                selectCalculatedStandingsSql() + """
                where group_id = ?
                order by rank asc, team_name asc
                """,
                this::mapStanding,
                groupId);
    }

    public List<GroupStanding> findOrganizerStandingsByTournamentId(UUID tournamentId) {
        return jdbcTemplate.query(
                selectCalculatedStandingsSql() + """
                where tournament_id = ?
                order by group_sort_order asc, group_name asc, rank asc, team_name asc
                """,
                this::mapStanding,
                tournamentId);
    }

    private Optional<TournamentGroupTeam> findGroupTeamById(UUID assignmentId) {
        return jdbcTemplate.query(
                        selectGroupTeamSql() + """
                        where tgt.id = ?
                        limit 1
                        """,
                        this::mapGroupTeam,
                        assignmentId)
                .stream()
                .findFirst();
    }

    private Optional<TournamentGroupTeam> findTeamInGroup(UUID groupId, UUID teamId) {
        return jdbcTemplate.query(
                        selectGroupTeamSql() + """
                        where tgt.group_id = ?
                          and tgt.team_id = ?
                        limit 1
                        """,
                        this::mapGroupTeam,
                        groupId,
                        teamId)
                .stream()
                .findFirst();
    }

    private String selectGroupSql() {
        return """
                select
                  tg.id,
                  tg.tournament_id,
                  tg.name,
                  tg.sort_order,
                  tg.created_at,
                  tg.updated_at
                from public.tournament_groups tg
                """;
    }

    private String selectGroupTeamSql() {
        return """
                select
                  tgt.id,
                  tgt.group_id,
                  tg.tournament_id,
                  tgt.team_id,
                  tm.name as team_name,
                  tm.tag as team_tag,
                  tm.slug as team_slug,
                  tgt.registration_id,
                  tgt.seed_number,
                  tgt.created_at,
                  tgt.updated_at
                from public.tournament_group_teams tgt
                join public.tournament_groups tg on tg.id = tgt.group_id
                join public.teams tm on tm.id = tgt.team_id
                """;
    }

    private String selectCalculatedStandingsSql() {
        return """
                with team_match_results as (
                  select
                    tg.id as group_id,
                    tg.name as group_name,
                    tg.sort_order as group_sort_order,
                    tg.tournament_id,
                    tgt.team_id,
                    tm.name as team_name,
                    m.id as match_id,
                    case
                      when m.status = 'finished'::public.dotaops_match_status
                        and (
                          (m.team_a_id = tgt.team_id and m.winner_team_id = tgt.team_id)
                          or (m.team_b_id = tgt.team_id and m.winner_team_id = tgt.team_id)
                          or (
                            m.winner_team_id is null
                            and (
                              (m.team_a_id = tgt.team_id and m.score_a > m.score_b)
                              or (m.team_b_id = tgt.team_id and m.score_b > m.score_a)
                            )
                          )
                        )
                        then 'win'
                      when m.status = 'finished'::public.dotaops_match_status
                        and m.score_a = m.score_b
                        and m.winner_team_id is null
                        then 'draw'
                      when m.status = 'finished'::public.dotaops_match_status
                        and (
                          (m.team_a_id = tgt.team_id and m.team_b_id is not null)
                          or (m.team_b_id = tgt.team_id and m.team_a_id is not null)
                        )
                        then 'loss'
                      else null
                    end as match_result,
                    case
                      when m.status <> 'finished'::public.dotaops_match_status then 0
                      when m.team_a_id = tgt.team_id then m.score_a
                      when m.team_b_id = tgt.team_id then m.score_b
                      else 0
                    end as game_wins,
                    case
                      when m.status <> 'finished'::public.dotaops_match_status then 0
                      when m.team_a_id = tgt.team_id then m.score_b
                      when m.team_b_id = tgt.team_id then m.score_a
                      else 0
                    end as game_losses
                  from public.tournament_groups tg
                  join public.tournament_group_teams tgt on tgt.group_id = tg.id
                  join public.teams tm on tm.id = tgt.team_id
                  left join public.matches m
                    on m.group_id = tg.id
                    and m.tournament_id = tg.tournament_id
                    and (m.team_a_id = tgt.team_id or m.team_b_id = tgt.team_id)
                ),
                standings as (
                  select
                    group_id,
                    group_name,
                    group_sort_order,
                    tournament_id,
                    team_id,
                    team_name,
                    count(match_id) filter (where match_result is not null)::integer as matches_played,
                    count(match_id) filter (where match_result = 'win')::integer as match_wins,
                    count(match_id) filter (where match_result = 'loss')::integer as match_losses,
                    count(match_id) filter (where match_result = 'draw')::integer as match_draws,
                    coalesce(sum(game_wins), 0)::integer as game_wins,
                    coalesce(sum(game_losses), 0)::integer as game_losses
                  from team_match_results
                  group by group_id, group_name, group_sort_order, tournament_id, team_id, team_name
                )
                select
                  group_id,
                  group_name,
                  group_sort_order,
                  tournament_id,
                  team_id,
                  team_name,
                  matches_played,
                  match_wins,
                  match_losses,
                  match_draws,
                  game_wins,
                  game_losses,
                  (game_wins - game_losses)::integer as game_diff,
                  ((match_wins * 3) + match_draws)::integer as points,
                  (row_number() over (
                    partition by group_id
                    order by
                      ((match_wins * 3) + match_draws) desc,
                      match_wins desc,
                      (game_wins - game_losses) desc,
                      game_wins desc,
                      team_name asc
                  ))::integer as rank
                from standings
                """;
    }

    private TournamentGroup mapGroup(ResultSet resultSet, int rowNumber) throws SQLException {
        return new TournamentGroup(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("tournament_id", UUID.class),
                resultSet.getString("name"),
                resultSet.getInt("sort_order"),
                resultSet.getObject("created_at", OffsetDateTime.class),
                resultSet.getObject("updated_at", OffsetDateTime.class));
    }

    private TournamentGroupTeam mapGroupTeam(ResultSet resultSet, int rowNumber) throws SQLException {
        return new TournamentGroupTeam(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("group_id", UUID.class),
                resultSet.getObject("tournament_id", UUID.class),
                resultSet.getObject("team_id", UUID.class),
                resultSet.getString("team_name"),
                resultSet.getString("team_tag"),
                resultSet.getString("team_slug"),
                resultSet.getObject("registration_id", UUID.class),
                resultSet.getObject("seed_number", Integer.class),
                resultSet.getObject("created_at", OffsetDateTime.class),
                resultSet.getObject("updated_at", OffsetDateTime.class));
    }

    private GroupStanding mapStanding(ResultSet resultSet, int rowNumber) throws SQLException {
        return new GroupStanding(
                resultSet.getObject("group_id", UUID.class),
                resultSet.getString("group_name"),
                resultSet.getObject("tournament_id", UUID.class),
                resultSet.getObject("team_id", UUID.class),
                resultSet.getString("team_name"),
                resultSet.getInt("matches_played"),
                resultSet.getInt("match_wins"),
                resultSet.getInt("match_losses"),
                resultSet.getInt("match_draws"),
                resultSet.getInt("game_wins"),
                resultSet.getInt("game_losses"),
                resultSet.getInt("game_diff"),
                resultSet.getInt("points"),
                resultSet.getInt("rank"));
    }
}
