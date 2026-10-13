package si.um.feri.dotaops.backend.team.repository;

import java.util.UUID;

public record CreateTeamCommand(
        String name,
        String tag,
        String slug,
        UUID captainProfileId,
        String region,
        String logoUrl,
        String description,
        UUID createdBy
) {
}
