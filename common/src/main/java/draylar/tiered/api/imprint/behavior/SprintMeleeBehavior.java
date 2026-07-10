package draylar.tiered.api.imprint.behavior;

import java.util.Map;
import net.minecraft.world.entity.player.Player;

public class SprintMeleeBehavior extends ImprintBehavior {

    public static final String ID = "tiered:sprint_melee";

    @Override
    public float meleeDamageFraction(Player player, float resolvedValue, Map<String, Float> params) {
        return player.isSprinting() ? resolvedValue : 0f;
    }
}
