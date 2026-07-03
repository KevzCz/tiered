package draylar.tiered.api.imprint.behavior;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;

public class RetaliationBehavior extends ImprintBehavior {

    public static final String ID = "tiered:retaliation";

    private static final Map<PlayerEntity, long[]> CHARGED_UNTIL = new WeakHashMap<>();

    public static float consumeCharge(PlayerEntity player) {
        long[] entry = CHARGED_UNTIL.get(player);
        if (entry == null) return 0f;
        if (player.getWorld().getTime() > entry[0]) {
            CHARGED_UNTIL.remove(player);
            return 0f;
        }
        CHARGED_UNTIL.remove(player);
        return Float.intBitsToFloat((int) entry[1]);
    }

    @Override
    public void onDamageTaken(PlayerEntity player, float amount, DamageSource source,
            float resolvedValue, Map<String, Float> params) {
        float bonus = resolvedValue;
        Float dv = params == null ? null : params.get("duration_ticks");
        int duration = dv == null ? 100 : dv.intValue();
        long expiry = player.getWorld().getTime() + duration;
        long[] existing = CHARGED_UNTIL.get(player);
        if (existing == null || Float.intBitsToFloat((int) existing[1]) < bonus
                || player.getWorld().getTime() > existing[0]) {
            CHARGED_UNTIL.put(player, new long[]{expiry, Float.floatToRawIntBits(bonus)});
        }
    }

}
