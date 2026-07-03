package draylar.tiered.api.imprint.behavior;

import java.util.Map;
import java.util.WeakHashMap;

import draylar.tiered.api.Cooldowns;
import net.minecraft.entity.player.PlayerEntity;

public class SecondWindBehavior extends ImprintBehavior {

    public static final String ID = "tiered:second_wind";

    private static final String PROC_CD = "tiered:second_wind";

    private static final Map<PlayerEntity, Float> HEAL_REMAINING = new WeakHashMap<>();

    private static final Map<PlayerEntity, Boolean> WAS_LOW = new WeakHashMap<>();

    @Override
    public void tick(PlayerEntity player, float resolvedValue, Map<String, Float> params) {
        float threshold = param(params, "health_threshold", 0.35f);
        int cooldownTicks = (int) param(params, "cooldown_ticks", 200f);
        int healTicks = (int) param(params, "heal_ticks", 40f);

        float maxHp = player.getMaxHealth();
        boolean isLow = player.getHealth() < maxHp * threshold;
        boolean wasLow = WAS_LOW.getOrDefault(player, false);
        WAS_LOW.put(player, isLow);

        Float remaining = HEAL_REMAINING.get(player);
        if (remaining != null && remaining > 0f) {
            float perTick = (resolvedValue * maxHp) / healTicks;
            float toHeal = Math.min(perTick, remaining);
            player.heal(toHeal);
            float newRemaining = remaining - toHeal;
            if (newRemaining <= 0.001f) HEAL_REMAINING.remove(player);
            else HEAL_REMAINING.put(player, newRemaining);
        }

        if (!isLow || wasLow) return;

        if (!Cooldowns.ready(player, PROC_CD)) return;

        Cooldowns.start(player, PROC_CD, cooldownTicks);
        HEAL_REMAINING.put(player, resolvedValue * maxHp);
    }
}
