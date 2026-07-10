package draylar.tiered.api.imprint.behavior;

import java.util.Map;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public abstract class ImprintBehavior {

    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected, Map<String, Float> params) {
    }

    public float onGrant(ItemStack stack, float rolledValue, Map<String, Float> params) {
        return rolledValue;
    }

    public float onExtract(ItemStack stack, float storedValue, Map<String, Float> params) {
        return storedValue;
    }

    public void onRemove(ItemStack stack, Map<String, Float> params) {
    }

    public float meleeDamageFraction(Player player, float resolvedValue, Map<String, Float> params) {
        return 0f;
    }

    public float rangedDamageFraction(Player player, float resolvedValue, Map<String, Float> params) {
        return 0f;
    }

    public float magicDamageFraction(Player player, DamageSource source,
            float resolvedValue, Map<String, Float> params) {
        return 0f;
    }

    public float meleeDamageFraction(Player player, LivingEntity target, float resolvedValue, Map<String, Float> params) {
        return meleeDamageFraction(player, resolvedValue, params);
    }

    public float rangedDamageFraction(Player player, LivingEntity target, float resolvedValue, Map<String, Float> params) {
        return rangedDamageFraction(player, resolvedValue, params);
    }

    public float magicDamageFraction(Player player, LivingEntity target, DamageSource source,
            float resolvedValue, Map<String, Float> params) {
        return magicDamageFraction(player, source, resolvedValue, params);
    }

    public float bonusMeleeDamage(Player player, LivingEntity target, float amount,
            float resolvedValue, Map<String, Float> params) {
        return 0f;
    }

    public void onDamageTaken(Player player, float amount, DamageSource source,
            float resolvedValue, Map<String, Float> params) {
    }

    public void onDamageDealt(Player player, LivingEntity target, float amount,
            float resolvedValue, Map<String, Float> params) {
    }

    public void onKill(Player player, LivingEntity target,
            float resolvedValue, Map<String, Float> params) {
    }

    public void onProjectileDamageDealt(Player player, LivingEntity target, float amount,
            float resolvedValue, Map<String, Float> params) {
    }

    public void onProjectileKill(Player player, LivingEntity target,
            float resolvedValue, Map<String, Float> params) {
    }

    public void onSpellCast(Player player, float resolvedValue, Map<String, Float> params) {
    }

    public void onSpellHeal(Player player, LivingEntity target, float amount,
            float resolvedValue, Map<String, Float> params) {
    }

    public void tick(Player player, float resolvedValue, Map<String, Float> params) {
    }

    public void onEquip(Player player, float resolvedValue, Map<String, Float> params) {
    }

    public void onUnequip(Player player, float resolvedValue, Map<String, Float> params) {
    }

    public void onCustomEvent(Player player, String eventId, float value, float resolvedValue, Map<String, Float> params) {
    }

    protected static float param(Map<String, Float> params, String key, float fallback) {
        Float v = params == null ? null : params.get(key);
        return v == null ? fallback : v;
    }
}
