package draylar.tiered.neoforge.compat;

import dev.architectury.platform.Platform;
import draylar.tiered.compat.ATCCompat;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Curios is NeoForge-only (no Fabric port) and Accessories already reimplements what it did,
 * but some users run Curios directly without Accessories installed, so tiered still needs to
 * read Curios slots. Registered into {@link ATCCompat} so eligibility/attribute checks
 * see items from either mod, merged, rather than picking a single active provider.
 */
public final class CuriosCompat {

    private static final String MOD_ID = "curios";

    private CuriosCompat() {
    }

    public static void init() {
        if (!Platform.isModLoaded(MOD_ID) || Platform.isModLoaded("accessories")) return;
        ATCCompat.registerExtraProvider(CuriosCompat::getEquippedStacks);
        ATCCompat.registerExtraSlotProvider(CuriosCompat::getEquippedStacksForSlot);
        ATCCompat.registerCurioItemPredicate(CuriosCompat::isCurioItem);
        ATCCompat.registerCurioSlotIdsProvider(CuriosCompat::getAllSlotIds);
    }

    private static Set<String> getAllSlotIds() {
        try {
            Set<String> out = new LinkedHashSet<>();
            for (ISlotType slotType : CuriosApi.getSlots().values()) {
                out.add(slotType.getIdentifier());
            }
            return out;
        } catch (Throwable ignored) {
            return Collections.emptySet();
        }
    }

    // Curios has no umbrella "all curio items" tag of its own - slot definitions are validated
    // per-slot by the built-in "curios:tag" predicate against #curios:<slot identifier>. Rather
    // than hardcoding this mod's shipped slot names (which would silently miss custom slots added
    // by other datapacks/addons), the slot identifiers are enumerated live via CuriosApi.getSlots()
    // - the full slot type registry, independent of any entity/level - and each one's tag checked.
    @SuppressWarnings("deprecation")
    private static boolean isCurioItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        try {
            for (ISlotType slotType : CuriosApi.getSlots().values()) {
                TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("curios", slotType.getIdentifier()));
                if (item.builtInRegistryHolder().is(tag)) return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static List<ItemStack> getEquippedStacks(LivingEntity entity) {
        List<ItemStack> out = new ArrayList<>();
        try {
            Optional<ICuriosItemHandler> handler = CuriosApi.getCuriosInventory(entity);
            if (handler.isEmpty()) return out;
            IItemHandler equipped = handler.get().getEquippedCurios();
            for (int i = 0; i < equipped.getSlots(); i++) {
                ItemStack stack = equipped.getStackInSlot(i);
                if (stack != null && !stack.isEmpty()) out.add(stack);
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private static List<ItemStack> getEquippedStacksForSlot(LivingEntity entity, String slotName) {
        List<ItemStack> out = new ArrayList<>();
        try {
            Optional<ICuriosItemHandler> handler = CuriosApi.getCuriosInventory(entity);
            if (handler.isEmpty()) return out;
            Optional<ICurioStacksHandler> stacksHandler = handler.get().getStacksHandler(slotName);
            if (stacksHandler.isEmpty()) return out;
            IItemHandler stacks = stacksHandler.get().getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                if (stack != null && !stack.isEmpty()) out.add(stack);
            }
        } catch (Throwable ignored) {
        }
        return out;
    }
}
