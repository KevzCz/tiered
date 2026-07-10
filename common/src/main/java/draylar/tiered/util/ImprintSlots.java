package draylar.tiered.util;

import draylar.tiered.Tiered;
import draylar.tiered.config.ConfigInit;
import draylar.tiered.data.ImprintSlotLoader;
import draylar.tiered.registry.ModComponents;
import net.minecraft.world.item.ItemStack;

public final class ImprintSlots {

    public static final int UNBOUNDED = -1;

    private ImprintSlots() {
    }

    public static int baseCapacity(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        Integer override = stack.get(ModComponents.IMPRINT_SLOTS);
        if (override != null) return Math.max(0, override);
        return Tiered.IMPRINT_SLOT_LOADER.datapackSlots(stack.getItem());
    }

    public static int maxCapacity(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return UNBOUNDED;
        return Tiered.IMPRINT_SLOT_LOADER.datapackMaxSlots(stack.getItem());
    }

    public static int capacity(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        int total = baseCapacity(stack) + getBonusSlots(stack);
        int max = maxCapacity(stack);
        if (max != UNBOUNDED) total = Math.min(total, max);
        return Math.max(0, total);
    }

    public static boolean canEverHaveSlots(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (baseCapacity(stack) > 0) return true;
        int max = maxCapacity(stack);
        return max != UNBOUNDED && max > 0;
    }

    public static int getBonusSlots(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        Integer bonus = stack.get(ModComponents.BONUS_IMPRINT_SLOTS);
        return bonus == null ? 0 : Math.max(0, bonus);
    }

    public static void setBonusSlots(ItemStack stack, int bonus) {
        if (stack == null || stack.isEmpty()) return;
        int clamped = Math.max(0, bonus);
        int max = maxCapacity(stack);
        if (max != UNBOUNDED) {
            int maxBonus = Math.max(0, max - baseCapacity(stack));
            clamped = Math.min(clamped, maxBonus);
        }
        if (clamped <= 0) stack.remove(ModComponents.BONUS_IMPRINT_SLOTS);
        else stack.set(ModComponents.BONUS_IMPRINT_SLOTS, clamped);
    }

    public static int addBonusSlots(ItemStack stack, int n) {
        if (stack == null || stack.isEmpty() || n == 0) return 0;
        int before = getBonusSlots(stack);
        setBonusSlots(stack, before + n);
        return getBonusSlots(stack) - before;
    }

    public static int bonusHeadroom(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        int max = maxCapacity(stack);
        if (max == UNBOUNDED) return Integer.MAX_VALUE;
        return Math.max(0, max - capacity(stack));
    }
}
