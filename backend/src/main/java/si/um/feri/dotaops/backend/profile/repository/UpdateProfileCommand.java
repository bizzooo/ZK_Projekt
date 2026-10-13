package si.um.feri.dotaops.backend.profile.repository;

public record UpdateProfileCommand(
        boolean nicknamePresent,
        String nickname,
        boolean displayNamePresent,
        String displayName,
        boolean avatarUrlPresent,
        String avatarUrl,
        boolean bioPresent,
        String bio,
        boolean countryCodePresent,
        String countryCode
) {
}
