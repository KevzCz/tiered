package draylar.tiered.api.effect;

import draylar.tiered.Tiered;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class ReforgeEffectRegistry {

    private static final Map<String, ReforgeEffect> REGISTRY = new HashMap<>();

    private ReforgeEffectRegistry() {
    }

    public static ReforgeEffect register(String id, ReforgeEffect effect) {
        REGISTRY.put(id, effect);
        return effect;
    }

    @Nullable
    public static ReforgeEffect get(String id) {
        ReforgeEffect builtin = getBuiltin(id);
        if (builtin != null) return builtin;
        return Tiered.EFFECT_DEFINITION_LOADER.get(id);
    }

    @Nullable
    public static ReforgeEffect getBuiltin(String id) {
        if (id == null) return null;
        Identifier parsed = Identifier.tryParse(id);
        return parsed == null ? null : REGISTRY.get(parsed.toString());
    }

    public static Set<String> builtinIds() {
        return REGISTRY.keySet();
    }
}
