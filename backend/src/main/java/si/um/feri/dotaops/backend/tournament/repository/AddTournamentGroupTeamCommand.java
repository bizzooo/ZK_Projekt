package si.um.feri.dotaops.backend.tournament.repository;

import java.util.UUID;

public record AddTournamentGroupTeamCommand(
        UUID groupId,
        UUID teamId,
        UUID registrationId,
        Integer seedNumber
) {
}
