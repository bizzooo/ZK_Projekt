package si.um.feri.dotaops.backend.tournament.domain;

import java.util.Locale;

public enum MatchSlotSourceType {
    MANUAL("manual"),
    SEED("seed"),
    WINNER("winner"),
    LOSER("loser"),
    BYE("bye");

    private final String databaseValue;

    MatchSlotSourceType(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String databaseValue() {
        return databaseValue;
    }

    public static MatchSlotSourceType fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Match slot source type is required.");
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (MatchSlotSourceType sourceType : values()) {
            if (sourceType.databaseValue.equals(normalized)) {
                return sourceType;
            }
        }

        throw new IllegalArgumentException("Unknown match slot source type: " + value);
    }
}
