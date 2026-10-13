package si.um.feri.dotaops.backend.integration;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import si.um.feri.dotaops.backend.BackendApplication;
import si.um.feri.dotaops.backend.auth.service.SupabaseJwtTestSupport;
import si.um.feri.dotaops.backend.auth.service.SupabasePrincipal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {
        BackendApplication.class,
        ApiFlowIntegrationTest.IntegrationSecurityController.class
})
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@EnabledIfEnvironmentVariable(named = "SUPABASE_DB_URL", matches = ".+")
@TestPropertySource(properties = {
        "dotaops.supabase.auth.jwt-secret=" + SupabaseJwtTestSupport.SECRET,
        "dotaops.supabase.auth.issuer=" + SupabaseJwtTestSupport.ISSUER,
        "dotaops.supabase.auth.audience=" + SupabaseJwtTestSupport.AUDIENCE
})
class ApiFlowIntegrationTest extends PostgresIntegrationTestSupport {

    private static final UUID PLAYER_AUTH_USER_ID = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1");
    private static final UUID ORGANIZER_AUTH_USER_ID = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb1");

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void seedProfiles() {
        upsertProfile(PLAYER_AUTH_USER_ID, "player");
        upsertProfile(ORGANIZER_AUTH_USER_ID, "organizer");
    }

    @Test
    void publicHealthEndpointWorksWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void protectedEndpointRequiresJwt() throws Exception {
        mockMvc.perform(get("/api/me/integration-security-test"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void validJwtLoadsProfileFromMigratedDatabase() throws Exception {
        mockMvc.perform(get("/api/me/integration-security-test")
                        .header("Authorization", bearerToken(PLAYER_AUTH_USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authUserId").value(PLAYER_AUTH_USER_ID.toString()))
                .andExpect(jsonPath("$.role").value("PLAYER"))
                .andExpect(jsonPath("$.hasProfile").value(true));
    }

    @Test
    void playerCannotUseOrganizerEndpoint() throws Exception {
        mockMvc.perform(get("/api/organizer/integration-security-test")
                        .header("Authorization", bearerToken(PLAYER_AUTH_USER_ID)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void organizerCanUseOrganizerEndpoint() throws Exception {
        mockMvc.perform(get("/api/organizer/integration-security-test")
                        .header("Authorization", bearerToken(ORGANIZER_AUTH_USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("organizer"));
    }

    private static String bearerToken(UUID authUserId) throws Exception {
        return "Bearer " + SupabaseJwtTestSupport.token(authUserId, Instant.now());
    }

    @RestController
    static class IntegrationSecurityController {

        @GetMapping("/api/me/integration-security-test")
        Map<String, Object> me(Authentication authentication) {
            SupabasePrincipal principal = (SupabasePrincipal) authentication.getPrincipal();

            return Map.of(
                    "authUserId", principal.authUserId().toString(),
                    "role", principal.role().name(),
                    "hasProfile", principal.profile().isPresent());
        }

        @GetMapping("/api/organizer/integration-security-test")
        Map<String, String> organizer() {
            return Map.of("result", "organizer");
        }
    }
}
