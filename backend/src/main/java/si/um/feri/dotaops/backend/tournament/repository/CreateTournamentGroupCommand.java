package si.um.feri.dotaops.backend.tournament.repository;

import java.util.UUID;

public record CreateTournamentGroupCommand(
        UUID tournamentId,
        String name,
        int sortOrder
) {
}
