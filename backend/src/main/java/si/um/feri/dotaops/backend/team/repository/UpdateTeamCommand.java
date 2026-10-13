package si.um.feri.dotaops.backend.team.repository;

public record UpdateTeamCommand(
        boolean namePresent,
        String name,
        boolean tagPresent,
        String tag,
        boolean slugPresent,
        String slug,
        boolean regionPresent,
        String region,
        boolean logoUrlPresent,
        String logoUrl,
        boolean descriptionPresent,
        String description
) {
}
