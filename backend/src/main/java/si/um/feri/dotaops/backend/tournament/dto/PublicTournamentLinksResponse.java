package si.um.feri.dotaops.backend.tournament.dto;

import java.util.UUID;

public record PublicTournamentLinksResponse(
        String bracket,
        String standings,
        String matches,
        String groups,
        String metrics
) {

    public static PublicTournamentLinksResponse forTournament(UUID tournamentId) {
        String base = "/api/public/tournaments/" + tournamentId;
        return new PublicTournamentLinksResponse(
                base + "/bracket",
                base + "/standings",
                base + "/matches",
                base + "/groups",
                base + "/metrics");
    }
}
