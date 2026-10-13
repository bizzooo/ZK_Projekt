package si.um.feri.dotaops.backend.integration;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import si.um.feri.dotaops.backend.auth.service.SupabaseJwtTestSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@EnabledIfEnvironmentVariable(named = "SUPABASE_DB_URL", matches = ".+")
@TestPropertySource(properties = {
        "dotaops.supabase.auth.jwt-secret=" + SupabaseJwtTestSupport.SECRET,
        "dotaops.supabase.auth.issuer=" + SupabaseJwtTestSupport.ISSUER,
        "dotaops.supabase.auth.audience=" + SupabaseJwtTestSupport.AUDIENCE
})
class MatchManagementIntegrationTest extends PostgresIntegrationTestSupport {

    private static final UUID ORGANIZER_AUTH_USER_ID = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb3");

    @Autowired
    private MockMvc mockMvc;

    private UUID organizerProfileId;

    @BeforeEach
    void seedOrganizer() {
        organizerProfileId = upsertProfile(ORGANIZER_AUTH_USER_ID, "organizer");
    }

    @Test
    void organizerCanScheduleSubmitResultAndPropagateWinnerAndLoserToNextRound() throws Exception {
        String suffix = uniqueSuffix();
        String stageName = "Playoffs " + suffix;
        UUID tournamentId = insertTournament(suffix);
        UUID teamAId = insertTeam("Match Team A " + suffix, "match-team-a-" + suffix);
        UUID teamBId = insertTeam("Match Team B " + suffix, "match-team-b-" + suffix);
        UUID semifinalId = insertMatch(tournamentId, stageName, "Semifinal", 1, 1, 3, teamAId, teamBId);
        UUID finalId = insertMatch(tournamentId, stageName, "Final", 2, 1, 3, null, null);
        UUID consolationId = insertMatch(tournamentId, stageName, "Third Place", 2, 2, 3, null, null);
        insertSourceSlot(finalId, semifinalId, "team_a", "winner", false);
        insertSourceSlot(consolationId, semifinalId, "team_a", "loser", false);

        mockMvc.perform(patch("/api/organizer/matches/" + semifinalId + "/schedule")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scheduledAt": "2026-06-20T18:00:00Z"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("scheduled"))
                .andExpect(jsonPath("$.data.scheduledAt").value("2026-06-20T18:00:00Z"));

        mockMvc.perform(get("/api/tournaments/" + tournamentId + "/matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].scheduledAt").value("2026-06-20T18:00:00Z"));

        mockMvc.perform(patch("/api/organizer/matches/" + semifinalId + "/result")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scoreA": 2,
                                  "scoreB": 1,
                                  "winnerTeamId": "%s"
                                }
                                """.formatted(teamAId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("finished"))
                .andExpect(jsonPath("$.data.scoreA").value(2))
                .andExpect(jsonPath("$.data.scoreB").value(1))
                .andExpect(jsonPath("$.data.winnerTeamId").value(teamAId.toString()));

        mockMvc.perform(get("/api/tournaments/" + tournamentId + "/matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("finished"))
                .andExpect(jsonPath("$.data[0].winnerTeamId").value(teamAId.toString()))
                .andExpect(jsonPath("$.data[1].teamAId").value(teamAId.toString()));

        mockMvc.perform(get("/api/tournaments/" + tournamentId + "/bracket")
                        .queryParam("stageName", stageName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matches[0].scoreA").value(2))
                .andExpect(jsonPath("$.data.matches[0].winnerTeamId").value(teamAId.toString()))
                .andExpect(jsonPath("$.data.matches[1].teamAId").value(teamAId.toString()))
                .andExpect(jsonPath("$.data.matches[1].slots[0].teamId").value(teamAId.toString()))
                .andExpect(jsonPath("$.data.matches[2].teamAId").value(teamBId.toString()))
                .andExpect(jsonPath("$.data.matches[2].slots[0].teamId").value(teamBId.toString()));

        UUID propagatedSlotTeamId = jdbcTemplate.queryForObject(
                """
                select team_id
                from public.match_slots
                where match_id = ?
                  and source_type = 'winner'
                  and source_match_id = ?
                """,
                UUID.class,
                finalId,
                semifinalId);
        UUID propagatedLoserSlotTeamId = jdbcTemplate.queryForObject(
                """
                select team_id
                from public.match_slots
                where match_id = ?
                  and source_type = 'loser'
                  and source_match_id = ?
                """,
                UUID.class,
                consolationId,
                semifinalId);
        UUID propagatedMatchTeamId = jdbcTemplate.queryForObject(
                """
                select team_a_id
                from public.matches
                where id = ?
                """,
                UUID.class,
                finalId);

        assertThat(propagatedSlotTeamId).isEqualTo(teamAId);
        assertThat(propagatedLoserSlotTeamId).isEqualTo(teamBId);
        assertThat(propagatedMatchTeamId).isEqualTo(teamAId);
        List<String> auditReasons = jdbcTemplate.queryForList(
                """
                select reason
                from public.match_advancement_audit_logs
                where source_match_id = ?
                order by target_slot, source_type
                """,
                String.class,
                semifinalId);
        assertThat(auditReasons).containsExactlyInAnyOrder("AUTO_ADVANCE_WINNER", "AUTO_ADVANCE_LOSER");
    }

    @Test
    void lockedSlotConflictRollsBackResultAndDoesNotCreatePartialPropagation() throws Exception {
        String suffix = uniqueSuffix();
        String stageName = "Locked Playoffs " + suffix;
        UUID tournamentId = insertTournament("locked-" + suffix);
        UUID teamAId = insertTeam("Locked Team A " + suffix, "locked-team-a-" + suffix);
        UUID teamBId = insertTeam("Locked Team B " + suffix, "locked-team-b-" + suffix);
        UUID semifinalId = insertMatch(tournamentId, stageName, "Semifinal", 1, 1, 1, teamAId, teamBId);
        UUID finalId = insertMatch(tournamentId, stageName, "Final", 2, 1, 1, null, null);
        insertSourceSlot(finalId, semifinalId, "team_a", "winner", true);

        mockMvc.perform(patch("/api/organizer/matches/" + semifinalId + "/result")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scoreA": 1,
                                  "scoreB": 0,
                                  "winnerTeamId": "%s"
                                }
                                """.formatted(teamAId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        String sourceStatus = jdbcTemplate.queryForObject(
                """
                select status::text
                from public.matches
                where id = ?
                """,
                String.class,
                semifinalId);
        Integer sourceScoreA = jdbcTemplate.queryForObject(
                "select score_a from public.matches where id = ?",
                Integer.class,
                semifinalId);
        UUID sourceWinner = jdbcTemplate.queryForObject(
                "select winner_team_id from public.matches where id = ?",
                UUID.class,
                semifinalId);
        UUID lockedSlotTeamId = jdbcTemplate.queryForObject(
                "select team_id from public.match_slots where match_id = ?",
                UUID.class,
                finalId);
        Integer auditRows = jdbcTemplate.queryForObject(
                "select count(*) from public.match_advancement_audit_logs where source_match_id = ?",
                Integer.class,
                semifinalId);

        assertThat(sourceStatus).isEqualTo("scheduled");
        assertThat(sourceScoreA).isZero();
        assertThat(sourceWinner).isNull();
        assertThat(lockedSlotTeamId).isNull();
        assertThat(auditRows).isZero();
    }

    private UUID insertTournament(String suffix) {
        return jdbcTemplate.queryForObject(
                """
                insert into public.tournaments (
                  slug,
                  title,
                  status,
                  format,
                  organizer_profile_id,
                  starts_at,
                  registration_closes_at,
                  is_public,
                  max_teams
                )
                values (
                  ?,
                  ?,
                  'published'::public.dotaops_tournament_status,
                  'single_elimination'::public.dotaops_tournament_format,
                  ?,
                  now() + interval '14 days',
                  now() + interval '7 days',
                  true,
                  8
                )
                returning id
                """,
                UUID.class,
                "match-management-" + suffix,
                "Match Management " + suffix,
                organizerProfileId);
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

    private UUID insertMatch(
            UUID tournamentId,
            String stageName,
            String roundName,
            int roundNumber,
            int bracketPosition,
            int bestOf,
            UUID teamAId,
            UUID teamBId
    ) {
        return jdbcTemplate.queryForObject(
                """
                insert into public.matches (
                  tournament_id,
                  stage_name,
                  round_name,
                  round_number,
                  bracket_position,
                  status,
                  best_of,
                  team_a_id,
                  team_b_id
                )
                values (?, ?, ?, ?, ?, 'scheduled'::public.dotaops_match_status, ?, ?, ?)
                returning id
                """,
                UUID.class,
                tournamentId,
                stageName,
                roundName,
                roundNumber,
                bracketPosition,
                bestOf,
                teamAId,
                teamBId);
    }

    private void insertSourceSlot(UUID matchId, UUID sourceMatchId, String slot, String sourceType, boolean locked) {
        jdbcTemplate.update(
                """
                insert into public.match_slots (
                  match_id,
                  slot,
                  source_type,
                  source_match_id,
                  display_label,
                  is_locked
                )
                values (?, ?::public.dotaops_match_slot, ?::public.dotaops_match_slot_source, ?, ?, ?)
                """,
                matchId,
                slot,
                sourceType,
                sourceMatchId,
                sourceType + " of semifinal",
                locked);
    }

    private static String bearerToken() throws Exception {
        return "Bearer " + SupabaseJwtTestSupport.token(ORGANIZER_AUTH_USER_ID, Instant.now());
    }
}
