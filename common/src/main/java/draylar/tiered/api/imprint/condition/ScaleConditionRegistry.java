package draylar.tiered.api.imprint.condition;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public final class ScaleConditionRegistry {

    private static final Map<String, ScaleCondition> CONDITIONS = new HashMap<>();

    private ScaleConditionRegistry() {
    }

    public static ScaleCondition register(String id, ScaleCondition condition) {
        CONDITIONS.put(id, condition);
        return condition;
    }

    @Nullable
    public static ScaleCondition get(String id) {
        return id == null ? null : CONDITIONS.get(id);
    }

    public static boolean contains(String id) {
        return id != null && CONDITIONS.containsKey(id);
    }

    public static Set<String> ids() {
        return CONDITIONS.keySet();
    }

    public static void init() {
        register("tiered:target_health_below", (player, target, source, params) ->
                target != null && healthFraction(target) < param(params, "threshold", 0.5f)
                        ? param(params, "multiplier", 1f) : 1f);
        register("tiered:target_health_above", (player, target, source, params) ->
                target != null && healthFraction(target) > param(params, "threshold", 0.5f)
                        ? param(params, "multiplier", 1f) : 1f);
        register("tiered:self_health_below", (player, target, source, params) ->
                healthFraction(player) < param(params, "threshold", 0.5f)
                        ? param(params, "multiplier", 1f) : 1f);
        register("tiered:holder_sprinting", (player, target, source, params) ->
                player.isSprinting() ? param(params, "multiplier", 1f) : 1f);
        register("tiered:holder_on_ground", (player, target, source, params) ->
                player.onGround() ? param(params, "multiplier", 1f) : 1f);
        register("tiered:holder_airborne", (player, target, source, params) ->
                !player.onGround() ? param(params, "multiplier", 1f) : 1f);
    }

    private static float healthFraction(LivingEntity entity) {
        float max = entity.getMaxHealth();
        return max <= 0f ? 1f : entity.getHealth() / max;
    }

    private static float param(Map<String, Float> params, String key, float fallback) {
        Float v = params == null ? null : params.get(key);
        return v == null ? fallback : v;
    }
}
