package si.um.feri.dotaops.backend.profile.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import si.um.feri.dotaops.backend.auth.steam.domain.SteamPlayerSummary;
import si.um.feri.dotaops.backend.opendota.domain.OpenDotaPlayerProfile;
import si.um.feri.dotaops.backend.opendota.service.DotaAccountIdConverter;
import si.um.feri.dotaops.backend.opendota.service.OpenDotaClient;
import si.um.feri.dotaops.backend.profile.repository.ProfileBootstrapRepository;

@Service
public class SteamProfileBootstrapService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SteamProfileBootstrapService.class);

    private final ProfileBootstrapRepository profileBootstrapRepository;
    private final OpenDotaClient openDotaClient;

    public SteamProfileBootstrapService(
            ProfileBootstrapRepository profileBootstrapRepository,
            OpenDotaClient openDotaClient
    ) {
        this.profileBootstrapRepository = profileBootstrapRepository;
        this.openDotaClient = openDotaClient;
    }

    @Async("profileBootstrapTaskExecutor")
    public void bootstrapAfterSteamLogin(UUID profileId, String steamId64, SteamPlayerSummary summary) {
        try {
            long opendotaAccountId = DotaAccountIdConverter.steamId64ToAccountId32(steamId64);
            profileBootstrapRepository.markSteamProfileSynced(profileId, opendotaAccountId, summary);

            openDotaClient.fetchPlayer(opendotaAccountId)
                    .ifPresentOrElse(
                            playerProfile -> syncOpenDotaProfile(profileId, playerProfile),
                            () -> profileBootstrapRepository.markOpenDotaProfileSyncFailed(
                                    profileId,
                                    opendotaAccountId));
        } catch (Exception exception) {
            LOGGER.warn("Steam/OpenDota profile bootstrap failed for profile {}.", profileId, exception);
        }
    }

    private void syncOpenDotaProfile(UUID profileId, OpenDotaPlayerProfile playerProfile) {
        profileBootstrapRepository.markOpenDotaProfileSynced(profileId, playerProfile);
    }
}
