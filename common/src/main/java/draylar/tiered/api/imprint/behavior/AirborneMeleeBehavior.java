package draylar.tiered.api.imprint.behavior;

import java.util.Map;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

public class AirborneMeleeBehavior extends ImprintBehavior {

    public static final String ID = "tiered:airborne_melee";

    @Override
    public float meleeDamageFraction(Player player, float resolvedValue, Map<String, Float> params) {
        return !player.onGround() ? resolvedValue : 0f;
    }

    @Override
    public float rangedDamageFraction(Player player, float resolvedValue, Map<String, Float> params) {
        return !player.onGround() ? resolvedValue : 0f;
    }

    @Override
    public float magicDamageFraction(Player player, DamageSource source, float resolvedValue, Map<String, Float> params) {
        return !player.onGround() ? resolvedValue : 0f;
    }
}
