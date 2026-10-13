package si.um.feri.dotaops.backend.tournament.dto;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import si.um.feri.dotaops.backend.tournament.domain.PublicTournamentMatch;

public record PublicBracketResponse(
        UUID tournamentId,
        String stageName,
        String bracketType,
        int bracketSize,
        List<PublicBracketRoundResponse> rounds
) {

    private static final String BRACKET_TYPE = "single_elimination";

    public static PublicBracketResponse from(UUID tournamentId, String stageName, List<PublicTournamentMatch> matches) {
        List<PublicBracketRoundResponse> rounds = matches.stream()
                .collect(Collectors.groupingBy(PublicTournamentMatch::roundNumber))
                .entrySet()
                .stream()
                .sorted(Comparator.comparingInt(entry -> entry.getKey()))
                .map(entry -> {
                    List<PublicTournamentMatchResponse> roundMatches = entry.getValue()
                            .stream()
                            .sorted(Comparator
                                    .comparing(PublicTournamentMatch::bracketPosition, Comparator.nullsLast(Integer::compareTo))
                                    .thenComparing(PublicTournamentMatch::id))
                            .map(PublicTournamentMatchResponse::from)
                            .toList();
                    String roundName = roundMatches.isEmpty() ? null : roundMatches.getFirst().roundName();

                    return new PublicBracketRoundResponse(entry.getKey(), roundName, roundMatches);
                })
                .toList();

        return new PublicBracketResponse(
                tournamentId,
                stageName,
                BRACKET_TYPE,
                inferBracketSize(matches),
                rounds);
    }

    private static int inferBracketSize(List<PublicTournamentMatch> matches) {
        if (matches.isEmpty()) {
            return 0;
        }

        long firstRoundMatches = matches.stream()
                .filter(match -> match.roundNumber() == 1)
                .count();

        return (int) firstRoundMatches * 2;
    }
}
