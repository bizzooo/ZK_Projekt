package si.um.feri.dotaops.backend.tournament.domain;

import java.util.Locale;

public enum MatchSlotName {
    TEAM_A("team_a", 1),
    TEAM_B("team_b", 2);

    private final String databaseValue;
    private final int slotNumber;

    MatchSlotName(String databaseValue, int slotNumber) {
        this.databaseValue = databaseValue;
        this.slotNumber = slotNumber;
    }

    public String databaseValue() {
        return databaseValue;
    }

    public int slotNumber() {
        return slotNumber;
    }

    public static MatchSlotName fromSlotNumber(int slotNumber) {
        return switch (slotNumber) {
            case 1 -> TEAM_A;
            case 2 -> TEAM_B;
            default -> throw new IllegalArgumentException("Match slot number must be 1 or 2.");
        };
    }

    public static MatchSlotName fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Match slot is required.");
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (MatchSlotName slot : values()) {
            if (slot.databaseValue.equals(normalized)) {
                return slot;
            }
        }

        throw new IllegalArgumentException("Unknown match slot: " + value);
    }
}
