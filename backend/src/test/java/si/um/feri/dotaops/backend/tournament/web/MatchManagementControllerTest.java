package si.um.feri.dotaops.backend.tournament.web;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import si.um.feri.dotaops.backend.BackendApplication;
import si.um.feri.dotaops.backend.auth.domain.AuthenticatedProfile;
import si.um.feri.dotaops.backend.auth.domain.ProfileRole;
import si.um.feri.dotaops.backend.auth.repository.AuthenticatedProfileRepository;
import si.um.feri.dotaops.backend.auth.service.SupabaseJwtTestSupport;
import si.um.feri.dotaops.backend.tournament.dto.CancelMatchRequest;
import si.um.feri.dotaops.backend.tournament.dto.MatchResponse;
import si.um.feri.dotaops.backend.tournament.dto.ScheduleMatchRequest;
import si.um.feri.dotaops.backend.tournament.dto.SubmitMatchResultRequest;
import si.um.feri.dotaops.backend.tournament.service.MatchManagementService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {
        BackendApplication.class,
        MatchManagementControllerTest.MatchManagementControllerTestConfig.class
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "dotaops.supabase.auth.jwt-secret=" + SupabaseJwtTestSupport.SECRET,
        "dotaops.supabase.auth.issuer=" + SupabaseJwtTestSupport.ISSUER,
        "dotaops.supabase.auth.audience=" + SupabaseJwtTestSupport.AUDIENCE,
        "dotaops.steam.session.jwt-secret=" + SupabaseJwtTestSupport.SECRET,
        "dotaops.steam.session.ttl=1h"
})
class MatchManagementControllerTest {

    private static final UUID AUTH_USER_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID PROFILE_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID TOURNAMENT_ID = UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final UUID MATCH_ID = UUID.fromString("44444444-4444-4444-8444-444444444444");
    private static final UUID TEAM_A_ID = UUID.fromString("55555555-5555-4555-8555-555555555555");
    private static final UUID TEAM_B_ID = UUID.fromString("66666666-6666-4666-8666-666666666666");
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-01T18:00:00Z");
    private static final OffsetDateTime SCHEDULED_AT = OffsetDateTime.parse("2026-06-05T18:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MatchManagementService matchManagementService;

    @Autowired
    private AuthenticatedProfileRepository authenticatedProfileRepository;

    @BeforeEach
    void setUp() {
        Mockito.reset(matchManagementService, authenticatedProfileRepository);
        when(authenticatedProfileRepository.findByAuthUserId(AUTH_USER_ID))
                .thenReturn(Optional.of(authenticatedProfile(ProfileRole.ORGANIZER)));
    }

    @Test
    void publicMatchReadDoesNotRequireAuthentication() throws Exception {
        when(matchManagementService.getPublicMatch(MATCH_ID)).thenReturn(matchResponse("finished", 2, 1, TEAM_A_ID));

        mockMvc.perform(get("/api/matches/" + MATCH_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(MATCH_ID.toString()))
                .andExpect(jsonPath("$.data.scoreA").value(2))
                .andExpect(jsonPath("$.data.winnerTeamId").value(TEAM_A_ID.toString()));
    }

    @Test
    void publicTournamentMatchesReadDoesNotRequireAuthentication() throws Exception {
        when(matchManagementService.listPublicTournamentMatches(TOURNAMENT_ID))
                .thenReturn(List.of(matchResponse("scheduled", 0, 0, null)));

        mockMvc.perform(get("/api/tournaments/" + TOURNAMENT_ID + "/matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(MATCH_ID.toString()))
                .andExpect(jsonPath("$.data[0].status").value("scheduled"));
    }

    @Test
    void scheduleRequiresAuthentication() throws Exception {
        mockMvc.perform(patch("/api/organizer/matches/" + MATCH_ID + "/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scheduledAt": "2026-06-05T18:00:00Z"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void organizerCanScheduleMatch() throws Exception {
        when(matchManagementService.scheduleMatch(eq(MATCH_ID), any(ScheduleMatchRequest.class)))
                .thenReturn(matchResponse("scheduled", 0, 0, null));

        mockMvc.perform(patch("/api/organizer/matches/" + MATCH_ID + "/schedule")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scheduledAt": "2026-06-05T18:00:00Z"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("scheduled"))
                .andExpect(jsonPath("$.data.scheduledAt").value("2026-06-05T18:00:00Z"));
    }

    @Test
    void organizerCanStartCancelAndSubmitResult() throws Exception {
        when(matchManagementService.startMatch(MATCH_ID)).thenReturn(matchResponse("live", 0, 0, null));
        when(matchManagementService.cancelMatch(eq(MATCH_ID), any(CancelMatchRequest.class)))
                .thenReturn(matchResponse("cancelled", 0, 0, null));
        when(matchManagementService.finishMatch(MATCH_ID)).thenReturn(matchResponse("finished", 2, 1, TEAM_A_ID));
        when(matchManagementService.submitResult(eq(MATCH_ID), any(SubmitMatchResultRequest.class)))
                .thenReturn(matchResponse("finished", 2, 1, TEAM_A_ID));

        mockMvc.perform(post("/api/organizer/matches/" + MATCH_ID + "/start")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("live"));

        mockMvc.perform(post("/api/organizer/matches/" + MATCH_ID + "/cancel")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "Team did not show up."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("cancelled"));

        mockMvc.perform(post("/api/organizer/matches/" + MATCH_ID + "/finish")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("finished"));

        mockMvc.perform(patch("/api/organizer/matches/" + MATCH_ID + "/result")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scoreA": 2,
                                  "scoreB": 1,
                                  "winnerTeamId": "%s"
                                }
                                """.formatted(TEAM_A_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("finished"))
                .andExpect(jsonPath("$.data.scoreA").value(2))
                .andExpect(jsonPath("$.data.winnerTeamId").value(TEAM_A_ID.toString()));
    }

    @Test
    void normalUserCannotSubmitResult() throws Exception {
        when(authenticatedProfileRepository.findByAuthUserId(AUTH_USER_ID))
                .thenReturn(Optional.of(authenticatedProfile(ProfileRole.PLAYER)));

        mockMvc.perform(patch("/api/organizer/matches/" + MATCH_ID + "/result")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scoreA": 1,
                                  "scoreB": 0,
                                  "winnerTeamId": "%s"
                                }
                                """.formatted(TEAM_A_ID)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private static String bearerToken() throws Exception {
        return "Bearer " + SupabaseJwtTestSupport.token(AUTH_USER_ID, Instant.now());
    }

    private static AuthenticatedProfile authenticatedProfile(ProfileRole role) {
        return new AuthenticatedProfile(
                PROFILE_ID,
                AUTH_USER_ID,
                "Organizer",
                role);
    }

    private static MatchResponse matchResponse(String status, int scoreA, int scoreB, UUID winnerTeamId) {
        return new MatchResponse(
                MATCH_ID,
                TOURNAMENT_ID,
                null,
                status,
                TEAM_A_ID,
                "Team A",
                TEAM_B_ID,
                "Team B",
                scoreA,
                scoreB,
                winnerTeamId,
                TEAM_A_ID.equals(winnerTeamId) ? "Team A" : TEAM_B_ID.equals(winnerTeamId) ? "Team B" : null,
                3,
                SCHEDULED_AT,
                "live".equals(status) || "finished".equals(status) ? NOW : null,
                "finished".equals(status) ? NOW.plusHours(1) : null,
                "cancelled".equals(status) ? NOW.plusMinutes(15) : null,
                "cancelled".equals(status) ? "Team did not show up." : null,
                1,
                1,
                "Playoffs",
                "Final",
                NOW.minusDays(1),
                NOW);
    }

    @TestConfiguration
    static class MatchManagementControllerTestConfig {

        @Bean
        @Primary
        MatchManagementService matchManagementService() {
            return Mockito.mock(MatchManagementService.class);
        }

        @Bean
        @Primary
        AuthenticatedProfileRepository authenticatedProfileRepository() {
            return Mockito.mock(AuthenticatedProfileRepository.class);
        }
    }
}
