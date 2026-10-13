package si.um.feri.dotaops.backend.opendota.domain;

public record OpenDotaPlayerProfile(
        long accountId,
        String personaName,
        String avatarUrl,
        String profileUrl
) {
}
