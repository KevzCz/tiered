package draylar.tiered.api.effect;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import draylar.tiered.api.imprint.Imprint;
import org.jetbrains.annotations.Nullable;

public abstract class ReforgeEffect {

    public boolean skipsReforge() {
        return false;
    }

    public boolean canRun(ItemStack target) {
        return true;
    }

    public abstract boolean onReforge(Player player, ItemStack stack, EffectContext context);

    public void preReforge(Player player, ItemStack stack, EffectContext context, RollBias rollBias) {
    }

    public record EffectContext(Level world, @Nullable BlockPos pos, Map<String, Float> params, ItemStack addition) {
        public EffectContext(Level world, @Nullable BlockPos pos, Map<String, Float> params) {
            this(world, pos, params, ItemStack.EMPTY);
        }

        public float param(String key, float fallback) {
            Float v = params == null ? null : params.get(key);
            return v == null ? fallback : v;
        }
    }

    public static class RollBias {
        @Nullable public String guaranteedMinRarity;
        public float extraRarityBoost;
        public int extraBaseCost;

        public boolean isEmpty() {
            return guaranteedMinRarity == null && extraRarityBoost == 0f && extraBaseCost == 0;
        }
    }
}
