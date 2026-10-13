package si.um.feri.dotaops.backend.opendota.domain;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MatchImportStatus {
    QUEUED("queued"),
    PROCESSING("processing"),
    READY("ready"),
    ERROR("error");

    private final String databaseValue;

    MatchImportStatus(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    @JsonValue
    public String databaseValue() {
        return databaseValue;
    }

    @JsonCreator
    public static MatchImportStatus fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Match import status is required.");
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (MatchImportStatus status : values()) {
            if (status.databaseValue.equals(normalized)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Unknown match import status: " + value);
    }
}
