package net.doole.doolestools.item;

import org.jetbrains.annotations.Nullable;

/** The four upgrade card types. Replaces the string literals "speed", "stack", "range", "efficiency". */
public enum UpgradeType {
    SPEED("speed", "Speed"),
    STACK("stack", "Stack"),
    RANGE("range", "Range"),
    EFFICIENCY("efficiency", "Efficiency");

    /** Lower-case persistence/config key (identical to the string previously used everywhere). */
    public final String id;
    /** Display label used in player messages and UI. */
    public final String label;

    UpgradeType(String id, String label) {
        this.id = id;
        this.label = label;
    }

    /** Returns the matching type for a given id string, or {@code null} if unrecognised. */
    @Nullable
    public static UpgradeType byId(@Nullable String id) {
        if (id == null) return null;
        for (UpgradeType t : values()) if (t.id.equals(id)) return t;
        return null;
    }
}
