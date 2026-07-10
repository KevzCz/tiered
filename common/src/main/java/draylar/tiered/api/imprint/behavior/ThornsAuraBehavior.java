package draylar.tiered.api.imprint.behavior;

import java.util.Map;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class ThornsAuraBehavior extends ImprintBehavior {

    public static final String ID = "tiered:thorns_aura";

    @Override
    public void onDamageTaken(Player player, float amount, DamageSource source,
            float resolvedValue, Map<String, Float> params) {
        if (!(source.getEntity() instanceof LivingEntity attacker)) return;
        float fraction = resolvedValue;
        float minDmg = param(params, "min_damage", 0.5f);
        float reflected = Math.max(amount * fraction, minDmg);
        attacker.hurt(player.damageSources().magic(), reflected);
    }
}
