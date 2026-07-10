package draylar.tiered.api.imprint.ability;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

public final class ImprintAbilityRegistry {

    private static final Map<String, ImprintAbility> ABILITIES = new HashMap<>();

    private ImprintAbilityRegistry() {
    }

    public static void register(String id, ImprintAbility ability) {
        ABILITIES.put(id, ability);
    }

    @Nullable
    public static ImprintAbility get(String id) {
        return ABILITIES.get(id);
    }
}
