package draylar.tiered.api.imprint.behavior;

import java.util.Map;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class ExecutionerBehavior extends ImprintBehavior {

    public static final String ID = "tiered:executioner";

    @Override
    public void onKill(Player player, LivingEntity target, float resolvedValue,
            Map<String, Float> params) {
        float heal = param(params, "heal_amount", 2.0f) * resolvedValue;
        if (heal > 0f) player.heal(heal);
    }
}
