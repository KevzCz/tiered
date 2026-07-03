package draylar.tiered.api.imprint.behavior;

import java.util.Map;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;

public class SoulHarvestBehavior extends ImprintBehavior {

    public static final String ID = "tiered:soul_harvest";

    @Override
    public void onKill(PlayerEntity player, LivingEntity target, float resolvedValue,
            Map<String, Float> params) {
        int addDuration = (int) (param(params, "duration_ticks", 100f) * resolvedValue);
        int amplifier = (int) param(params, "amplifier", 0f);
        int maxDuration = (int) param(params, "max_duration_ticks", 400f);

        StatusEffectInstance existing = player.getStatusEffect(StatusEffects.STRENGTH);
        int currentDuration = existing != null && existing.getAmplifier() == amplifier
                ? existing.getDuration() : 0;
        int newDuration = Math.min(currentDuration + addDuration, maxDuration);

        player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, newDuration, amplifier));
    }
}
