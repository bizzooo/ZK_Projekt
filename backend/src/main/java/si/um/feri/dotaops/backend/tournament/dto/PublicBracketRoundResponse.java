package si.um.feri.dotaops.backend.tournament.dto;

import java.util.List;

public record PublicBracketRoundResponse(
        int roundNumber,
        String roundName,
        List<PublicTournamentMatchResponse> matches
) {
}
