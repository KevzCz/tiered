package draylar.tiered.api.imprint.behavior;

import java.util.Map;
import net.minecraft.world.item.ItemStack;
import draylar.tiered.api.imprint.ImprintComponent;
import draylar.tiered.api.imprint.MaxDurabilityImprint;
import draylar.tiered.registry.ModComponents;

public class ReinforceBehavior extends ImprintBehavior {

    public static final String ID = "tiered:reinforce";

    @Override
    public float onGrant(ItemStack stack, float rolledValue, Map<String, Float> params) {

        if (!stack.isDamageableItem()) return rolledValue;
        return Math.round(baseMaxDurability(stack) * rolledValue);
    }

    @Override
    public float onExtract(ItemStack stack, float storedValue, Map<String, Float> params) {

        if (!stack.isDamageableItem() || storedValue <= 0f) return storedValue;
        int baseMax = baseMaxDurability(stack);
        return baseMax <= 0 ? storedValue : storedValue / baseMax;
    }

    @Override
    public void onRemove(ItemStack stack, Map<String, Float> params) {

        int max = stack.getMaxDamage();
        if (stack.getDamageValue() > max - 1) {
            stack.setDamageValue(Math.max(0, max - 1));
        }
    }

    private static int baseMaxDurability(ItemStack stack) {
        var imprints = stack.get(ModComponents.IMPRINTS);
        int existingBonus = imprints == null ? 0
                : (int) imprints.valueOf(MaxDurabilityImprint.ID);
        return stack.getMaxDamage() - existingBonus;
    }
}
