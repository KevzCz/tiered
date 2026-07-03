package draylar.tiered.api.imprint.behavior;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

public final class ImprintBehaviorRegistry {

    private static final Map<String, ImprintBehavior> BEHAVIORS = new HashMap<>();

    private ImprintBehaviorRegistry() {
    }

    public static ImprintBehavior register(String id, ImprintBehavior behavior) {
        BEHAVIORS.put(id, behavior);
        return behavior;
    }

    @Nullable
    public static ImprintBehavior get(String id) {
        return id == null ? null : BEHAVIORS.get(id);
    }

    public static boolean contains(String id) {
        return id != null && BEHAVIORS.containsKey(id);
    }

    public static Set<String> ids() {
        return BEHAVIORS.keySet();
    }
}
