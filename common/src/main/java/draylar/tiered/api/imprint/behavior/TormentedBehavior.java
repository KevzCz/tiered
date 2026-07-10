package draylar.tiered.api.imprint.behavior;

import java.util.Map;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class TormentedBehavior extends ImprintBehavior {

    public static final String ID = "tiered:tormented";

    private static float bonus(LivingEntity target, float resolvedValue) {
        if (target == null) return 0f;
        int totalLevels = 0;
        for (MobEffectInstance instance : target.getActiveEffects()) {
            if (instance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                totalLevels += instance.getAmplifier() + 1;
            }
        }
        return totalLevels * resolvedValue;
    }

    @Override
    public float meleeDamageFraction(Player player, LivingEntity target, float resolvedValue, Map<String, Float> params) {
        return bonus(target, resolvedValue);
    }

    @Override
    public float rangedDamageFraction(Player player, LivingEntity target, float resolvedValue, Map<String, Float> params) {
        return bonus(target, resolvedValue);
    }

    @Override
    public float magicDamageFraction(Player player, LivingEntity target, DamageSource source, float resolvedValue, Map<String, Float> params) {
        return bonus(target, resolvedValue);
    }
}
