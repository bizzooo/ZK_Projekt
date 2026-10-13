package si.um.feri.dotaops.backend.opendota.domain;

public record OpenDotaMatchSummary(
        long matchId,
        Long startTime,
        Integer heroId,
        Integer playerSlot,
        Boolean radiantWin
) {
}
