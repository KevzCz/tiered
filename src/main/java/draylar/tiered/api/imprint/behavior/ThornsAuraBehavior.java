package draylar.tiered.api.imprint.behavior;

import java.util.Map;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;

public class ThornsAuraBehavior extends ImprintBehavior {

    public static final String ID = "tiered:thorns_aura";

    @Override
    public void onDamageTaken(PlayerEntity player, float amount, DamageSource source,
            float resolvedValue, Map<String, Float> params) {
        if (!(source.getAttacker() instanceof LivingEntity attacker)) return;
        float fraction = resolvedValue;
        float minDmg = param(params, "min_damage", 0.5f);
        float reflected = Math.max(amount * fraction, minDmg);
        attacker.damage(player.getDamageSources().magic(), reflected);
    }
}
