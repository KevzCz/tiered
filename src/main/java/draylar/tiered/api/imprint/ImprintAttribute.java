package draylar.tiered.api.imprint;

import draylar.tiered.api.AttributeTemplate;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.util.Identifier;

public record ImprintAttribute(
        String attributeTypeId,
        Identifier modifierId,
        double value,
        EntityAttributeModifier.Operation operation,
        EquipmentSlot[] requiredSlots,
        EquipmentSlot[] optionalSlots,
        String[] accessoriesSlots,
        boolean anyWornSlot) {

    public static final String[] ALL_ACCESSORIES = {"*"};

    public static ImprintAttribute anyWorn(String attributeTypeId, Identifier modifierId, double value,
            EntityAttributeModifier.Operation operation) {
        return new ImprintAttribute(attributeTypeId, modifierId, value, operation, null, null, ALL_ACCESSORIES, true);
    }

    public boolean isOnlyForAccessories() {
        return !anyWornSlot
                && (requiredSlots == null || requiredSlots.length == 0)
                && (optionalSlots == null || optionalSlots.length == 0)
                && accessoriesSlots != null && accessoriesSlots.length > 0;
    }

    public EntityAttributeModifier modifierForSlot(EquipmentSlot slot) {
        return new EntityAttributeModifier(
                Identifier.of(modifierId.getNamespace(), modifierId.getPath() + "_" + slot.getName()),
                value, operation);
    }

    public EntityAttributeModifier modifierForAccessory(String slotName) {
        String safeName = slotName.toLowerCase().replace(':', '_').replaceAll("[^a-z0-9/._-]", "_");
        return new EntityAttributeModifier(
                Identifier.of(modifierId.getNamespace(), modifierId.getPath() + "_" + safeName),
                value, operation);
    }
}
