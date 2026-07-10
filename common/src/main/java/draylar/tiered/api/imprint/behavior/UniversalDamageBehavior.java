package draylar.tiered.api.imprint.behavior;

import java.util.Map;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

public class UniversalDamageBehavior extends ImprintBehavior {

    public static final String ID = "tiered:universal_damage";

    public static final String PARAM_APPLY_MELEE = "apply_melee";
    public static final String PARAM_APPLY_RANGED = "apply_ranged";
    public static final String PARAM_APPLY_MAGIC = "apply_magic";

    private static boolean enabled(Map<String, Float> params, String key) {
        if (params == null) return true;
        Float flag = params.get(key);
        return flag == null || flag > 0f;
    }

    @Override
    public float meleeDamageFraction(Player player, float resolvedValue, Map<String, Float> params) {
        return enabled(params, PARAM_APPLY_MELEE) ? resolvedValue : 0f;
    }

    @Override
    public float rangedDamageFraction(Player player, float resolvedValue, Map<String, Float> params) {
        return enabled(params, PARAM_APPLY_RANGED) ? resolvedValue : 0f;
    }

    @Override
    public float magicDamageFraction(Player player, DamageSource source, float resolvedValue, Map<String, Float> params) {
        return enabled(params, PARAM_APPLY_MAGIC) ? resolvedValue : 0f;
    }
}
