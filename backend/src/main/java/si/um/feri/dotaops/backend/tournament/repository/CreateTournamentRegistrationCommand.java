package si.um.feri.dotaops.backend.tournament.repository;

import java.util.UUID;

public record CreateTournamentRegistrationCommand(
        UUID tournamentId,
        UUID teamId,
        UUID captainProfileId,
        String message,
        String contactEmail
) {
}
