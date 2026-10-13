package si.um.feri.dotaops.backend.profile.service;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Async;

import si.um.feri.dotaops.backend.auth.steam.domain.SteamPlayerSummary;
import si.um.feri.dotaops.backend.opendota.domain.OpenDotaPlayerProfile;
import si.um.feri.dotaops.backend.opendota.service.OpenDotaClient;
import si.um.feri.dotaops.backend.profile.repository.ProfileBootstrapRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class SteamProfileBootstrapServiceTest {

    private static final UUID PROFILE_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final String STEAM_ID = "76561198000000001";
    private static final long OPENDOTA_ACCOUNT_ID = 39734273L;
    private static final SteamPlayerSummary STEAM_SUMMARY = new SteamPlayerSummary(
            STEAM_ID,
            "Dota Player",
            "https://cdn.example.test/avatar.png",
            "https://steamcommunity.com/profiles/" + STEAM_ID + "/");
    private static final OpenDotaPlayerProfile OPENDOTA_PROFILE = new OpenDotaPlayerProfile(
            OPENDOTA_ACCOUNT_ID,
            "Dota Player",
            "https://cdn.example.test/avatar.png",
            "https://steamcommunity.com/profiles/" + STEAM_ID + "/");

    private final ProfileBootstrapRepository profileBootstrapRepository = mock(ProfileBootstrapRepository.class);
    private final OpenDotaClient openDotaClient = mock(OpenDotaClient.class);
    private final SteamProfileBootstrapService service = new SteamProfileBootstrapService(
            profileBootstrapRepository,
            openDotaClient);

    @Test
    void bootstrapAfterSteamLoginIsScheduledAsynchronouslyBySpring() throws Exception {
        Async async = SteamProfileBootstrapService.class
                .getMethod("bootstrapAfterSteamLogin", UUID.class, String.class, SteamPlayerSummary.class)
                .getAnnotation(Async.class);

        assertThat(async).isNotNull();
        assertThat(async.value()).isEqualTo("profileBootstrapTaskExecutor");
    }

    @Test
    void bootstrapStoresSteamAndOpenDotaProfileData() {
        when(openDotaClient.fetchPlayer(OPENDOTA_ACCOUNT_ID)).thenReturn(Optional.of(OPENDOTA_PROFILE));

        service.bootstrapAfterSteamLogin(PROFILE_ID, STEAM_ID, STEAM_SUMMARY);

        verify(profileBootstrapRepository).markSteamProfileSynced(
                PROFILE_ID,
                OPENDOTA_ACCOUNT_ID,
                STEAM_SUMMARY);
        verify(openDotaClient).fetchPlayer(OPENDOTA_ACCOUNT_ID);
        verify(profileBootstrapRepository).markOpenDotaProfileSynced(PROFILE_ID, OPENDOTA_PROFILE);
    }

    @Test
    void bootstrapRecordsOpenDotaFailureWithoutThrowing() {
        when(openDotaClient.fetchPlayer(OPENDOTA_ACCOUNT_ID)).thenReturn(Optional.empty());

        service.bootstrapAfterSteamLogin(PROFILE_ID, STEAM_ID, STEAM_SUMMARY);

        verify(profileBootstrapRepository).markSteamProfileSynced(
                PROFILE_ID,
                OPENDOTA_ACCOUNT_ID,
                STEAM_SUMMARY);
        verify(profileBootstrapRepository).markOpenDotaProfileSyncFailed(PROFILE_ID, OPENDOTA_ACCOUNT_ID);
    }

    @Test
    void bootstrapCanRunRepeatedlyForSameProfileAndSteamAccount() {
        when(openDotaClient.fetchPlayer(OPENDOTA_ACCOUNT_ID)).thenReturn(Optional.of(OPENDOTA_PROFILE));

        service.bootstrapAfterSteamLogin(PROFILE_ID, STEAM_ID, STEAM_SUMMARY);
        service.bootstrapAfterSteamLogin(PROFILE_ID, STEAM_ID, STEAM_SUMMARY);

        verify(profileBootstrapRepository, times(2)).markSteamProfileSynced(
                PROFILE_ID,
                OPENDOTA_ACCOUNT_ID,
                STEAM_SUMMARY);
        verify(profileBootstrapRepository, times(2)).markOpenDotaProfileSynced(PROFILE_ID, OPENDOTA_PROFILE);
        verifyNoMoreInteractions(profileBootstrapRepository);
    }
}
