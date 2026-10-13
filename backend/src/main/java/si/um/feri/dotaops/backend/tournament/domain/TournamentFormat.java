package si.um.feri.dotaops.backend.tournament.domain;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TournamentFormat {
    SINGLE_ELIMINATION("single_elimination"),
    GROUPS_PLAYOFF("groups_playoff"),
    ROUND_ROBIN("round_robin"),
    BEST_OF_THREE_PLAYOFF("best_of_three_playoff");

    private final String databaseValue;

    TournamentFormat(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    @JsonValue
    public String databaseValue() {
        return databaseValue;
    }

    @JsonCreator
    public static TournamentFormat fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Tournament format is required.");
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (TournamentFormat format : values()) {
            if (format.databaseValue.equals(normalized)) {
                return format;
            }
        }

        throw new IllegalArgumentException("Unknown tournament format: " + value);
    }
}
