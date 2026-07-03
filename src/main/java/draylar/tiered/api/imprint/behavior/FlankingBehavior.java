package draylar.tiered.api.imprint.behavior;

import java.util.Map;

import draylar.tiered.api.imprint.ImprintResolver;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class FlankingBehavior extends ImprintBehavior {

    public static final String ID = "tiered:flanking";

    private float directionalFraction(PlayerEntity player, LivingEntity target, float resolvedValue, Map<String, Float> params) {
        if (target == null) return 0f;

        float bonusThreshold = param(params, "bonus_angle_threshold", 120f);
        float penaltyThreshold = param(params, "penalty_angle_threshold", 60f);

        double yawRad = Math.toRadians(target.bodyYaw);
        double facingX = -Math.sin(yawRad);
        double facingZ = Math.cos(yawRad);
        Vec3d toAttacker = player.getPos().subtract(target.getPos());
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
    public float meleeDamageFraction(PlayerEntity player, LivingEntity target, float resolvedValue, Map<String, Float> params) {
        return directionalFraction(player, target, resolvedValue, params);
    }

    @Override
    public float rangedDamageFraction(PlayerEntity player, LivingEntity target, float resolvedValue, Map<String, Float> params) {
        return directionalFraction(player, target, resolvedValue, params);
    }

    @Override
    public float magicDamageFraction(PlayerEntity player, LivingEntity target, DamageSource source, float resolvedValue, Map<String, Float> params) {
        return directionalFraction(player, target, resolvedValue, params);
    }
}
