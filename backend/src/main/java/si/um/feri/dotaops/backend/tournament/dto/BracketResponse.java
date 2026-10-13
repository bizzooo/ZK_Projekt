package si.um.feri.dotaops.backend.tournament.dto;

import java.util.List;
import java.util.UUID;

import si.um.feri.dotaops.backend.tournament.domain.BracketMatch;

public record BracketResponse(
        UUID tournamentId,
        String stageName,
        String bracketType,
        int bracketSize,
        List<BracketMatchResponse> matches
) {

    public static BracketResponse from(
            UUID tournamentId,
            String stageName,
            String bracketType,
            int bracketSize,
            List<BracketMatch> matches
    ) {
        return new BracketResponse(
                tournamentId,
                stageName,
                bracketType,
                bracketSize,
                matches.stream()
                        .map(BracketMatchResponse::from)
                        .toList());
    }
}
