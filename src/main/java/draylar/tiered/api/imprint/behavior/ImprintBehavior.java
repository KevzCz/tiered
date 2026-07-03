package draylar.tiered.api.imprint.behavior;

import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public abstract class ImprintBehavior {

    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected, Map<String, Float> params) {
    }

    public float onGrant(ItemStack stack, float rolledValue, Map<String, Float> params) {
        return rolledValue;
    }

    public float onExtract(ItemStack stack, float storedValue, Map<String, Float> params) {
        return storedValue;
    }

    public void onRemove(ItemStack stack, Map<String, Float> params) {
    }

    public float meleeDamageFraction(PlayerEntity player, float resolvedValue, Map<String, Float> params) {
        return 0f;
    }

    public float rangedDamageFraction(PlayerEntity player, float resolvedValue, Map<String, Float> params) {
        return 0f;
    }

    public float magicDamageFraction(PlayerEntity player, DamageSource source,
            float resolvedValue, Map<String, Float> params) {
        return 0f;
    }

    public float meleeDamageFraction(PlayerEntity player, LivingEntity target, float resolvedValue, Map<String, Float> params) {
        return meleeDamageFraction(player, resolvedValue, params);
    }

    public float rangedDamageFraction(PlayerEntity player, LivingEntity target, float resolvedValue, Map<String, Float> params) {
        return rangedDamageFraction(player, resolvedValue, params);
    }

    public float magicDamageFraction(PlayerEntity player, LivingEntity target, DamageSource source,
            float resolvedValue, Map<String, Float> params) {
        return magicDamageFraction(player, source, resolvedValue, params);
    }

    public float bonusMeleeDamage(PlayerEntity player, LivingEntity target, float amount,
            float resolvedValue, Map<String, Float> params) {
        return 0f;
    }

    public void onDamageTaken(PlayerEntity player, float amount, DamageSource source,
            float resolvedValue, Map<String, Float> params) {
    }

    public void onDamageDealt(PlayerEntity player, LivingEntity target, float amount,
            float resolvedValue, Map<String, Float> params) {
    }

    public void onKill(PlayerEntity player, LivingEntity target,
            float resolvedValue, Map<String, Float> params) {
    }

    public void onProjectileDamageDealt(PlayerEntity player, LivingEntity target, float amount,
            float resolvedValue, Map<String, Float> params) {
    }

    public void onProjectileKill(PlayerEntity player, LivingEntity target,
            float resolvedValue, Map<String, Float> params) {
    }

    public void onSpellCast(PlayerEntity player, float resolvedValue, Map<String, Float> params) {
    }

    public void onSpellHeal(PlayerEntity player, LivingEntity target, float amount,
            float resolvedValue, Map<String, Float> params) {
    }

    public void tick(PlayerEntity player, float resolvedValue, Map<String, Float> params) {
    }

    public void onEquip(PlayerEntity player, float resolvedValue, Map<String, Float> params) {
    }

    public void onUnequip(PlayerEntity player, float resolvedValue, Map<String, Float> params) {
    }

    public void onCustomEvent(PlayerEntity player, String eventId, float value, float resolvedValue, Map<String, Float> params) {
    }

    protected static float param(Map<String, Float> params, String key, float fallback) {
        Float v = params == null ? null : params.get(key);
        return v == null ? fallback : v;
    }
}
