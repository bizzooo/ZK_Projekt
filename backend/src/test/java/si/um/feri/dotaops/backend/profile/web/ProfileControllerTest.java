package si.um.feri.dotaops.backend.profile.web;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import si.um.feri.dotaops.backend.auth.steam.domain.SteamAuthResult;
import si.um.feri.dotaops.backend.auth.steam.service.SteamSessionTokenService;
import si.um.feri.dotaops.backend.common.error.ResourceNotFoundException;
import si.um.feri.dotaops.backend.common.pagination.PageMeta;
import si.um.feri.dotaops.backend.common.pagination.PageResponse;
import si.um.feri.dotaops.backend.profile.service.ProfileMutationResult;
import si.um.feri.dotaops.backend.profile.service.ProfileService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {
        BackendApplication.class,
        ProfileControllerTest.ProfileControllerTestConfig.class
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
class ProfileControllerTest {

    private static final UUID AUTH_USER_ID = UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final UUID PROFILE_ID = UUID.fromString("44444444-4444-4444-8444-444444444444");
    private static final UUID STEAM_PROFILE_ID = UUID.fromString("55555555-5555-4555-8555-555555555555");
    private static final String STEAM_ID = "76561198000000001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private AuthenticatedProfileRepository authenticatedProfileRepository;

    @Autowired
    private SteamSessionTokenService steamSessionTokenService;

    @BeforeEach
    void setUp() {
        Mockito.reset(profileService, authenticatedProfileRepository);
        when(authenticatedProfileRepository.findByAuthUserId(AUTH_USER_ID))
                .thenReturn(Optional.of(authenticatedProfile()));
        when(authenticatedProfileRepository.findByProfileId(STEAM_PROFILE_ID))
                .thenReturn(Optional.of(steamAuthenticatedProfile()));
    }

    @Test
    void listProfilesWorksWithoutJwt() throws Exception {
        when(profileService.listProfiles(isNull(), eq(0), eq(20))).thenReturn(new PageResponse<>(
                java.util.List.of(profileResponse()),
                new PageMeta(0, 20, 1, 1, false, false)));

        mockMvc.perform(get("/api/profiles"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.items[0].id").value(PROFILE_ID.toString()))
                .andExpect(jsonPath("$.data.items[0].nickname").value("MidPulse"))
                .andExpect(jsonPath("$.data.items[0].steamId64").value("76561190000000001"))
                .andExpect(jsonPath("$.data.items[0].opendotaAccountId").value(39734273))
                .andExpect(jsonPath("$.data.items[0].role").value("player"))
                .andExpect(jsonPath("$.data.page.totalElements").value(1));
    }

    @Test
    void getCurrentProfileRequiresJwt() throws Exception {
        mockMvc.perform(get("/api/me/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void getCurrentProfileUsesAuthenticatedUser() throws Exception {
        when(profileService.getCurrentProfile()).thenReturn(profileResponse());

        mockMvc.perform(get("/api/me/profile")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(PROFILE_ID.toString()))
                .andExpect(jsonPath("$.data.nickname").value("MidPulse"))
                .andExpect(jsonPath("$.data.steamId64").value("76561190000000001"))
                .andExpect(jsonPath("$.data.opendotaAccountId").value(39734273));
    }

    @Test
    void getCurrentProfileAcceptsSteamSessionCookie() throws Exception {
        when(profileService.getCurrentProfile()).thenReturn(profileResponse());

        mockMvc.perform(get("/api/me/profile")
                        .cookie(steamSessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(PROFILE_ID.toString()))
                .andExpect(jsonPath("$.data.nickname").value("MidPulse"))
                .andExpect(jsonPath("$.data.steamId64").value("76561190000000001"))
                .andExpect(jsonPath("$.data.opendotaAccountId").value(39734273));
    }

    @Test
    void getProfileByNicknameWorksWithoutJwt() throws Exception {
        when(profileService.getProfileByNickname("MidPulse")).thenReturn(profileResponse());

        mockMvc.perform(get("/api/profiles/by-nickname/MidPulse"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(PROFILE_ID.toString()))
                .andExpect(jsonPath("$.data.nickname").value("MidPulse"))
                .andExpect(jsonPath("$.data.steamId64").value("76561190000000001"));
    }

    @Test
    void getProfileByNicknameReturnsNotFound() throws Exception {
        when(profileService.getProfileByNickname("missing"))
                .thenThrow(new ResourceNotFoundException("Profile", "nickname", "missing"));

        mockMvc.perform(get("/api/profiles/by-nickname/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void createCurrentProfileReturnsCreatedProfile() throws Exception {
        when(profileService.createCurrentProfile(any(CreateProfileRequest.class)))
                .thenReturn(new ProfileMutationResult(profileResponse(), true));

        mockMvc.perform(post("/api/me/profile")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "MidPulse",
                                  "displayName": "Mid Pulse",
                                  "countryCode": "si"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/profiles/" + PROFILE_ID))
                .andExpect(jsonPath("$.data.nickname").value("MidPulse"));
    }

    @Test
    void createCurrentProfileReturnsOkWhenProfileAlreadyExists() throws Exception {
        when(profileService.createCurrentProfile(any(CreateProfileRequest.class)))
                .thenReturn(new ProfileMutationResult(profileResponse(), false));

        mockMvc.perform(post("/api/me/profile")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "MidPulse",
                                  "display_name": "Mid Pulse",
                                  "country_code": "si",
                                  "desired_role": "organizer"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("MidPulse"));
    }

    @Test
    void invalidCreatePayloadReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/me/profile")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "x",
                                  "countryCode": "svn"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void updateCurrentProfileReturnsUpdatedProfile() throws Exception {
        when(profileService.updateCurrentProfile(any(UpdateProfileRequest.class))).thenReturn(profileResponse());

        mockMvc.perform(patch("/api/me/profile")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Mid Pulse"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("Mid Pulse"));
    }

    @Test
    void updateCurrentProfileAcceptsSnakeCaseFields() throws Exception {
        when(profileService.updateCurrentProfile(any(UpdateProfileRequest.class))).thenReturn(profileResponse());

        mockMvc.perform(patch("/api/me/profile")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "display_name": "Mid Pulse",
                                  "country_code": "si"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("Mid Pulse"));

        ArgumentCaptor<UpdateProfileRequest> captor = ArgumentCaptor.forClass(UpdateProfileRequest.class);
        verify(profileService).updateCurrentProfile(captor.capture());

        assertThat(captor.getValue().hasDisplayName()).isTrue();
        assertThat(captor.getValue().displayName()).isEqualTo("Mid Pulse");
        assertThat(captor.getValue().hasCountryCode()).isTrue();
        assertThat(captor.getValue().countryCode()).isEqualTo("si");
    }

    @Test
    void updateCurrentProfileAcceptsSteamSessionCookie() throws Exception {
        when(profileService.updateCurrentProfile(any(UpdateProfileRequest.class))).thenReturn(profileResponse());

        mockMvc.perform(patch("/api/me/profile")
                        .cookie(steamSessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Steam Player"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(PROFILE_ID.toString()));
    }

    @Test
    void updateCurrentProfileRequiresAuthentication() throws Exception {
        mockMvc.perform(patch("/api/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Mid Pulse"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void uploadCurrentAvatarReturnsPersistedAvatarUrl() throws Exception {
        when(profileService.updateCurrentAvatar(any())).thenReturn(new AvatarUploadResponse(
                "https://project.supabase.co/storage/v1/object/public/dotaops-images/profiles/"
                        + PROFILE_ID + "/avatars/avatar.png",
                "Avatar uploaded successfully.",
                true));

        mockMvc.perform(multipart("/api/me/avatar")
                        .file("avatar", new byte[] {1, 2, 3})
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avatarUrl").value(
                        "https://project.supabase.co/storage/v1/object/public/dotaops-images/profiles/"
                                + PROFILE_ID + "/avatars/avatar.png"))
                .andExpect(jsonPath("$.data.persisted").value(true));
    }

    @Test
    void uploadCurrentAvatarRequiresAuthentication() throws Exception {
        mockMvc.perform(multipart("/api/me/avatar")
                        .file("avatar", new byte[] {1, 2, 3})
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void syncCurrentOpenDotaProfileReturnsUpdatedProfile() throws Exception {
        when(profileService.syncCurrentOpenDotaProfile()).thenReturn(profileResponse());

        mockMvc.perform(post("/api/me/profile/opendota/sync")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(PROFILE_ID.toString()))
                .andExpect(jsonPath("$.data.opendotaAccountId").value(39734273));
    }

    private static String bearerToken() throws Exception {
        return "Bearer " + SupabaseJwtTestSupport.token(AUTH_USER_ID, Instant.now());
    }

    private Cookie steamSessionCookie() {
        return new Cookie("dotaops_steam_session", steamSessionTokenService.createToken(new SteamAuthResult(
                STEAM_ID,
                STEAM_PROFILE_ID,
                UUID.fromString("66666666-6666-4666-8666-666666666666"),
                false,
                false,
                "Steam Player",
                null,
                "https://steamcommunity.com/profiles/" + STEAM_ID + "/",
                URI.create("http://localhost:3000/auth/steam/callback"))));
    }

    private static AuthenticatedProfile authenticatedProfile() {
        return new AuthenticatedProfile(
                UUID.nameUUIDFromBytes(AUTH_USER_ID.toString().getBytes(StandardCharsets.UTF_8)),
                AUTH_USER_ID,
                "MidPulse",
                ProfileRole.PLAYER);
    }

    private static AuthenticatedProfile steamAuthenticatedProfile() {
        return new AuthenticatedProfile(
                STEAM_PROFILE_ID,
                null,
                "steam_player",
                ProfileRole.PLAYER);
    }

    private static ProfileResponse profileResponse() {
        OffsetDateTime now = OffsetDateTime.parse("2026-05-12T00:00:00Z");

        return new ProfileResponse(
                PROFILE_ID,
                "MidPulse",
                "Mid Pulse",
                "76561190000000001",
                "76561190000000001",
                39734273L,
                "player",
                "https://cdn.example.test/avatar.png",
                "Position two player",
                "SI",
                now,
                now,
                now,
                now);
    }

    @TestConfiguration
    static class ProfileControllerTestConfig {

        @Bean
        @Primary
        ProfileService profileService() {
            return Mockito.mock(ProfileService.class);
        }

        @Bean
        @Primary
        AuthenticatedProfileRepository authenticatedProfileRepository() {
            return Mockito.mock(AuthenticatedProfileRepository.class);
        }
    }
}
