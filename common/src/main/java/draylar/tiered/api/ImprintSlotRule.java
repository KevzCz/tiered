package draylar.tiered.api;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ImprintSlotRule {

    @Nullable
    private final String target;
    // Alternative to "target": a list of ItemVerifier-shaped objects ({"id"/"tag"/"custom": ...}),
    // OR-matched. Lets a single rule target e.g. both Trinkets and Curios items via "custom" ids,
    // without overloading the single-string "target" field's syntax.
    @Nullable
    private final List<ItemVerifier> targets;
    @SerializedName("slots")
    private final Integer slots;
    @SerializedName("min")
    private final Integer min;
    @SerializedName("max")
    private final Integer max;

    public ImprintSlotRule(@Nullable String target, Integer slots) {
        this(target, slots, null, null);
    }

    public ImprintSlotRule(@Nullable String target, Integer slots, Integer min, Integer max) {
        this.target = target;
        this.targets = null;
        this.slots = slots;
        this.min = min;
        this.max = max;
    }

    @Nullable
    public String getTarget() {
        return target;
    }

    @Nullable
    public List<ItemVerifier> getTargets() {
        return targets;
    }

    public int getSlots() {
        Integer base = slots != null ? slots : min;
        return base == null ? 0 : Math.max(0, base);
    }

    public boolean hasMax() {
        return max != null && max >= 0;
    }

    public int getMax() {
        if (!hasMax()) return -1;
        return Math.max(getSlots(), max);
    }

    public int specificity() {
        if (targets != null) return 2;
        if (target == null || target.equals("*")) return 0;
        if (target.endsWith(":*")) return 1;
        if (target.startsWith("#")) return 2;
        return 3;
    }
}
