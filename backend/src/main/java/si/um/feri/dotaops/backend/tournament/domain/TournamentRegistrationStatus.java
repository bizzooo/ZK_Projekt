package si.um.feri.dotaops.backend.tournament.domain;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TournamentRegistrationStatus {
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected"),
    WAITLISTED("waitlisted");

    private final String databaseValue;

    TournamentRegistrationStatus(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    @JsonValue
    public String databaseValue() {
        return databaseValue;
    }

    @JsonCreator
    public static TournamentRegistrationStatus fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Tournament registration status is required.");
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (TournamentRegistrationStatus status : values()) {
            if (status.databaseValue.equals(normalized)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Unknown tournament registration status: " + value);
    }
}
