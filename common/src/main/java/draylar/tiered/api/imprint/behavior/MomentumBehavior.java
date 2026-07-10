package draylar.tiered.api.imprint.behavior;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class MomentumBehavior extends ImprintBehavior {

    public static final String ID = "tiered:momentum";

    private static final Map<Player, State> STATES = new WeakHashMap<>();

    @Override
    public void onDamageDealt(Player player, LivingEntity target, float amount,
            float resolvedValue, Map<String, Float> params) {
        addStack(player, target, params);
    }

    @Override
    public void onProjectileDamageDealt(Player player, LivingEntity target, float amount,
            float resolvedValue, Map<String, Float> params) {
        addStack(player, target, params);
    }

    @Override
    public void onSpellHeal(Player player, LivingEntity target, float amount,
            float resolvedValue, Map<String, Float> params) {

        if (target != null && target != player) addStack(player, target, params);
    }

    private void addStack(Player player, LivingEntity target, Map<String, Float> params) {
        if (target == null) return;
        int maxStacks = (int) param(params, "max_stacks", 5f);
        int resetTicks = (int) param(params, "reset_ticks", 60f);
        long now = player.level().getGameTime();
        UUID targetId = target.getUUID();

        State state = STATES.get(player);
        if (state == null || !targetId.equals(state.targetId) || now - state.lastHitTime > resetTicks) {
            STATES.put(player, new State(targetId, now, 1));
        } else {
            state.stacks = Math.min(state.stacks + 1, maxStacks);
            state.lastHitTime = now;
        }
    }

    private float fraction(Player player, float resolvedValue, Map<String, Float> params) {
        int resetTicks = (int) param(params, "reset_ticks", 60f);
        State state = STATES.get(player);
        if (state == null) return 0f;
        if (player.level().getGameTime() - state.lastHitTime > resetTicks) {
            STATES.remove(player);
            return 0f;
        }
        return resolvedValue * state.stacks;
    }

    @Override
    public float meleeDamageFraction(Player player, float resolvedValue, Map<String, Float> params) {
        return fraction(player, resolvedValue, params);
    }

    @Override
    public float rangedDamageFraction(Player player, float resolvedValue, Map<String, Float> params) {
        return fraction(player, resolvedValue, params);
    }

    @Override
    public float magicDamageFraction(Player player, DamageSource source, float resolvedValue, Map<String, Float> params) {
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
