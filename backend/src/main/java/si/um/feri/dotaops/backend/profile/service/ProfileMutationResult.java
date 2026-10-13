package si.um.feri.dotaops.backend.profile.service;

import si.um.feri.dotaops.backend.profile.web.ProfileResponse;

public record ProfileMutationResult(
        ProfileResponse profile,
        boolean created
) {
}
