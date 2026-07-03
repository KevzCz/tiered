package draylar.tiered.api.imprint.behavior;

import java.util.Map;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;

public class AirborneMeleeBehavior extends ImprintBehavior {

    public static final String ID = "tiered:airborne_melee";

    @Override
    public float meleeDamageFraction(PlayerEntity player, float resolvedValue, Map<String, Float> params) {
        return !player.isOnGround() ? resolvedValue : 0f;
    }

    @Override
    public float rangedDamageFraction(PlayerEntity player, float resolvedValue, Map<String, Float> params) {
        return !player.isOnGround() ? resolvedValue : 0f;
    }

    @Override
    public float magicDamageFraction(PlayerEntity player, DamageSource source, float resolvedValue, Map<String, Float> params) {
        return !player.isOnGround() ? resolvedValue : 0f;
    }
}
