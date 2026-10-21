package si.um.feri.dotaops.backend.auth.web;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.ServletException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.mock.web.MockFilterChain;

import si.um.feri.dotaops.backend.auth.domain.AuthenticatedProfile;
import si.um.feri.dotaops.backend.auth.domain.ProfileRole;
import si.um.feri.dotaops.backend.auth.domain.SupabaseJwtClaims;
import si.um.feri.dotaops.backend.auth.repository.AuthenticatedProfileRepository;
import si.um.feri.dotaops.backend.auth.service.SupabaseJwtVerifier;
import si.um.feri.dotaops.backend.auth.service.SupabasePrincipal;
import si.um.feri.dotaops.backend.auth.steam.domain.SteamSessionClaims;
import si.um.feri.dotaops.backend.auth.steam.service.SteamSessionCookieService;
import si.um.feri.dotaops.backend.auth.steam.service.SteamSessionTokenService;
import si.um.feri.dotaops.backend.config.properties.SteamSessionProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupabaseJwtAuthenticationFilterTest {

    private static final UUID AUTH_USER_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID PROFILE_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final String COOKIE_NAME = "steam-session";

    private final SupabaseJwtVerifier jwtVerifier = mock(SupabaseJwtVerifier.class);
    private final AuthenticatedProfileRepository profileRepository = mock(AuthenticatedProfileRepository.class);
    private final SteamSessionTokenService steamSessionTokenService = mock(SteamSessionTokenService.class);
    private final SteamSessionCookieService steamSessionCookieService = new SteamSessionCookieService(
            new SteamSessionProperties("secret", null, null, null, COOKIE_NAME, "/", null, true, "Lax"));
    private final AuthenticationEntryPoint entryPoint = mock(AuthenticationEntryPoint.class);
    private final SupabaseJwtAuthenticationFilter filter = new SupabaseJwtAuthenticationFilter(
            jwtVerifier,
            profileRepository,
            steamSessionTokenService,
            steamSessionCookieService,
            entryPoint);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesValidBearerTokenAndContinuesChain() throws Exception {
        SupabaseJwtClaims claims = new SupabaseJwtClaims(
                AUTH_USER_ID, "player@example.test", "issuer", List.of("audience"),
                Instant.parse("2026-10-21T00:00:00Z"), Instant.parse("2026-10-20T00:00:00Z"), Map.of());
        AuthenticatedProfile profile = new AuthenticatedProfile(
                PROFILE_ID, AUTH_USER_ID, "Player", ProfileRole.PLAYER);
        when(jwtVerifier.verify("jwt-token")).thenReturn(claims);
        when(profileRepository.findByAuthUserId(AUTH_USER_ID)).thenReturn(Optional.of(profile));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer jwt-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        java.util.concurrent.atomic.AtomicReference<org.springframework.security.core.Authentication> authenticationAtChain =
                new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<jakarta.servlet.ServletRequest> requestAtChain =
                new java.util.concurrent.atomic.AtomicReference<>();
        jakarta.servlet.FilterChain chain = (servletRequest, servletResponse) -> {
            authenticationAtChain.set(SecurityContextHolder.getContext().getAuthentication());
            requestAtChain.set(servletRequest);
        };

        filter.doFilterInternal(request, response, chain);

        assertThat(authenticationAtChain.get().getPrincipal())
                .isInstanceOf(SupabasePrincipal.class);
        assertThat(((SupabasePrincipal) authenticationAtChain.get().getPrincipal())
                .profile()).contains(profile);
        assertThat(authenticationAtChain.get().getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .contains("ROLE_PLAYER", "ROLE_AUTHENTICATED");
        assertThat(authenticationAtChain.get().getDetails()).isNotNull();
        assertThat(requestAtChain.get()).isSameAs(request);
        verify(entryPoint, never()).commence(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsAuthorizationHeaderWithoutBearerScheme() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic credentials");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(chain.getRequest()).isNull();
        verify(entryPoint).commence(
                org.mockito.ArgumentMatchers.same(request),
                org.mockito.ArgumentMatchers.same(response),
                org.mockito.ArgumentMatchers.any());
        verify(jwtVerifier, never()).verify(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsInvalidBearerTokenAndClearsExistingAuthentication() throws Exception {
        when(jwtVerifier.verify("invalid-token"))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Invalid token."));
        SecurityContextHolder.getContext().setAuthentication(
                org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                        .authenticated("previous-user", null, List.of()));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNull();
        verify(entryPoint).commence(
                org.mockito.ArgumentMatchers.same(request),
                org.mockito.ArgumentMatchers.same(response),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void authenticatesValidSteamCookieWhenBearerHeaderIsAbsent() throws Exception {
        AuthenticatedProfile profile = new AuthenticatedProfile(
                PROFILE_ID, AUTH_USER_ID, "Player", ProfileRole.PLAYER);
        SteamSessionClaims claims = new SteamSessionClaims(
                PROFILE_ID, "76561198000000001", Instant.EPOCH, Instant.EPOCH.plusSeconds(60));
        when(steamSessionTokenService.verify("steam-token")).thenReturn(claims);
        when(profileRepository.findByProfileId(PROFILE_ID)).thenReturn(Optional.of(profile));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie(COOKIE_NAME, "steam-token"));
        java.util.concurrent.atomic.AtomicReference<org.springframework.security.core.Authentication> authenticationAtChain =
                new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<jakarta.servlet.ServletRequest> requestAtChain =
                new java.util.concurrent.atomic.AtomicReference<>();
        jakarta.servlet.FilterChain chain = (servletRequest, servletResponse) -> {
            authenticationAtChain.set(SecurityContextHolder.getContext().getAuthentication());
            requestAtChain.set(servletRequest);
        };

        filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

        assertThat(authenticationAtChain.get().getPrincipal())
                .isInstanceOf(SupabasePrincipal.class);
        assertThat(((SupabasePrincipal) authenticationAtChain.get().getPrincipal())
                .steamId()).isEqualTo("76561198000000001");
        assertThat(authenticationAtChain.get().getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .contains("ROLE_PLAYER", "ROLE_AUTHENTICATED");
        assertThat(authenticationAtChain.get().getDetails()).isNotNull();
        assertThat(requestAtChain.get()).isSameAs(request);
    }

    @Test
    void clearsInvalidSteamCookieAndContinuesWithoutAuthentication() throws Exception {
        when(steamSessionTokenService.verify("expired-token"))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("expired"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie(COOKIE_NAME, "expired-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).contains("steam-session=;");
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void continuesUnauthenticatedWhenNoCredentialsAreProvided() throws ServletException, java.io.IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isSameAs(request);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtVerifier, never()).verify(org.mockito.ArgumentMatchers.any());
        verify(steamSessionTokenService, never()).verify(org.mockito.ArgumentMatchers.any());
        verify(entryPoint, never()).commence(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }
}
