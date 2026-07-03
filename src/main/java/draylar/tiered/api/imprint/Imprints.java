package draylar.tiered.api.imprint;

import java.util.ArrayList;
import java.util.List;

import draylar.tiered.registry.ModComponents;
import draylar.tiered.util.ImprintSlots;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

public final class Imprints {

    private Imprints() {
    }

    public static ImprintComponent get(ItemStack stack) {
        ImprintComponent comp = stack.get(ModComponents.IMPRINTS);
        return comp == null ? ImprintComponent.EMPTY : comp;
    }

    public static List<String> ids(ItemStack stack) {
        return get(stack).ids();
    }

    public static int slotsUsed(ItemStack stack) {
        return get(stack).slotsUsed();
    }

    public static int freeSlots(ItemStack stack) {
        return Math.max(0, ImprintSlots.capacity(stack) - slotsUsed(stack));
    }

    public static boolean hasFreeSlot(ItemStack stack) {
        return freeSlots(stack) > 0;
    }

    public static boolean grantGroup(ItemStack stack, String sourceId, List<ImprintComponent.Entry> group) {
        if (group == null || group.isEmpty()) return false;
        List<ImprintComponent.Entry> usable = new ArrayList<>();
        for (ImprintComponent.Entry e : group) {
            Imprint imprint = ImprintRegistry.get(e.id());
            if (imprint == null) continue;
            float storedValue = e.value();
            if (imprint instanceof DataImprint di && di.behavior() != null) {
                storedValue = di.behavior().onGrant(stack, storedValue, di.params());
            }
            usable.add(storedValue == e.value() ? e
                    : new ImprintComponent.Entry(e.id(), e.count(), storedValue, e.extraValues()));
        }
        if (usable.isEmpty()) return false;

        ImprintComponent current = get(stack);
        ImprintComponent updated = current.withSlot(sourceId, usable, ImprintSlots.capacity(stack));
        if (updated == current) return false;
        stack.set(ModComponents.IMPRINTS, updated);
        return true;
    }

    public static boolean grantSingle(ItemStack stack, String sourceId, String imprintId, float value) {
        return grantGroup(stack, sourceId, List.of(new ImprintComponent.Entry(imprintId, value)));
    }

    public static boolean grantFromContent(ItemStack target, ItemStack source) {
        RuneContentComponent content = source.get(ModComponents.RUNE_CONTENT);
        if (content == null || content.isEmpty()) return false;
        List<ImprintComponent.Entry> group = new ArrayList<>();
        for (RuneContentComponent.Entry e : content.entries()) {
            group.add(new ImprintComponent.Entry(e.imprintId(), 1, e.value(), e.extraValues()));
        }
        String sourceId = Registries.ITEM.getId(source.getItem()).toString();
        return grantGroup(target, sourceId, group);
    }

    public static List<ImprintComponent.Entry> extractLastSlot(ItemStack stack) {
        return extractSlot(stack, get(stack).slots().size() - 1);
    }

    public static List<ImprintComponent.Entry> extractSlot(ItemStack stack, int index) {
        ImprintComponent current = get(stack);
        if (index < 0 || index >= current.slots().size()) return List.of();
        ImprintComponent.Slot slot = current.slots().get(index);
        List<ImprintComponent.Entry> group = new ArrayList<>();
        for (ImprintComponent.Entry e : slot.entries()) {
            Imprint imprint = ImprintRegistry.get(e.id());
            float carried = e.value();
            if (imprint instanceof DataImprint di && di.behavior() != null) {
                carried = di.behavior().onExtract(stack, carried, di.params());
            }
            group.add(carried == e.value() ? e
                    : new ImprintComponent.Entry(e.id(), e.count(), carried, e.extraValues()));
        }
        applyRemoval(stack, current.withoutSlot(index), slotImprintIds(slot));
        return group;
    }

    public static boolean removeSlot(ItemStack stack, int index) {
        ImprintComponent current = get(stack);
        if (index < 0 || index >= current.slots().size()) return false;
        ImprintComponent.Slot removed = current.slots().get(index);
        applyRemoval(stack, current.withoutSlot(index), slotImprintIds(removed));
        return true;
    }

    public static boolean removeSource(ItemStack stack, String sourceId) {
        ImprintComponent current = get(stack);
        ImprintComponent updated = current.withoutSource(sourceId);
        if (updated == current) return false;
        List<String> removedIds = new ArrayList<>(current.ids());
        removedIds.removeAll(updated.ids());
        applyRemoval(stack, updated, removedIds);
        return true;
    }

    public static int forgetAll(ItemStack stack) {
        ImprintComponent current = get(stack);
        int count = current.slotsUsed();
        if (count == 0) return 0;
        applyRemoval(stack, ImprintComponent.EMPTY, current.ids());
        return count;
    }

    private static List<String> slotImprintIds(ImprintComponent.Slot slot) {
        List<String> ids = new ArrayList<>();
        for (ImprintComponent.Entry e : slot.entries()) {
            if (!ids.contains(e.id())) ids.add(e.id());
        }
        return ids;
    }

    private static void applyRemoval(ItemStack stack, ImprintComponent updated, List<String> removedIds) {
        if (updated.slots().isEmpty()) {
            stack.remove(ModComponents.IMPRINTS);
        } else {
            stack.set(ModComponents.IMPRINTS, updated);
        }
        for (String id : removedIds) {
            if (!updated.has(id)) {
                Imprint imprint = ImprintRegistry.get(id);
                if (imprint != null) imprint.onRemove(stack);
            }
        }
    }
}
