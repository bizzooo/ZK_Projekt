package si.um.feri.dotaops.backend.storage.service;

public record StoredImage(
        String path,
        String publicUrl,
        String contentType
) {
}
