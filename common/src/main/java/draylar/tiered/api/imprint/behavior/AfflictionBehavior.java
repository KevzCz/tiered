package draylar.tiered.api.imprint.behavior;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import draylar.tiered.api.imprint.ImprintResolver;

public class AfflictionBehavior extends ImprintBehavior {

    public static final String ID = "tiered:affliction";

    private static final Map<LivingEntity, float[]> MARKS = new WeakHashMap<>();

    @Override
    public void onDamageDealt(Player player, LivingEntity target, float amount,
            float resolvedValue, Map<String, Float> params) {
        if (resolvedValue <= 0f) return;
        int baseDuration = (int) param(params, "effect_duration_ticks", 100f);
        int bonusDuration = (int) ImprintResolver.resolveExtraValue(player, ID, "duration");
        long expiry = player.level().getGameTime() + baseDuration + bonusDuration;
        MARKS.put(target, new float[]{resolvedValue, expiry});
    }

    public static float amplifierFor(LivingEntity target) {
        float[] mark = MARKS.get(target);
        if (mark == null) return 0f;
        if (target.level().getGameTime() > (long) mark[1]) {
            MARKS.remove(target);
            return 0f;
        }
        return mark[0];
    }
}
