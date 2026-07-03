package draylar.tiered.api.imprint.behavior;

import java.util.Map;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;

public class CondemnedBehavior extends ImprintBehavior {

    public static final String ID = "tiered:condemned";

    private float bonus(LivingEntity target, float resolvedValue, Map<String, Float> params) {
        if (target == null) return 0f;
        int negatives = countNegative(target);
        if (negatives == 0) return 0f;

        float perDebuff = resolvedValue;
        int thresholdCount = (int) param(params, "threshold_count", 4f);
        if (negatives >= thresholdCount) {
            perDebuff *= param(params, "threshold_multiplier", 1.2f);
        }
        return perDebuff * negatives;
    }

    private static int countNegative(LivingEntity target) {
        int n = 0;
        for (StatusEffectInstance instance : target.getStatusEffects()) {
            if (instance.getEffectType().value().getCategory() == StatusEffectCategory.HARMFUL) n++;
        }
        return n;
    }

    @Override
    public float meleeDamageFraction(PlayerEntity player, LivingEntity target, float resolvedValue, Map<String, Float> params) {
        return bonus(target, resolvedValue, params);
    }

    @Override
    public float rangedDamageFraction(PlayerEntity player, LivingEntity target, float resolvedValue, Map<String, Float> params) {
        return bonus(target, resolvedValue, params);
    }

    @Override
    public float magicDamageFraction(PlayerEntity player, LivingEntity target, DamageSource source, float resolvedValue, Map<String, Float> params) {
        return bonus(target, resolvedValue, params);
    }
}
