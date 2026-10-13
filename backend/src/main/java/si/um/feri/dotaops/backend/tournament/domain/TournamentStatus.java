package si.um.feri.dotaops.backend.tournament.domain;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TournamentStatus {
    DRAFT("draft"),
    REGISTRATION("registration"),
    PUBLISHED("published"),
    LIVE("live"),
    FINISHED("finished"),
    ARCHIVED("archived");

    private final String databaseValue;

    TournamentStatus(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    @JsonValue
    public String databaseValue() {
        return databaseValue;
    }

    public boolean isPublicVisible() {
        return this != DRAFT && this != ARCHIVED;
    }

    public boolean canPublish() {
        return this == DRAFT || this == REGISTRATION || this == PUBLISHED;
    }

    @JsonCreator
    public static TournamentStatus fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Tournament status is required.");
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (TournamentStatus status : values()) {
            if (status.databaseValue.equals(normalized)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Unknown tournament status: " + value);
    }
}
