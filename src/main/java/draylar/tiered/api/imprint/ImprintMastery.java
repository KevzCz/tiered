package draylar.tiered.api.imprint;

import net.minecraft.entity.player.PlayerEntity;

public final class ImprintMastery {

    private ImprintMastery() {
    }

    public static boolean isMastered(PlayerEntity player, String imprintId, float threshold) {
        float max = ImprintResolver.maxCombinedValue(imprintId);
        if (max <= 0f) return false;
        return ImprintResolver.resolveValue(player, imprintId) >= max * threshold;
    }
}
