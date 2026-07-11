package draylar.tiered.api.imprint.ability;

import java.util.Map;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public interface ImprintAbility {

    default void onHit(Player player, @Nullable LivingEntity target, float amount, int tier, Map<String, Float> params) {
    }

    default void tick(Player player, int tier, Map<String, Float> params) {
    }

    default void onEquip(Player player, int tier, Map<String, Float> params) {
    }

    default void onMagicHit(Player player, @Nullable LivingEntity target, DamageSource source, float amount, int tier, Map<String, Float> params) {
    }

    default void onHeal(Player healer, @Nullable LivingEntity healed, float amount, int tier, Map<String, Float> params) {
    }
}
