package draylar.tiered.api.imprint.behavior;

import java.util.Map;

import draylar.tiered.api.imprint.ImprintResolver;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;

public class RavenousBehavior extends ImprintBehavior {

    public static final String ID = "tiered:ravenous";

    private static final float FULL_SATURATION = 20f;

    public static final float DRAIN_PER_STACK = 0.05f;

    public static final float MAX_DRAIN = 0.20f;

    public static float exhaustionMultiplier(PlayerEntity player) {
        int stacks = ImprintResolver.stackCount(player, ID);
        if (stacks <= 0) return 1f;
        return 1f + Math.min(stacks * DRAIN_PER_STACK, MAX_DRAIN);
    }

    private float bonus(PlayerEntity player, float resolvedValue, Map<String, Float> params) {
        if (player.getHungerManager().getFoodLevel() < 20) return 0f;

        float maxMultiplier = param(params, "saturation_max_multiplier", 1.5f);
        float saturation = Math.min(player.getHungerManager().getSaturationLevel(), FULL_SATURATION);

        float multiplier = 1f + (saturation / FULL_SATURATION) * (maxMultiplier - 1f);
        return resolvedValue * multiplier;
    }

    @Override
    public float meleeDamageFraction(PlayerEntity player, float resolvedValue, Map<String, Float> params) {
        return bonus(player, resolvedValue, params);
    }

    @Override
    public float rangedDamageFraction(PlayerEntity player, float resolvedValue, Map<String, Float> params) {
        return bonus(player, resolvedValue, params);
    }

    @Override
    public float magicDamageFraction(PlayerEntity player, DamageSource source, float resolvedValue, Map<String, Float> params) {
        return bonus(player, resolvedValue, params);
    }
}
