package si.um.feri.dotaops.backend.integration;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import si.um.feri.dotaops.backend.BackendApplication;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = BackendApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@EnabledIfEnvironmentVariable(named = "SUPABASE_DB_URL", matches = ".+")
class PublicTournamentOverviewIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicEndpointsExposeOnlyPublicTournamentDataFromRealTables() throws Exception {
        String suffix = uniqueSuffix();
        UUID tournamentId = asServiceRole(() -> insertTournament(
                "public-overview-" + suffix,
                "Public Overview " + suffix,
                true,
                "published"));
        UUID privateTournamentId = asServiceRole(() -> insertTournament(
                "private-overview-" + suffix,
                "Private Overview " + suffix,
                false,
                "draft"));

        UUID groupId = asServiceRole(() -> insertGroup(tournamentId, "Group A " + suffix, 1));
        UUID teamAId = asServiceRole(() -> insertTeam("Radiant Five " + suffix, "radiant-five-" + suffix, "R5"));
        UUID teamBId = asServiceRole(() -> insertTeam("Dire Stack " + suffix, "dire-stack-" + suffix, "DS"));
        UUID teamCId = asServiceRole(() -> insertTeam("Ancient Crew " + suffix, "ancient-crew-" + suffix, "AC"));

        UUID registrationAId = asServiceRole(() -> insertRegistration(tournamentId, teamAId, 1));
        UUID registrationBId = asServiceRole(() -> insertRegistration(tournamentId, teamBId, 2));
        asServiceRole(() -> insertRegistration(tournamentId, teamCId, 3));

        UUID groupMatchId = asServiceRole(() -> insertMatch(
                tournamentId,
                groupId,
                "Group Stage",
                "Group Stage",
                1,
                1,
                teamAId,
                teamBId,
                "finished",
                3,
                2,
                1,
                teamAId));

        UUID finalMatchId = asServiceRole(() -> insertMatch(
                tournamentId,
                null,
                "Playoffs",
                "Final",
                1,
                1,
                teamAId,
                teamCId,
                "scheduled",
                3,
                0,
                0,
                null));

        asServiceRole(() -> {
            insertGroupTeam(groupId, teamAId, registrationAId, 1);
            insertGroupTeam(groupId, teamBId, registrationBId, 2);
            insertSlot(finalMatchId, "team_a", "winner", teamAId, groupMatchId, 1);
            insertSlot(finalMatchId, "team_b", "seed", teamCId, null, 3);
            return null;
        });

        mockMvc.perform(get("/api/public/tournaments")
                        .queryParam("search", suffix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].id").value(tournamentId.toString()))
                .andExpect(jsonPath("$.data.items[0].title").value("Public Overview " + suffix))
                .andExpect(jsonPath("$.data.items[0].teamCount").value(3))
                .andExpect(jsonPath("$.data.items[0].groupCount").value(1))
                .andExpect(jsonPath("$.data.items[0].matchCount").value(2))
                .andExpect(jsonPath("$.data.items[0].finishedMatchCount").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value(not(containsString("Private"))));

        mockMvc.perform(get("/api/public/tournaments/" + tournamentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(tournamentId.toString()))
                .andExpect(jsonPath("$.data.teams", hasSize(3)))
                .andExpect(jsonPath("$.data.teams[0].id").value(teamAId.toString()))
                .andExpect(jsonPath("$.data.groups[0].groupId").value(groupId.toString()))
                .andExpect(jsonPath("$.data.groups[0].teams", hasSize(2)))
                .andExpect(jsonPath("$.data.matches", hasSize(2)))
                .andExpect(jsonPath("$.data.metrics.approvedTeamCount").value(3))
                .andExpect(jsonPath("$.data.metrics.totalGamesPlayed").value(3))
                .andExpect(jsonPath("$.data.links.matches").value("/api/public/tournaments/" + tournamentId + "/matches"));

        mockMvc.perform(get("/api/public/tournaments/" + tournamentId + "/standings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].teamId").value(teamAId.toString()))
                .andExpect(jsonPath("$.data[0].matchWins").value(1))
                .andExpect(jsonPath("$.data[0].gameWins").value(2))
                .andExpect(jsonPath("$.data[0].gameLosses").value(1))
                .andExpect(jsonPath("$.data[0].points").value(3));

        mockMvc.perform(get("/api/public/tournaments/" + tournamentId + "/bracket")
                        .queryParam("stageName", "Playoffs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rounds[0].matches[0].matchId").value(finalMatchId.toString()))
                .andExpect(jsonPath("$.data.rounds[0].matches[0].teamA.id").value(teamAId.toString()))
                .andExpect(jsonPath("$.data.rounds[0].matches[0].slots[0].sourceType").value("winner"))
                .andExpect(jsonPath("$.data.rounds[0].matches[0].slots[0].sourceMatchId").value(groupMatchId.toString()))
                .andExpect(jsonPath("$.data.rounds[0].matches[0].slots[0].team.id").value(teamAId.toString()));

        mockMvc.perform(get("/api/public/tournaments/" + tournamentId + "/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matchCount").value(2))
                .andExpect(jsonPath("$.data.finishedMatchCount").value(1))
                .andExpect(jsonPath("$.data.totalGamesPlayed").value(3))
                .andExpect(jsonPath("$.data.averageGamesPerFinishedMatch").value(3.0));

        mockMvc.perform(get("/api/public/tournaments/" + privateTournamentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    private UUID insertTournament(String slug, String title, boolean publicVisible, String status) {
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
                  max_teams,
                  settings,
                  published_at
                )
                values (
                  ?,
                  ?,
                  ?::public.dotaops_tournament_status,
                  'groups_playoff'::public.dotaops_tournament_format,
                  now() + interval '14 days',
                  now() + interval '7 days',
                  ?,
                  8,
                  '{"maxTeams":8,"minTeams":2,"teamSize":5,"bestOf":3,"format":"groups_playoff","checkInEnabled":false,"allowSubstitutes":true}'::jsonb,
                  case when ? then now() else null end
                )
                returning id
                """,
                UUID.class,
                slug,
                title,
                status,
                publicVisible,
                publicVisible);
    }

    private UUID insertTeam(String name, String slug, String tag) {
        return jdbcTemplate.queryForObject(
                """
                insert into public.teams (name, slug, tag, logo_url)
                values (?, ?, ?, ?)
                returning id
                """,
                UUID.class,
                name,
                slug,
                tag,
                "https://cdn.example.test/" + slug + ".png");
    }

    private UUID insertRegistration(UUID tournamentId, UUID teamId, int seedNumber) {
        return jdbcTemplate.queryForObject(
                """
                insert into public.tournament_registrations (tournament_id, team_id, status, seed_number)
                values (?, ?, 'approved'::public.dotaops_registration_status, ?)
                returning id
                """,
                UUID.class,
                tournamentId,
                teamId,
                seedNumber);
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
            String stageName,
            String roundName,
            int roundNumber,
            int bracketPosition,
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
                  stage_name,
                  round_name,
                  round_number,
                  bracket_position,
                  team_a_id,
                  team_b_id,
                  status,
                  best_of,
                  score_a,
                  score_b,
                  winner_team_id,
                  scheduled_at,
                  started_at,
                  finished_at
                )
                values (
                  ?,
                  ?,
                  ?,
                  ?,
                  ?,
                  ?,
                  ?,
                  ?,
                  ?::public.dotaops_match_status,
                  ?,
                  ?,
                  ?,
                  ?,
                  case when ? = 'finished' then now() - interval '2 hours' else now() + interval '1 day' end,
                  case when ? = 'finished' then now() - interval '1 hour' else null end,
                  case when ? = 'finished' then now() - interval '30 minutes' else null end
                )
                returning id
                """,
                UUID.class,
                tournamentId,
                groupId,
                stageName,
                roundName,
                roundNumber,
                bracketPosition,
                teamAId,
                teamBId,
                status,
                bestOf,
                scoreA,
                scoreB,
                winnerTeamId,
                status,
                status,
                status);
    }

    private UUID insertSlot(
            UUID matchId,
            String slot,
            String sourceType,
            UUID teamId,
            UUID sourceMatchId,
            int seedNumber
    ) {
        return jdbcTemplate.queryForObject(
                """
                insert into public.match_slots (
                  match_id,
                  slot,
                  source_type,
                  team_id,
                  source_match_id,
                  seed_number,
                  display_label
                )
                values (?, ?::public.dotaops_match_slot, ?::public.dotaops_match_slot_source, ?, ?, ?, ?)
                returning id
                """,
                UUID.class,
                matchId,
                slot,
                sourceType,
                teamId,
                sourceMatchId,
                seedNumber,
                sourceType + " slot");
    }
}
