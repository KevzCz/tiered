package draylar.tiered.api.imprint.behavior;

import java.util.Map;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class WitherStrikeBehavior extends ImprintBehavior {

    public static final String ID = "tiered:wither_strike";

    @Override
    public void onDamageDealt(Player player, LivingEntity target, float amount,
            float resolvedValue, Map<String, Float> params) {
        float chance = param(params, "chance", 1.0f);
        if (chance < 1.0f && player.getRandom().nextFloat() >= chance * resolvedValue) return;
        int duration = (int) param(params, "duration_ticks", 60f);
        int amplifier = (int) param(params, "amplifier", 1f);
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, duration, amplifier));
    }
}
