package draylar.tiered.api.imprint;

import net.minecraft.world.entity.player.Player;

public final class ImprintMastery {

    private ImprintMastery() {
    }

    public static boolean isMastered(Player player, String imprintId, float threshold) {
        float max = ImprintResolver.maxCombinedValue(imprintId);
        if (max <= 0f) return false;
        return ImprintResolver.resolveValue(player, imprintId) >= max * threshold;
    }
}
