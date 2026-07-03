package draylar.tiered.api.imprint.ability;

import java.util.Map;

import draylar.tiered.api.imprint.behavior.ImprintBehavior;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

public interface ImprintAbility {

    default void onHit(PlayerEntity player, @Nullable LivingEntity target, float amount, int tier, Map<String, Float> params) {
    }

    default void tick(PlayerEntity player, int tier, Map<String, Float> params) {
    }

    default void onEquip(PlayerEntity player, int tier, Map<String, Float> params) {
    }

    default void onMagicHit(PlayerEntity player, @Nullable LivingEntity target, DamageSource source, float amount, int tier, Map<String, Float> params) {
    }

    /**
     * Fired when the ability's owner heals an entity (self or ally/pet). {@code amount} is the heal magnitude.
     * Drives healing-driven abilities (e.g. Requiem). Feeding this is up to the driver (behavior/mixin/event);
     * see {@link ImprintBehavior#onSpellHeal}.
     */
    default void onHeal(PlayerEntity healer, @Nullable LivingEntity healed, float amount, int tier, Map<String, Float> params) {
    }
}
