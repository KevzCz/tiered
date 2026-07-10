package draylar.tiered.api.imprint.behavior;

import java.util.Map;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class SoulHarvestBehavior extends ImprintBehavior {

    public static final String ID = "tiered:soul_harvest";

    @Override
    public void onKill(Player player, LivingEntity target, float resolvedValue,
            Map<String, Float> params) {
        int addDuration = (int) (param(params, "duration_ticks", 100f) * resolvedValue);
        int amplifier = (int) param(params, "amplifier", 0f);
        int maxDuration = (int) param(params, "max_duration_ticks", 400f);

        MobEffectInstance existing = player.getEffect(MobEffects.DAMAGE_BOOST);
        int currentDuration = existing != null && existing.getAmplifier() == amplifier
                ? existing.getDuration() : 0;
        int newDuration = Math.min(currentDuration + addDuration, maxDuration);

        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, newDuration, amplifier));
    }
}
