package draylar.tiered.api.imprint.behavior;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

public class RetaliationBehavior extends ImprintBehavior {

    public static final String ID = "tiered:retaliation";

    private static final Map<Player, long[]> CHARGED_UNTIL = new WeakHashMap<>();

    public static float consumeCharge(Player player) {
        long[] entry = CHARGED_UNTIL.get(player);
        if (entry == null) return 0f;
        if (player.level().getGameTime() > entry[0]) {
            CHARGED_UNTIL.remove(player);
            return 0f;
        }
        CHARGED_UNTIL.remove(player);
        return Float.intBitsToFloat((int) entry[1]);
    }

    @Override
    public void onDamageTaken(Player player, float amount, DamageSource source,
            float resolvedValue, Map<String, Float> params) {
        float bonus = resolvedValue;
        Float dv = params == null ? null : params.get("duration_ticks");
        int duration = dv == null ? 100 : dv.intValue();
        long expiry = player.level().getGameTime() + duration;
        long[] existing = CHARGED_UNTIL.get(player);
        if (existing == null || Float.intBitsToFloat((int) existing[1]) < bonus
                || player.level().getGameTime() > existing[0]) {
            CHARGED_UNTIL.put(player, new long[]{expiry, Float.floatToRawIntBits(bonus)});
        }
    }

}
