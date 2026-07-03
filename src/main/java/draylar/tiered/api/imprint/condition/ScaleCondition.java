package draylar.tiered.api.imprint.condition;

import java.util.Map;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface ScaleCondition {

    float multiplier(PlayerEntity player, @Nullable LivingEntity target, @Nullable DamageSource source,
            Map<String, Float> params);
}
