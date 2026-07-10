package draylar.tiered.api.imprint;

import java.util.HashMap;
import java.util.Map;

public final class ImprintCooldownDisplay {

    private static final Map<String, Float> FILLS = new HashMap<>();
    private static final Map<String, Boolean> ACTIVE = new HashMap<>();

    private ImprintCooldownDisplay() {}

    public static void set(String imprintId, float fill) {
        if (imprintId != null) FILLS.put(imprintId, Math.max(0f, Math.min(1f, fill)));
    }

    public static void setActive(String imprintId, boolean active) {
        if (imprintId != null) ACTIVE.put(imprintId, active);
    }

    public static float fillOf(String imprintId) {
        return FILLS.getOrDefault(imprintId, 1.0f);
    }

    public static boolean isActive(String imprintId) {
        return ACTIVE.getOrDefault(imprintId, false);
    }
}
