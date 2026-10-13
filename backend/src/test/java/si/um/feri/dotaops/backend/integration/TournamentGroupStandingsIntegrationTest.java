package si.um.feri.dotaops.backend.integration;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.ActiveProfiles;

import si.um.feri.dotaops.backend.tournament.repository.TournamentGroupRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("integration")
@EnabledIfEnvironmentVariable(named = "SUPABASE_DB_URL", matches = ".+")
class TournamentGroupStandingsIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private TournamentGroupRepository groupRepository;

    @Test
    void groupStandingsUseFinishedMatchScoresAsDotaGames() {
        String suffix = uniqueSuffix();

        UUID tournamentId = asServiceRole(() -> insertTournament(suffix));
        UUID groupId = asServiceRole(() -> insertGroup(tournamentId, "Group A " + suffix, 1));
        UUID teamAId = asServiceRole(() -> insertTeam("Radiant Five " + suffix, "radiant-five-" + suffix));
        UUID teamBId = asServiceRole(() -> insertTeam("Dire Stack " + suffix, "dire-stack-" + suffix));
        UUID teamCId = asServiceRole(() -> insertTeam("Ancient Crew " + suffix, "ancient-crew-" + suffix));

        UUID registrationAId = asServiceRole(() -> insertRegistration(tournamentId, teamAId));
        UUID registrationBId = asServiceRole(() -> insertRegistration(tournamentId, teamBId));
        UUID registrationCId = asServiceRole(() -> insertRegistration(tournamentId, teamCId));

        asServiceRole(() -> {
            insertGroupTeam(groupId, teamAId, registrationAId, 1);
            insertGroupTeam(groupId, teamBId, registrationBId, 2);
            insertGroupTeam(groupId, teamCId, registrationCId, 3);
            insertMatch(tournamentId, groupId, teamAId, teamBId, "finished", 3, 2, 1, teamAId);
            insertMatch(tournamentId, groupId, teamAId, teamCId, "scheduled", 1, 1, 0, teamAId);
            return null;
        });

        List<Map<String, Object>> standings = asServiceRole(() -> jdbcTemplate.queryForList(
                """
                select
                  team_id,
                  matches_played,
                  match_wins,
                  match_losses,
                  match_draws,
                  game_wins,
                  game_losses,
                  game_diff,
                  points,
                  rank
                from public.v_group_standings
                where group_id = ?
                order by rank asc
                """,
                groupId));

        assertThat(standings).hasSize(3);

        Map<String, Object> first = standings.getFirst();
        assertThat(first.get("team_id")).isEqualTo(teamAId);
        assertThat(first.get("matches_played")).isEqualTo(1);
        assertThat(first.get("match_wins")).isEqualTo(1);
        assertThat(first.get("match_losses")).isEqualTo(0);
        assertThat(first.get("match_draws")).isEqualTo(0);
        assertThat(first.get("game_wins")).isEqualTo(2);
        assertThat(first.get("game_losses")).isEqualTo(1);
        assertThat(first.get("game_diff")).isEqualTo(1);
        assertThat(first.get("points")).isEqualTo(3);

        Map<String, Object> second = standings.get(1);
        assertThat(second.get("team_id")).isEqualTo(teamCId);
        assertThat(second.get("matches_played")).isEqualTo(0);
        assertThat(second.get("game_wins")).isEqualTo(0);
        assertThat(second.get("game_losses")).isEqualTo(0);

        Map<String, Object> third = standings.get(2);
        assertThat(third.get("team_id")).isEqualTo(teamBId);
        assertThat(third.get("matches_played")).isEqualTo(1);
        assertThat(third.get("match_losses")).isEqualTo(1);
        assertThat(third.get("game_wins")).isEqualTo(1);
        assertThat(third.get("game_losses")).isEqualTo(2);
        assertThat(third.get("game_diff")).isEqualTo(-1);
    }

    @Test
    void organizerRepositoryStandingsIncludePrivateTournaments() {
        String suffix = uniqueSuffix();

        UUID tournamentId = asServiceRole(() -> insertTournament(suffix, false));
        UUID groupId = asServiceRole(() -> insertGroup(tournamentId, "Group A " + suffix, 1));
        UUID teamAId = asServiceRole(() -> insertTeam("Radiant Five " + suffix, "radiant-private-" + suffix));
        UUID teamBId = asServiceRole(() -> insertTeam("Dire Stack " + suffix, "dire-private-" + suffix));
        UUID registrationAId = asServiceRole(() -> insertRegistration(tournamentId, teamAId));
        UUID registrationBId = asServiceRole(() -> insertRegistration(tournamentId, teamBId));

        asServiceRole(() -> {
            insertGroupTeam(groupId, teamAId, registrationAId, 1);
            insertGroupTeam(groupId, teamBId, registrationBId, 2);
            insertMatch(tournamentId, groupId, teamAId, teamBId, "finished", 3, 2, 0, teamAId);
            return null;
        });

        List<Map<String, Object>> publicStandings = asServiceRole(() -> jdbcTemplate.queryForList(
                """
                select team_id
                from public.v_group_standings
                where group_id = ?
                """,
                groupId));

        var organizerStandings = asServiceRole(() -> groupRepository.findOrganizerStandingsByTournamentId(tournamentId));

        assertThat(publicStandings).isEmpty();
        assertThat(organizerStandings).hasSize(2);
        assertThat(organizerStandings.getFirst().groupName()).startsWith("Group A");
        assertThat(organizerStandings.getFirst().teamId()).isEqualTo(teamAId);
        assertThat(organizerStandings.getFirst().points()).isEqualTo(3);
    }

    @Test
    void databaseRejectsSameTeamInMultipleGroupsForOneTournament() {
        String suffix = uniqueSuffix();
        UUID tournamentId = asServiceRole(() -> insertTournament(suffix));
        UUID groupAId = asServiceRole(() -> insertGroup(tournamentId, "Group A " + suffix, 1));
        UUID groupBId = asServiceRole(() -> insertGroup(tournamentId, "Group B " + suffix, 2));
        UUID teamId = asServiceRole(() -> insertTeam("Duplicate Seed " + suffix, "duplicate-seed-" + suffix));
        UUID registrationId = asServiceRole(() -> insertRegistration(tournamentId, teamId));

        asServiceRole(() -> {
            insertGroupTeam(groupAId, teamId, registrationId, 1);
            return null;
        });

        assertThatThrownBy(() -> asServiceRole(() -> {
            insertGroupTeam(groupBId, teamId, registrationId, 1);
            return null;
        })).isInstanceOf(DataAccessException.class);
    }

    private UUID insertTournament(String suffix) {
        return insertTournament(suffix, true);
    }

    private UUID insertTournament(String suffix, boolean isPublic) {
        return jdbcTemplate.queryForObject(
                """
                insert into public.tournaments (
                  slug,
                  title,
                  status,
                  format,
                  starts_at,
                  registration_closes_at,
                  is_public,
                  max_teams
                )
                values (
                  ?,
                  ?,
                  'published'::public.dotaops_tournament_status,
                  'groups_playoff'::public.dotaops_tournament_format,
                  now() + interval '14 days',
                  now() + interval '7 days',
                  ?,
                  8
                )
                returning id
                """,
                UUID.class,
                "group-standings-" + suffix,
                "Group Standings " + suffix,
                isPublic);
    }

    private UUID insertGroup(UUID tournamentId, String name, int sortOrder) {
        return jdbcTemplate.queryForObject(
                """
                insert into public.tournament_groups (tournament_id, name, sort_order)
                values (?, ?, ?)
                returning id
                """,
                UUID.class,
                tournamentId,
                name,
                sortOrder);
    }

    private UUID insertTeam(String name, String slug) {
        return jdbcTemplate.queryForObject(
                """
                insert into public.teams (name, slug)
                values (?, ?)
                returning id
                """,
                UUID.class,
                name,
                slug);
    }

    private UUID insertRegistration(UUID tournamentId, UUID teamId) {
        return jdbcTemplate.queryForObject(
                """
                insert into public.tournament_registrations (tournament_id, team_id, status)
                values (?, ?, 'approved'::public.dotaops_registration_status)
                returning id
                """,
                UUID.class,
                tournamentId,
                teamId);
    }

    private UUID insertGroupTeam(UUID groupId, UUID teamId, UUID registrationId, int seedNumber) {
        return jdbcTemplate.queryForObject(
                """
                insert into public.tournament_group_teams (group_id, team_id, registration_id, seed_number)
                values (?, ?, ?, ?)
                returning id
                """,
                UUID.class,
                groupId,
                teamId,
                registrationId,
                seedNumber);
    }

    private UUID insertMatch(
            UUID tournamentId,
            UUID groupId,
            UUID teamAId,
            UUID teamBId,
            String status,
            int bestOf,
            int scoreA,
            int scoreB,
            UUID winnerTeamId
    ) {
        return jdbcTemplate.queryForObject(
                """
                insert into public.matches (
                  tournament_id,
                  group_id,
                  round_name,
                  round_number,
                  team_a_id,
                  team_b_id,
                  status,
                  best_of,
                  score_a,
                  score_b,
                  winner_team_id
                )
                values (?, ?, 'Group Stage', 1, ?, ?, ?::public.dotaops_match_status, ?, ?, ?, ?)
                returning id
                """,
                UUID.class,
                tournamentId,
                groupId,
                teamAId,
                teamBId,
                status,
                bestOf,
                scoreA,
                scoreB,
                winnerTeamId);
    }
}
