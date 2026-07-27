package draylar.tiered.api.imprint.ability;

import java.util.HashMap;
import java.util.Map;

import draylar.tiered.api.imprint.DataImprint;
import draylar.tiered.api.imprint.ImprintRegistry;
import draylar.tiered.api.imprint.behavior.ImprintBehavior;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;


public abstract class AbilityBridgeBehavior extends ImprintBehavior {

    protected abstract String imprintId();

    @Nullable
    protected DataImprint imprint() {
        return ImprintRegistry.get(imprintId()) instanceof DataImprint di ? di : null;
    }

    @Nullable
    protected AbilityBinding activeBinding(Player player) {
        DataImprint imprint = imprint();
        return imprint == null ? null : imprint.activeBinding(player, player.getMainHandItem());
    }

    protected Map<String, Float> mergedParams(AbilityBinding binding, int tier) {
        DataImprint imprint = imprint();
        Map<String, Float> shared = imprint == null ? null : imprint.definition().getParams();
        Map<String, Float> own = binding.getParams(tier);
        if (shared == null || shared.isEmpty()) return own;
        if (own.isEmpty()) return shared;
        Map<String, Float> merged = new HashMap<>(shared);
        merged.putAll(own);
        return merged;
    }

    protected int tierOf(float resolvedValue) {
        return Math.max(1, (int) resolvedValue);
    }

    private void dispatch(Player player, float resolvedValue, AbilityAction action) {
        if (player == null || player.level().isClientSide()) return;
        AbilityBinding binding = activeBinding(player);
        if (binding == null) return;
        ImprintAbility ability = ImprintAbilityRegistry.get(binding.getAbility());
        if (ability == null) return;
        int tier = tierOf(resolvedValue);
        action.run(ability, tier, mergedParams(binding, tier));
    }

    @FunctionalInterface
    private interface AbilityAction {
        void run(ImprintAbility ability, int tier, Map<String, Float> params);
    }

    @Override
    public void onDamageDealt(Player player, LivingEntity target, float amount,
            float resolvedValue, Map<String, Float> params) {
        if (target == null) return;
        dispatch(player, resolvedValue, (a, t, p) -> a.onHit(player, target, amount, t, p));
    }

    @Override
    public void onKill(Player player, LivingEntity target,
            float resolvedValue, Map<String, Float> params) {
        if (target == null) return;
        dispatch(player, resolvedValue, (a, t, p) -> a.onKill(player, target, t, p));
    }

    @Override
    public void onDamageTaken(Player player, float amount, DamageSource source,
            float resolvedValue, Map<String, Float> params) {
        dispatch(player, resolvedValue, (a, t, p) -> a.onDamageTaken(player, amount, source, t, p));
    }

    @Override
    public void onProjectileDamageDealt(Player player, LivingEntity target, float amount,
            float resolvedValue, Map<String, Float> params) {
        dispatch(player, resolvedValue, (a, t, p) -> a.onProjectileHit(player, target, amount, t, p));
    }

    @Override
    public void onProjectileKill(Player player, LivingEntity target,
            float resolvedValue, Map<String, Float> params) {
        if (target == null) return;
        dispatch(player, resolvedValue, (a, t, p) -> a.onProjectileKill(player, target, t, p));
    }

    @Override
    public void onSpellCast(Player player, float resolvedValue, Map<String, Float> params) {
        dispatch(player, resolvedValue, (a, t, p) -> a.onSpellCast(player, t, p));
    }

    @Override
    public void onSpellHeal(Player player, LivingEntity target, float amount,
            float resolvedValue, Map<String, Float> params) {
        dispatch(player, resolvedValue, (a, t, p) -> a.onHeal(player, target, amount, t, p));
    }

    @Override
    public void tick(Player player, float resolvedValue, Map<String, Float> params) {
        dispatch(player, resolvedValue, (a, t, p) -> a.tick(player, t, p));
    }

    @Override
    public void onEquip(Player player, float resolvedValue, Map<String, Float> params) {
        dispatch(player, resolvedValue, (a, t, p) -> a.onEquip(player, t, p));
    }

    @Override
    public void onUnequip(Player player, float resolvedValue, Map<String, Float> params) {
        dispatch(player, resolvedValue, (a, t, p) -> a.onUnequip(player, t, p));
    }

    @Override
    public void onCustomEvent(Player player, String eventId, float value,
            float resolvedValue, Map<String, Float> params) {
        dispatch(player, resolvedValue, (a, t, p) -> a.onCustomEvent(player, eventId, value, t, p));
    }
}
