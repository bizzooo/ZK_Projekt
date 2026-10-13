package si.um.feri.dotaops.backend.tournament.web;

import java.time.Instant;
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
import si.um.feri.dotaops.backend.tournament.dto.BracketMatchResponse;
import si.um.feri.dotaops.backend.tournament.dto.BracketResponse;
import si.um.feri.dotaops.backend.tournament.dto.GenerateBracketRequest;
import si.um.feri.dotaops.backend.tournament.dto.MatchSlotResponse;
import si.um.feri.dotaops.backend.tournament.service.TournamentBracketService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {
        BackendApplication.class,
        TournamentBracketControllerTest.TournamentBracketControllerTestConfig.class
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
class TournamentBracketControllerTest {

    private static final UUID AUTH_USER_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID PROFILE_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID TOURNAMENT_ID = UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final UUID MATCH_ID = UUID.fromString("44444444-4444-4444-8444-444444444444");
    private static final UUID TEAM_ID = UUID.fromString("55555555-5555-4555-8555-555555555555");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TournamentBracketService bracketService;

    @Autowired
    private AuthenticatedProfileRepository authenticatedProfileRepository;

    @BeforeEach
    void setUp() {
        Mockito.reset(bracketService, authenticatedProfileRepository);
        when(authenticatedProfileRepository.findByAuthUserId(AUTH_USER_ID))
                .thenReturn(Optional.of(authenticatedProfile()));
    }

    @Test
    void generateBracketRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/organizer/tournaments/" + TOURNAMENT_ID + "/bracket/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void organizerCanGenerateBracket() throws Exception {
        when(bracketService.generateBracket(eq(TOURNAMENT_ID), any(GenerateBracketRequest.class)))
                .thenReturn(bracketResponse());

        mockMvc.perform(post("/api/organizer/tournaments/" + TOURNAMENT_ID + "/bracket/generate")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stageName": "Playoffs",
                                  "forceRegenerate": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stageName").value("Playoffs"))
                .andExpect(jsonPath("$.data.bracketType").value("single_elimination"))
                .andExpect(jsonPath("$.data.matches[0].roundName").value("Final"))
                .andExpect(jsonPath("$.data.matches[0].slots[0].sourceType").value("seed"));
    }

    @Test
    void publicReadBracketDoesNotRequireAuthentication() throws Exception {
        when(bracketService.getPublicBracket(TOURNAMENT_ID, "Playoffs")).thenReturn(bracketResponse());

        mockMvc.perform(get("/api/tournaments/" + TOURNAMENT_ID + "/bracket")
                        .queryParam("stageName", "Playoffs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matches[0].slots[0].teamId").value(TEAM_ID.toString()));
    }

    private static String bearerToken() throws Exception {
        return "Bearer " + SupabaseJwtTestSupport.token(AUTH_USER_ID, Instant.now());
    }

    private static AuthenticatedProfile authenticatedProfile() {
        return new AuthenticatedProfile(
                PROFILE_ID,
                AUTH_USER_ID,
                "Organizer",
                ProfileRole.ORGANIZER);
    }

    private static BracketResponse bracketResponse() {
        return new BracketResponse(
                TOURNAMENT_ID,
                "Playoffs",
                "single_elimination",
                2,
                List.of(new BracketMatchResponse(
                        MATCH_ID,
                        TOURNAMENT_ID,
                        null,
                        1,
                        1,
                        "Playoffs",
                        "Final",
                        "scheduled",
                        TEAM_ID,
                        "Radiant Five",
                        null,
                        null,
                        0,
                        0,
                        null,
                        null,
                        1,
                        null,
                        null,
                        null,
                        null,
                        null,
                        List.of(new MatchSlotResponse(
                                1,
                                "seed",
                                TEAM_ID,
                                "Radiant Five",
                                1,
                                null,
                                false)))));
    }

    @TestConfiguration
    static class TournamentBracketControllerTestConfig {

        @Bean
        @Primary
        TournamentBracketService bracketService() {
            return Mockito.mock(TournamentBracketService.class);
        }

        @Bean
        @Primary
        AuthenticatedProfileRepository authenticatedProfileRepository() {
            return Mockito.mock(AuthenticatedProfileRepository.class);
        }
    }
}
