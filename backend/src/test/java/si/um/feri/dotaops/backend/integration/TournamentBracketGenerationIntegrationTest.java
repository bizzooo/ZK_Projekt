package si.um.feri.dotaops.backend.integration;

import java.time.Instant;
import java.util.List;
import java.util.Map;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class TournamentBracketGenerationIntegrationTest extends PostgresIntegrationTestSupport {

    private static final UUID ORGANIZER_AUTH_USER_ID = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb2");

    @Autowired
    private MockMvc mockMvc;

    private UUID organizerProfileId;

    @BeforeEach
    void seedOrganizer() {
        organizerProfileId = upsertProfile(ORGANIZER_AUTH_USER_ID, "organizer");
    }

    @Test
    void organizerEndpointGeneratesSeededBracketAndIgnoresPendingRegistrations() throws Exception {
        String suffix = uniqueSuffix();
        String stageName = "Playoffs " + suffix;
        UUID tournamentId = insertTournament(suffix);
        UUID seedOneTeamId = insertTeam("Seed One " + suffix, "seed-one-" + suffix);
        UUID seedTwoTeamId = insertTeam("Seed Two " + suffix, "seed-two-" + suffix);
        UUID seedThreeTeamId = insertTeam("Seed Three " + suffix, "seed-three-" + suffix);
        UUID seedFourTeamId = insertTeam("Seed Four " + suffix, "seed-four-" + suffix);
        UUID pendingTeamId = insertTeam("Pending " + suffix, "pending-" + suffix);

        insertRegistration(tournamentId, seedOneTeamId, "approved", 1);
        insertRegistration(tournamentId, seedTwoTeamId, "approved", 2);
        insertRegistration(tournamentId, seedThreeTeamId, "approved", 3);
        insertRegistration(tournamentId, seedFourTeamId, "approved", 4);
        insertRegistration(tournamentId, pendingTeamId, "pending", 5);

        mockMvc.perform(post("/api/organizer/tournaments/" + tournamentId + "/bracket/generate")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stageName": "%s",
                                  "forceRegenerate": false
                                }
                                """.formatted(stageName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bracketSize").value(4))
                .andExpect(jsonPath("$.data.matches.length()").value(3));

        List<Map<String, Object>> slots = jdbcTemplate.queryForList(
                """
                select
                  m.round_number,
                  m.bracket_position,
                  m.round_name,
                  ms.slot::text as slot,
                  ms.source_type::text as source_type,
                  ms.team_id,
                  ms.seed_number,
                  ms.source_match_id
                from public.matches m
                join public.match_slots ms on ms.match_id = m.id
                where m.tournament_id = ?
                  and m.stage_name = ?
                order by m.round_number, m.bracket_position, ms.slot
                """,
                tournamentId,
                stageName);

        assertThat(slots).hasSize(6);
        assertSlot(slots.get(0), 1, 1, "Semifinal", "team_a", "seed", seedOneTeamId, 1, null);
        assertSlot(slots.get(1), 1, 1, "Semifinal", "team_b", "seed", seedFourTeamId, 4, null);
        assertSlot(slots.get(2), 1, 2, "Semifinal", "team_a", "seed", seedTwoTeamId, 2, null);
        assertSlot(slots.get(3), 1, 2, "Semifinal", "team_b", "seed", seedThreeTeamId, 3, null);
        assertThat(slots.get(4).get("source_type")).isEqualTo("winner");
        assertThat(slots.get(4).get("source_match_id")).isNotNull();
        assertThat(slots.get(5).get("source_type")).isEqualTo("winner");
        assertThat(slots.get(5).get("source_match_id")).isNotNull();

        List<UUID> assignedTeams = slots.stream()
                .map(row -> (UUID) row.get("team_id"))
                .filter(teamId -> teamId != null)
                .toList();
        assertThat(assignedTeams).doesNotContain(pendingTeamId);
    }

    private void assertSlot(
            Map<String, Object> row,
            int roundNumber,
            int bracketPosition,
            String roundName,
            String slot,
            String sourceType,
            UUID teamId,
            int seedNumber,
            UUID sourceMatchId
    ) {
        assertThat(row.get("round_number")).isEqualTo(roundNumber);
        assertThat(row.get("bracket_position")).isEqualTo(bracketPosition);
        assertThat(row.get("round_name")).isEqualTo(roundName);
        assertThat(row.get("slot")).isEqualTo(slot);
        assertThat(row.get("source_type")).isEqualTo(sourceType);
        assertThat(row.get("team_id")).isEqualTo(teamId);
        assertThat(row.get("seed_number")).isEqualTo(seedNumber);
        assertThat(row.get("source_match_id")).isEqualTo(sourceMatchId);
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
                "bracket-generation-" + suffix,
                "Bracket Generation " + suffix,
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

    private UUID insertRegistration(UUID tournamentId, UUID teamId, String status, int seedNumber) {
        return jdbcTemplate.queryForObject(
                """
                insert into public.tournament_registrations (tournament_id, team_id, status, seed_number)
                values (?, ?, ?::public.dotaops_registration_status, ?)
                returning id
                """,
                UUID.class,
                tournamentId,
                teamId,
                status,
                seedNumber);
    }

    private static String bearerToken() throws Exception {
        return "Bearer " + SupabaseJwtTestSupport.token(ORGANIZER_AUTH_USER_ID, Instant.now());
    }
}
