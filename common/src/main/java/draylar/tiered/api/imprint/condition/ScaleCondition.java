package draylar.tiered.api.imprint.condition;

import java.util.Map;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface ScaleCondition {

    float multiplier(Player player, @Nullable LivingEntity target, @Nullable DamageSource source,
            Map<String, Float> params);
}
