package draylar.tiered.compat;

import dev.architectury.platform.Platform;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.SlotTypeLoader;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Registers the Accessories provider with {@link ATCCompat} so tiered can recognise any item that
 * Accessories treats as an accessory. This is the provider that is active whenever the Accessories
 * mod is present (Trinkets/Curios items are bridged into Accessories by the compat layer), which is
 * why it is registered independently of {@code TrinketsCompat}/{@code CuriosCompat}.
 */
public final class AccessoriesCompat {

    private static final String MOD_ID = "accessories";

    private AccessoriesCompat() {
    }

    public static void init() {
        if (!Platform.isModLoaded(MOD_ID)) return;
        ATCCompat.registerAccessoryItemPredicate(AccessoriesCompat::isAccessoryItem);
        ATCCompat.registerAccessorySlotIdsProvider(AccessoriesCompat::getAllSlotIds);
    }

    private static Set<String> getAllSlotIds() {
        try {
            return new LinkedHashSet<>(SlotTypeLoader.INSTANCE.getSlotTypes(false).keySet());
        } catch (Throwable ignored) {
            return Collections.emptySet();
        }
    }

    @SuppressWarnings("deprecation")
    private static boolean isAccessoryItem(ItemStack stack) {
        if (stack.isEmpty()) return false;

        // Registered as an accessory instance (mirrors how TrinketsCompat checks TrinketsApi.getTrinket).
        try {
            Object accessory = AccessoriesAPI.getAccessory(stack);
            if (accessory != null && accessory != AccessoriesAPI.defaultAccessory()) return true;
        } catch (Throwable ignored) {
        }

        // Tag fallback: the item sits in any accessories:<slot> item tag (e.g. #accessories:ring).
        try {
            Item item = stack.getItem();
            for (SlotType slotType : SlotTypeLoader.INSTANCE.getSlotTypes(false).values()) {
                TagKey<Item> tag = AccessoriesAPI.getSlotTag(slotType);
                if (tag != null && item.builtInRegistryHolder().is(tag)) return true;
            }
        } catch (Throwable ignored) {
        }

        return false;
    }
}
