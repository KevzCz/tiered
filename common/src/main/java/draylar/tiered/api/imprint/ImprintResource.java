package draylar.tiered.api.imprint;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.player.Player;

public final class ImprintResource {

    private static final Map<Player, Map<String, Float>> RESOURCES = new WeakHashMap<>();

    private ImprintResource() {
    }

    public static float get(Player player, String key) {
        Map<String, Float> map = RESOURCES.get(player);
        if (map == null) return 0f;
        Float v = map.get(key);
        return v == null ? 0f : v;
    }

    public static void set(Player player, String key, float value) {
        RESOURCES.computeIfAbsent(player, p -> new HashMap<>()).put(key, value);
    }

    public static float add(Player player, String key, float delta, float max) {
        float next = get(player, key) + delta;
        if (next < 0f) next = 0f;
        if (max > 0f && next > max) next = max;
        set(player, key, next);
        return next;
    }

}
