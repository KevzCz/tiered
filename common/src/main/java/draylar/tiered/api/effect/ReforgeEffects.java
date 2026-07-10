package draylar.tiered.api.effect;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class ReforgeEffects {

    private ReforgeEffects() {
    }

    @Nullable
    public static DataEffect findExtract(@Nullable List<String> effectIds) {
        if (effectIds == null) return null;
        for (String id : effectIds) {
            ReforgeEffect effect = ReforgeEffectRegistry.get(id);
            if (effect instanceof DataEffect data && data.definition().isExtract()) return data;
        }
        return null;
    }

    public static boolean anySkipsReforge(@Nullable List<String> effectIds) {
        if (effectIds == null) return false;
        for (String id : effectIds) {
            ReforgeEffect effect = ReforgeEffectRegistry.get(id);
            if (effect != null && effect.skipsReforge()) return true;
        }
        return false;
    }

    public static boolean anyCanRun(@Nullable List<String> effectIds, ItemStack target) {
        if (effectIds == null) return false;
        for (String id : effectIds) {
            ReforgeEffect effect = ReforgeEffectRegistry.get(id);
            if (effect != null && effect.canRun(target)) return true;
        }
        return false;
    }

    public static boolean run(Player player, ItemStack stack, @Nullable List<String> effectIds,
            @Nullable Map<String, Map<String, Float>> effectParams, Level world, @Nullable BlockPos pos, ItemStack addition) {
        if (effectIds == null || effectIds.isEmpty()) return false;
        boolean changed = false;
        for (String id : effectIds) {
            ReforgeEffect effect = ReforgeEffectRegistry.get(id);
            if (effect == null) continue;
            Map<String, Float> params = effectParams == null ? Map.of() : effectParams.getOrDefault(id, Map.of());
            if (effect.onReforge(player, stack, new ReforgeEffect.EffectContext(world, pos, params,
                    addition == null ? ItemStack.EMPTY : addition))) {
                changed = true;
            }
        }
        return changed;
    }

    public static boolean run(Player player, ItemStack stack, @Nullable List<String> effectIds,
            @Nullable Map<String, Map<String, Float>> effectParams, Level world, @Nullable BlockPos pos) {
        return run(player, stack, effectIds, effectParams, world, pos, ItemStack.EMPTY);
    }

    public static ReforgeEffect.RollBias preReforge(Player player, ItemStack stack,
            @Nullable List<String> effectIds, @Nullable Map<String, Map<String, Float>> effectParams,
            Level world, @Nullable BlockPos pos, ItemStack addition) {
        ReforgeEffect.RollBias bias = new ReforgeEffect.RollBias();
        if (effectIds == null || effectIds.isEmpty()) return bias;
        for (String id : effectIds) {
            ReforgeEffect effect = ReforgeEffectRegistry.get(id);
            if (effect == null) continue;
            Map<String, Float> params = effectParams == null ? Map.of() : effectParams.getOrDefault(id, Map.of());
            effect.preReforge(player, stack, new ReforgeEffect.EffectContext(world, pos, params,
                    addition == null ? ItemStack.EMPTY : addition), bias);
        }
        return bias;
    }
}
