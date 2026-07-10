package draylar.tiered.api.imprint.behavior;

import java.util.Map;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

public class LastStandBehavior extends ImprintBehavior {

    public static final String ID = "tiered:last_stand";

    private float fraction(Player player, float resolvedValue, Map<String, Float> params) {
        float threshold = params != null && params.containsKey("health_threshold") ? params.get("health_threshold") : 0.25f;
        return player.getHealth() / player.getMaxHealth() <= threshold ? resolvedValue : 0f;
    }

    @Override
    public float meleeDamageFraction(Player player, float resolvedValue, Map<String, Float> params) {
        return fraction(player, resolvedValue, params);
    }

    @Override
    public float rangedDamageFraction(Player player, float resolvedValue, Map<String, Float> params) {
        return fraction(player, resolvedValue, params);
    }

    @Override
    public float magicDamageFraction(Player player, DamageSource source, float resolvedValue, Map<String, Float> params) {
        return fraction(player, resolvedValue, params);
    }
}
