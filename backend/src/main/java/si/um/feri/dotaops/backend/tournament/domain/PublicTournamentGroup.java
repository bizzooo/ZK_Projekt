package si.um.feri.dotaops.backend.tournament.domain;

import java.util.List;
import java.util.UUID;

public record PublicTournamentGroup(
        UUID id,
        UUID tournamentId,
        String name,
        int sortOrder,
        List<PublicTournamentTeam> teams
) {
}
