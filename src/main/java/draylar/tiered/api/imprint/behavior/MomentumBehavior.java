package draylar.tiered.api.imprint.behavior;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;

public class MomentumBehavior extends ImprintBehavior {

    public static final String ID = "tiered:momentum";

    private static final Map<PlayerEntity, State> STATES = new WeakHashMap<>();

    @Override
    public void onDamageDealt(PlayerEntity player, LivingEntity target, float amount,
            float resolvedValue, Map<String, Float> params) {
        addStack(player, target, params);
    }

    @Override
    public void onProjectileDamageDealt(PlayerEntity player, LivingEntity target, float amount,
            float resolvedValue, Map<String, Float> params) {
        addStack(player, target, params);
    }

    @Override
    public void onSpellHeal(PlayerEntity player, LivingEntity target, float amount,
            float resolvedValue, Map<String, Float> params) {

        if (target != null && target != player) addStack(player, target, params);
    }

    private void addStack(PlayerEntity player, LivingEntity target, Map<String, Float> params) {
        if (target == null) return;
        int maxStacks = (int) param(params, "max_stacks", 5f);
        int resetTicks = (int) param(params, "reset_ticks", 60f);
        long now = player.getWorld().getTime();
        UUID targetId = target.getUuid();

        State state = STATES.get(player);
        if (state == null || !targetId.equals(state.targetId) || now - state.lastHitTime > resetTicks) {
            STATES.put(player, new State(targetId, now, 1));
        } else {
            state.stacks = Math.min(state.stacks + 1, maxStacks);
            state.lastHitTime = now;
        }
    }

    private float fraction(PlayerEntity player, float resolvedValue, Map<String, Float> params) {
        int resetTicks = (int) param(params, "reset_ticks", 60f);
        State state = STATES.get(player);
        if (state == null) return 0f;
        if (player.getWorld().getTime() - state.lastHitTime > resetTicks) {
            STATES.remove(player);
            return 0f;
        }
        return resolvedValue * state.stacks;
    }

    @Override
    public float meleeDamageFraction(PlayerEntity player, float resolvedValue, Map<String, Float> params) {
        return fraction(player, resolvedValue, params);
    }

    @Override
    public float rangedDamageFraction(PlayerEntity player, float resolvedValue, Map<String, Float> params) {
        return fraction(player, resolvedValue, params);
    }

    @Override
    public float magicDamageFraction(PlayerEntity player, DamageSource source, float resolvedValue, Map<String, Float> params) {
        return fraction(player, resolvedValue, params);
    }

    private static final class State {
        UUID targetId;
        long lastHitTime;
        int stacks;

        State(UUID targetId, long lastHitTime, int stacks) {
            this.targetId = targetId;
            this.lastHitTime = lastHitTime;
            this.stacks = stacks;
        }
    }
}
