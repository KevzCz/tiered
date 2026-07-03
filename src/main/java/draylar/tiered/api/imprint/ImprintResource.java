package draylar.tiered.api.imprint;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.entity.player.PlayerEntity;

public final class ImprintResource {

    private static final Map<PlayerEntity, Map<String, Float>> RESOURCES = new WeakHashMap<>();

    private ImprintResource() {
    }

    public static float get(PlayerEntity player, String key) {
        Map<String, Float> map = RESOURCES.get(player);
        if (map == null) return 0f;
        Float v = map.get(key);
        return v == null ? 0f : v;
    }

    public static void set(PlayerEntity player, String key, float value) {
        RESOURCES.computeIfAbsent(player, p -> new HashMap<>()).put(key, value);
    }

    public static float add(PlayerEntity player, String key, float delta, float max) {
        float next = get(player, key) + delta;
        if (next < 0f) next = 0f;
        if (max > 0f && next > max) next = max;
        set(player, key, next);
        return next;
    }

}
