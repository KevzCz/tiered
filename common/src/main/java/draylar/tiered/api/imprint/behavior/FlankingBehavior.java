package draylar.tiered.api.imprint.behavior;

import java.util.Map;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import draylar.tiered.api.imprint.ImprintResolver;

public class FlankingBehavior extends ImprintBehavior {

    public static final String ID = "tiered:flanking";

    private float directionalFraction(Player player, LivingEntity target, float resolvedValue, Map<String, Float> params) {
        if (target == null) return 0f;

        float bonusThreshold = param(params, "bonus_angle_threshold", 120f);
        float penaltyThreshold = param(params, "penalty_angle_threshold", 60f);

        double yawRad = Math.toRadians(target.yBodyRot);
        double facingX = -Math.sin(yawRad);
        double facingZ = Math.cos(yawRad);
        Vec3 toAttacker = player.position().subtract(target.position());
        double toLen = Math.hypot(toAttacker.x, toAttacker.z);
        if (toLen < 1.0e-4) return 0f;

        double cos = (facingX * toAttacker.x + facingZ * toAttacker.z) / toLen;
        double angle = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, cos))));

        if (angle >= bonusThreshold) {
            return resolvedValue;
        }
        if (angle <= penaltyThreshold) {

            return ImprintResolver.resolveExtraValue(player, ID, "penalty");
        }
        return 0f;
    }

    @Override
    public float meleeDamageFraction(Player player, LivingEntity target, float resolvedValue, Map<String, Float> params) {
        return directionalFraction(player, target, resolvedValue, params);
    }

    @Override
    public float rangedDamageFraction(Player player, LivingEntity target, float resolvedValue, Map<String, Float> params) {
        return directionalFraction(player, target, resolvedValue, params);
    }

    @Override
    public float magicDamageFraction(Player player, LivingEntity target, DamageSource source, float resolvedValue, Map<String, Float> params) {
        return directionalFraction(player, target, resolvedValue, params);
    }
}
