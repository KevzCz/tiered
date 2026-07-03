package draylar.tiered.api.imprint.behavior;

import java.util.Map;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;

public class TormentedBehavior extends ImprintBehavior {

    public static final String ID = "tiered:tormented";

    private static float bonus(LivingEntity target, float resolvedValue) {
        if (target == null) return 0f;
        int totalLevels = 0;
        for (StatusEffectInstance instance : target.getStatusEffects()) {
            if (instance.getEffectType().value().getCategory() == StatusEffectCategory.HARMFUL) {
                totalLevels += instance.getAmplifier() + 1;
            }
        }
        return totalLevels * resolvedValue;
    }

    @Override
    public float meleeDamageFraction(PlayerEntity player, LivingEntity target, float resolvedValue, Map<String, Float> params) {
        return bonus(target, resolvedValue);
    }

    @Override
    public float rangedDamageFraction(PlayerEntity player, LivingEntity target, float resolvedValue, Map<String, Float> params) {
        return bonus(target, resolvedValue);
    }

    @Override
    public float magicDamageFraction(PlayerEntity player, LivingEntity target, DamageSource source, float resolvedValue, Map<String, Float> params) {
        return bonus(target, resolvedValue);
    }
}
