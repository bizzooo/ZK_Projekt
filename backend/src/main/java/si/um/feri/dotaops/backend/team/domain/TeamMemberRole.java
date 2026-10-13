package si.um.feri.dotaops.backend.team.domain;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TeamMemberRole {
    CARRY("carry"),
    MID("mid"),
    OFFLANE("offlane"),
    SUPPORT("support"),
    ROAMER("roamer"),
    COACH("coach"),
    SUBSTITUTE("substitute");

    private final String databaseValue;

    TeamMemberRole(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    @JsonValue
    public String databaseValue() {
        return databaseValue;
    }

    @JsonCreator
    public static TeamMemberRole fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Team member role is required.");
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (TeamMemberRole role : values()) {
            if (role.databaseValue.equals(normalized)) {
                return role;
            }
        }

        throw new IllegalArgumentException("Unknown team member role: " + value);
    }
}
