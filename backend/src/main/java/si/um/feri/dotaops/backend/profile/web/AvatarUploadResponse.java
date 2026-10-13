package si.um.feri.dotaops.backend.profile.web;

public record AvatarUploadResponse(
        String avatarUrl,
        String message,
        boolean persisted
) {
}
