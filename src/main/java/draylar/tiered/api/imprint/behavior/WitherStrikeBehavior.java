package draylar.tiered.api.imprint.behavior;

import java.util.Map;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;

public class WitherStrikeBehavior extends ImprintBehavior {

    public static final String ID = "tiered:wither_strike";

    @Override
    public void onDamageDealt(PlayerEntity player, LivingEntity target, float amount,
            float resolvedValue, Map<String, Float> params) {
        float chance = param(params, "chance", 1.0f);
        if (chance < 1.0f && player.getRandom().nextFloat() >= chance * resolvedValue) return;
        int duration = (int) param(params, "duration_ticks", 60f);
        int amplifier = (int) param(params, "amplifier", 1f);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, duration, amplifier));
    }
}
