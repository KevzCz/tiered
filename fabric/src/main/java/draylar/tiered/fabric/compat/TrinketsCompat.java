package draylar.tiered.fabric.compat;

import dev.architectury.platform.Platform;
import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketsApi;
import draylar.tiered.compat.ATCCompat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class TrinketsCompat {

    private static final String MOD_ID = "trinkets";

    private TrinketsCompat() {
    }

    public static void init() {
        if (!Platform.isModLoaded(MOD_ID) || Platform.isModLoaded("accessories")) return;
        ATCCompat.registerExtraProvider(TrinketsCompat::getEquippedStacks);
        ATCCompat.registerExtraSlotProvider(TrinketsCompat::getEquippedStacksForSlot);
        ATCCompat.registerTrinketItemPredicate(TrinketsCompat::isTrinketItem);
        ATCCompat.registerTrinketSlotIdsProvider(TrinketsCompat::getAllSlotIds);
    }

    private static Set<String> getAllSlotIds() {
        try {
            Set<String> out = new LinkedHashSet<>();
            for (SlotGroup group : TrinketsApi.getEntitySlots(EntityType.PLAYER).values()) {
                for (SlotType slotType : group.getSlots().values()) {
                    out.add(slotType.getName());
                }
            }
            return out;
        } catch (Throwable ignored) {
            return Collections.emptySet();
        }
    }

    @SuppressWarnings("deprecation")
    private static boolean isTrinketItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();

        try {
            Trinket registered = TrinketsApi.getTrinket(item);
            if (registered != null && registered != TrinketsApi.getDefaultTrinket()) return true;
        } catch (Throwable ignored) {
        }

        try {
            return item.builtInRegistryHolder().tags()
                    .anyMatch(tag -> "trinkets".equals(tag.location().getNamespace()));
        } catch (Throwable ignored) {
        }

        return false;
    }

    private static List<ItemStack> getEquippedStacks(LivingEntity entity) {
        List<ItemStack> out = new ArrayList<>();
        try {
            Optional<TrinketComponent> component = TrinketsApi.getTrinketComponent(entity);
            if (component.isEmpty()) return out;
            for (Tuple<SlotReference, ItemStack> pair : component.get().getAllEquipped()) {
                ItemStack stack = pair.getB();
                if (stack != null && !stack.isEmpty()) out.add(stack);
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private static List<ItemStack> getEquippedStacksForSlot(LivingEntity entity, String slotName) {
        List<ItemStack> out = new ArrayList<>();
        try {
            Optional<TrinketComponent> component = TrinketsApi.getTrinketComponent(entity);
            if (component.isEmpty()) return out;
            for (Tuple<SlotReference, ItemStack> pair : component.get().getAllEquipped()) {
                if (!slotName.equals(pair.getA().inventory().getSlotType().getName())) continue;
                ItemStack stack = pair.getB();
                if (stack != null && !stack.isEmpty()) out.add(stack);
            }
        } catch (Throwable ignored) {
        }
        return out;
    }
}
