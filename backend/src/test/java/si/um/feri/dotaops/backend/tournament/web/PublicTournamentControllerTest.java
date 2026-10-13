package si.um.feri.dotaops.backend.tournament.web;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import si.um.feri.dotaops.backend.BackendApplication;
import si.um.feri.dotaops.backend.auth.repository.AuthenticatedProfileRepository;
import si.um.feri.dotaops.backend.auth.service.SupabaseJwtTestSupport;
import si.um.feri.dotaops.backend.common.pagination.PageMeta;
import si.um.feri.dotaops.backend.common.pagination.PageResponse;
import si.um.feri.dotaops.backend.tournament.domain.TournamentFormat;
import si.um.feri.dotaops.backend.tournament.dto.PublicBracketResponse;
import si.um.feri.dotaops.backend.tournament.dto.PublicBracketRoundResponse;
import si.um.feri.dotaops.backend.tournament.dto.PublicGroupStandingResponse;
import si.um.feri.dotaops.backend.tournament.dto.PublicTeamResponse;
import si.um.feri.dotaops.backend.tournament.dto.PublicTournamentGroupResponse;
import si.um.feri.dotaops.backend.tournament.dto.PublicTournamentLinksResponse;
import si.um.feri.dotaops.backend.tournament.dto.PublicTournamentListItemResponse;
import si.um.feri.dotaops.backend.tournament.dto.PublicTournamentMatchResponse;
import si.um.feri.dotaops.backend.tournament.dto.PublicTournamentMatchSlotResponse;
import si.um.feri.dotaops.backend.tournament.dto.PublicTournamentMetricsResponse;
import si.um.feri.dotaops.backend.tournament.dto.PublicTournamentOverviewResponse;
import si.um.feri.dotaops.backend.tournament.dto.TournamentSettingsDto;
import si.um.feri.dotaops.backend.tournament.service.PublicTournamentService;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {
        BackendApplication.class,
        PublicTournamentControllerTest.PublicTournamentControllerTestConfig.class
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
class PublicTournamentControllerTest {

    private static final UUID TOURNAMENT_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID GROUP_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID MATCH_ID = UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final UUID TEAM_ID = UUID.fromString("44444444-4444-4444-8444-444444444444");
    private static final OffsetDateTime STARTS_AT = OffsetDateTime.parse("2026-06-01T18:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PublicTournamentService publicTournamentService;

    @Autowired
    private AuthenticatedProfileRepository authenticatedProfileRepository;

    @BeforeEach
    void setUp() {
        Mockito.reset(publicTournamentService, authenticatedProfileRepository);
        when(authenticatedProfileRepository.findByAuthUserId(Mockito.any())).thenReturn(Optional.empty());
    }

    @Test
    void publicListWorksWithoutJwt() throws Exception {
        when(publicTournamentService.listPublicTournaments(isNull(), eq(0), eq(20))).thenReturn(new PageResponse<>(
                List.of(listItem()),
                new PageMeta(0, 20, 1, 1, false, false)));

        mockMvc.perform(get("/api/public/tournaments"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.items[0].id").value(TOURNAMENT_ID.toString()))
                .andExpect(jsonPath("$.data.items[0].teamCount").value(4))
                .andExpect(jsonPath("$.data.items[0].finishedMatchCount").value(1));
    }

    @Test
    void publicOverviewWorksWithoutJwt() throws Exception {
        when(publicTournamentService.getOverview(TOURNAMENT_ID)).thenReturn(overview());

        mockMvc.perform(get("/api/public/tournaments/" + TOURNAMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(TOURNAMENT_ID.toString()))
                .andExpect(jsonPath("$.data.teams[0].id").value(TEAM_ID.toString()))
                .andExpect(jsonPath("$.data.groups[0].groupName").value("Group A"))
                .andExpect(jsonPath("$.data.matches[0].winnerTeamId").value(TEAM_ID.toString()))
                .andExpect(jsonPath("$.data.metrics.totalGamesPlayed").value(1))
                .andExpect(jsonPath("$.data.links.bracket").value("/api/public/tournaments/" + TOURNAMENT_ID + "/bracket"));
    }

    @Test
    void publicBracketGroupsMatchesByRound() throws Exception {
        when(publicTournamentService.getBracket(TOURNAMENT_ID, "Playoffs")).thenReturn(bracket());

        mockMvc.perform(get("/api/public/tournaments/" + TOURNAMENT_ID + "/bracket")
                        .queryParam("stageName", "Playoffs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rounds[0].roundName").value("Final"))
                .andExpect(jsonPath("$.data.rounds[0].matches[0].slots[0].sourceType").value("winner"));
    }

    @Test
    void publicMetricsWorksWithoutJwt() throws Exception {
        when(publicTournamentService.getMetrics(TOURNAMENT_ID)).thenReturn(metrics());

        mockMvc.perform(get("/api/public/tournaments/" + TOURNAMENT_ID + "/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approvedTeamCount").value(4))
                .andExpect(jsonPath("$.data.averageGamesPerFinishedMatch").value(1.0));
    }

    private static PublicTournamentListItemResponse listItem() {
        return new PublicTournamentListItemResponse(
                TOURNAMENT_ID,
                "mid-wars-open",
                "Mid Wars Open",
                "published",
                "single_elimination",
                "Dota 2",
                "Public qualifier",
                "TBD",
                STARTS_AT,
                STARTS_AT.plusDays(1),
                STARTS_AT.minusDays(7),
                STARTS_AT.minusDays(1),
                "UTC",
                8,
                4,
                1,
                2,
                1,
                "Organizer",
                STARTS_AT.minusDays(10),
                STARTS_AT.minusDays(20));
    }

    private static PublicTournamentOverviewResponse overview() {
        return new PublicTournamentOverviewResponse(
                TOURNAMENT_ID,
                "mid-wars-open",
                "Mid Wars Open",
                "Public qualifier",
                "Default rules",
                "published",
                "single_elimination",
                "Dota 2",
                STARTS_AT,
                STARTS_AT.plusDays(1),
                STARTS_AT.minusDays(7),
                STARTS_AT.minusDays(1),
                "UTC",
                8,
                "Organizer",
                "TBD",
                STARTS_AT.minusDays(10),
                settings(),
                List.of(team()),
                List.of(group()),
                List.of(match()),
                metrics(),
                PublicTournamentLinksResponse.forTournament(TOURNAMENT_ID));
    }

    private static PublicBracketResponse bracket() {
        return new PublicBracketResponse(
                TOURNAMENT_ID,
                "Playoffs",
                "single_elimination",
                2,
                List.of(new PublicBracketRoundResponse(1, "Final", List.of(match()))));
    }

    private static PublicTournamentGroupResponse group() {
        return new PublicTournamentGroupResponse(
                GROUP_ID,
                TOURNAMENT_ID,
                "Group A",
                1,
                List.of(team()));
    }

    private static PublicTournamentMatchResponse match() {
        return new PublicTournamentMatchResponse(
                MATCH_ID,
                TOURNAMENT_ID,
                GROUP_ID,
                "Group A",
                1,
                "Final",
                1,
                "Playoffs",
                "finished",
                1,
                team(),
                null,
                1,
                0,
                TEAM_ID,
                "Radiant Five",
                team(),
                STARTS_AT,
                STARTS_AT,
                STARTS_AT.plusMinutes(30),
                null,
                null,
                List.of(new PublicTournamentMatchSlotResponse(
                        1,
                        "winner",
                        MATCH_ID,
                        team(),
                        1,
                        false)));
    }

    private static PublicTeamResponse team() {
        return new PublicTeamResponse(
                TEAM_ID,
                "Radiant Five",
                "R5",
                "radiant-five",
                "https://cdn.example.test/r5.png",
                1);
    }

    private static PublicTournamentMetricsResponse metrics() {
        return new PublicTournamentMetricsResponse(
                TOURNAMENT_ID,
                4,
                4,
                1,
                2,
                1,
                0,
                1,
                0,
                1,
                1,
                new BigDecimal("1.00"),
                STARTS_AT.plusHours(2),
                STARTS_AT.plusMinutes(30));
    }

    private static TournamentSettingsDto settings() {
        return new TournamentSettingsDto(
                8,
                2,
                5,
                1,
                TournamentFormat.SINGLE_ELIMINATION,
                false,
                true);
    }

    @TestConfiguration
    static class PublicTournamentControllerTestConfig {

        @Bean
        @Primary
        PublicTournamentService publicTournamentService() {
            return Mockito.mock(PublicTournamentService.class);
        }

        @Bean
        @Primary
        AuthenticatedProfileRepository authenticatedProfileRepository() {
            return Mockito.mock(AuthenticatedProfileRepository.class);
        }
    }
}
