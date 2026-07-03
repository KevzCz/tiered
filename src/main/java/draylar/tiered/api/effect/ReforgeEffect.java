package draylar.tiered.api.effect;

import java.util.Map;

import draylar.tiered.api.imprint.Imprint;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public abstract class ReforgeEffect {

    public boolean skipsReforge() {
        return false;
    }

    public boolean canRun(ItemStack target) {
        return true;
    }

    public abstract boolean onReforge(PlayerEntity player, ItemStack stack, EffectContext context);

    public void preReforge(PlayerEntity player, ItemStack stack, EffectContext context, RollBias rollBias) {
    }

    public record EffectContext(World world, @Nullable BlockPos pos, Map<String, Float> params, ItemStack addition) {
        public EffectContext(World world, @Nullable BlockPos pos, Map<String, Float> params) {
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
