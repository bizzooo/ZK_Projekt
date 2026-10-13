package si.um.feri.dotaops.backend.tournament.domain;

import java.util.Locale;

public enum MatchStatus {
    SCHEDULED("scheduled"),
    READY("ready"),
    LIVE("live"),
    FINISHED("finished"),
    CANCELLED("cancelled");

    private final String databaseValue;

    MatchStatus(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String databaseValue() {
        return databaseValue;
    }

    public boolean isTerminal() {
        return this == FINISHED || this == CANCELLED;
    }

    public static MatchStatus fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Match status is required.");
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (MatchStatus status : values()) {
            if (status.databaseValue.equals(normalized)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Unknown match status: " + value);
    }
}
