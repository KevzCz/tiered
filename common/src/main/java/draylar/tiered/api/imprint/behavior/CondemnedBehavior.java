package draylar.tiered.api.imprint.behavior;

import java.util.Map;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

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
        for (MobEffectInstance instance : target.getActiveEffects()) {
            if (instance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) n++;
        }
        return n;
    }

    @Override
    public float meleeDamageFraction(Player player, LivingEntity target, float resolvedValue, Map<String, Float> params) {
        return bonus(target, resolvedValue, params);
    }

    @Override
    public float rangedDamageFraction(Player player, LivingEntity target, float resolvedValue, Map<String, Float> params) {
        return bonus(target, resolvedValue, params);
    }

    @Override
    public float magicDamageFraction(Player player, LivingEntity target, DamageSource source, float resolvedValue, Map<String, Float> params) {
        return bonus(target, resolvedValue, params);
    }
}
