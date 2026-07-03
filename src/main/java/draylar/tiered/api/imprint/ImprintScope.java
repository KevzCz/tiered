package draylar.tiered.api.imprint;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.EquipmentSlot;

public final class ImprintScope {

    public static final ImprintScope ANY = new ImprintScope(Set.of(), true, false);

    private final Set<EquipmentSlot> slots;
    private final boolean includeAccessories;
    private final boolean explicit;

    public ImprintScope(Set<EquipmentSlot> slots, boolean includeAccessories, boolean explicit) {
        this.slots = slots;
        this.includeAccessories = includeAccessories;
        this.explicit = explicit;
    }

    public static ImprintScope fromSlots(List<String> requiredSlots, List<String> optionalSlots, List<String> accessoriesSlots) {
        Set<EquipmentSlot> set = new HashSet<>();
        addSlots(set, requiredSlots);
        addSlots(set, optionalSlots);
        boolean explicit = !set.isEmpty();
        if (!explicit) return ANY;
        boolean accessories = accessoriesSlots == null || !accessoriesSlots.isEmpty();
        return new ImprintScope(set, accessories, true);
    }

    private static void addSlots(Set<EquipmentSlot> set, List<String> names) {
        if (names == null) return;
        for (String n : names) {
            EquipmentSlot s = parseSlot(n);
            if (s != null) set.add(s);
        }
    }

    public boolean includesEquipment(EquipmentSlot slot) {
        return slots.isEmpty() || slots.contains(slot);
    }

    public boolean includesAccessories() {
        return includeAccessories;
    }

    private static EquipmentSlot parseSlot(String name) {
        if (name == null) return null;
        return switch (name.toLowerCase()) {
            case "mainhand", "main_hand" -> EquipmentSlot.MAINHAND;
            case "offhand", "off_hand" -> EquipmentSlot.OFFHAND;
            case "head", "helmet" -> EquipmentSlot.HEAD;
            case "chest", "chestplate" -> EquipmentSlot.CHEST;
            case "legs", "leggings" -> EquipmentSlot.LEGS;
            case "feet", "boots" -> EquipmentSlot.FEET;
            default -> null;
        };
    }
}
