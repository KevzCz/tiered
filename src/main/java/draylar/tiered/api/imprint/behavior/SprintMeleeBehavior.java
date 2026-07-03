package draylar.tiered.api.imprint.behavior;

import java.util.Map;

import net.minecraft.entity.player.PlayerEntity;

public class SprintMeleeBehavior extends ImprintBehavior {

    public static final String ID = "tiered:sprint_melee";

    @Override
    public float meleeDamageFraction(PlayerEntity player, float resolvedValue, Map<String, Float> params) {
        return player.isSprinting() ? resolvedValue : 0f;
    }
}
