package draylar.tiered.api.imprint.behavior;

import java.util.Map;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

public class BloodthirstBehavior extends ImprintBehavior {

    public static final String ID = "tiered:bloodthirst";

    @Override
    public void onDamageDealt(PlayerEntity player, LivingEntity target, float amount,
            float resolvedValue, Map<String, Float> params) {
        if (resolvedValue <= 0f || amount <= 0f) return;
        if (player.getRandom().nextFloat() >= resolvedValue) return;
        float heal = amount * param(params, "heal_fraction", 0.1f);
        if (heal > 0f) player.heal(heal);
    }
}
