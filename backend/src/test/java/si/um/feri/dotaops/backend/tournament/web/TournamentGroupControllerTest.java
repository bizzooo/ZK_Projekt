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
import si.um.feri.dotaops.backend.tournament.dto.AddTeamToGroupRequest;
import si.um.feri.dotaops.backend.tournament.dto.CreateTournamentGroupRequest;
import si.um.feri.dotaops.backend.tournament.dto.GroupStandingResponse;
import si.um.feri.dotaops.backend.tournament.dto.TournamentGroupResponse;
import si.um.feri.dotaops.backend.tournament.dto.TournamentGroupTeamResponse;
import si.um.feri.dotaops.backend.tournament.service.TournamentGroupService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {
        BackendApplication.class,
        TournamentGroupControllerTest.TournamentGroupControllerTestConfig.class
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
class TournamentGroupControllerTest {

    private static final UUID AUTH_USER_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID PROFILE_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID TOURNAMENT_ID = UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final UUID GROUP_ID = UUID.fromString("44444444-4444-4444-8444-444444444444");
    private static final UUID TEAM_ID = UUID.fromString("55555555-5555-4555-8555-555555555555");
    private static final UUID ASSIGNMENT_ID = UUID.fromString("66666666-6666-4666-8666-666666666666");
    private static final UUID REGISTRATION_ID = UUID.fromString("77777777-7777-4777-8777-777777777777");
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-01T18:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TournamentGroupService groupService;

    @Autowired
    private AuthenticatedProfileRepository authenticatedProfileRepository;

    @BeforeEach
    void setUp() {
        Mockito.reset(groupService, authenticatedProfileRepository);
        when(authenticatedProfileRepository.findByAuthUserId(AUTH_USER_ID))
                .thenReturn(Optional.of(authenticatedProfile()));
    }

    @Test
    void createGroupRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/tournaments/" + TOURNAMENT_ID + "/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Group A"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void createGroupReturnsCreatedGroup() throws Exception {
        when(groupService.createGroup(eq(TOURNAMENT_ID), any(CreateTournamentGroupRequest.class)))
                .thenReturn(groupResponse());

        mockMvc.perform(post("/api/tournaments/" + TOURNAMENT_ID + "/groups")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Group A",
                                  "sortOrder": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/tournaments/" + TOURNAMENT_ID + "/groups/" + GROUP_ID))
                .andExpect(jsonPath("$.data.id").value(GROUP_ID.toString()))
                .andExpect(jsonPath("$.data.name").value("Group A"));
    }

    @Test
    void addTeamReturnsCreatedAssignment() throws Exception {
        when(groupService.addTeam(eq(GROUP_ID), any(AddTeamToGroupRequest.class)))
                .thenReturn(groupTeamResponse());

        mockMvc.perform(post("/api/tournament-groups/" + GROUP_ID + "/teams")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "teamId": "%s",
                                  "seedNumber": 2
                                }
                                """.formatted(TEAM_ID)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/tournament-groups/" + GROUP_ID + "/teams/" + TEAM_ID))
                .andExpect(jsonPath("$.data.teamId").value(TEAM_ID.toString()))
                .andExpect(jsonPath("$.data.seedNumber").value(2));
    }

    @Test
    void publicStandingsDoNotRequireAuthentication() throws Exception {
        when(groupService.getPublicStandings(GROUP_ID)).thenReturn(List.of(standingResponse()));

        mockMvc.perform(get("/api/tournament-groups/" + GROUP_ID + "/standings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].groupName").value("Group A"))
                .andExpect(jsonPath("$.data[0].teamId").value(TEAM_ID.toString()))
                .andExpect(jsonPath("$.data[0].matchWins").value(1))
                .andExpect(jsonPath("$.data[0].gameWins").value(2))
                .andExpect(jsonPath("$.data[0].gameLosses").value(1))
                .andExpect(jsonPath("$.data[0].points").value(3));
    }

    @Test
    void organizerStandingsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/organizer/tournaments/" + TOURNAMENT_ID + "/standings"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void organizerStandingsReturnCalculatedRows() throws Exception {
        when(groupService.listOrganizerStandings(TOURNAMENT_ID)).thenReturn(List.of(standingResponse()));

        mockMvc.perform(get("/api/organizer/tournaments/" + TOURNAMENT_ID + "/standings")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].groupId").value(GROUP_ID.toString()))
                .andExpect(jsonPath("$.data[0].groupName").value("Group A"))
                .andExpect(jsonPath("$.data[0].teamId").value(TEAM_ID.toString()))
                .andExpect(jsonPath("$.data[0].points").value(3));

        verify(groupService).listOrganizerStandings(TOURNAMENT_ID);
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

    private static TournamentGroupResponse groupResponse() {
        return new TournamentGroupResponse(
                GROUP_ID,
                TOURNAMENT_ID,
                "Group A",
                1,
                NOW.minusHours(1),
                NOW);
    }

    private static TournamentGroupTeamResponse groupTeamResponse() {
        return new TournamentGroupTeamResponse(
                ASSIGNMENT_ID,
                GROUP_ID,
                TOURNAMENT_ID,
                TEAM_ID,
                "Radiant Five",
                "R5",
                "radiant-five",
                REGISTRATION_ID,
                2,
                NOW.minusMinutes(10),
                NOW);
    }

    private static GroupStandingResponse standingResponse() {
        return new GroupStandingResponse(
                GROUP_ID,
                "Group A",
                TOURNAMENT_ID,
                TEAM_ID,
                "Radiant Five",
                1,
                1,
                0,
                0,
                2,
                1,
                1,
                3,
                1);
    }

    @TestConfiguration
    static class TournamentGroupControllerTestConfig {

        @Bean
        @Primary
        TournamentGroupService groupService() {
            return Mockito.mock(TournamentGroupService.class);
        }

        @Bean
        @Primary
        AuthenticatedProfileRepository authenticatedProfileRepository() {
            return Mockito.mock(AuthenticatedProfileRepository.class);
        }
    }
}
