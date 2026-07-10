package draylar.tiered.api.imprint;

import draylar.tiered.api.AttributeTemplate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public record ImprintAttribute(
        String attributeTypeId,
        ResourceLocation modifierId,
        double value,
        AttributeModifier.Operation operation,
        EquipmentSlot[] requiredSlots,
        EquipmentSlot[] optionalSlots,
        String[] accessoriesSlots,
        boolean anyWornSlot) {

    public static final String[] ALL_ACCESSORIES = {"*"};

    public static ImprintAttribute anyWorn(String attributeTypeId, ResourceLocation modifierId, double value,
            AttributeModifier.Operation operation) {
        return new ImprintAttribute(attributeTypeId, modifierId, value, operation, null, null, ALL_ACCESSORIES, true);
    }

    public boolean isOnlyForAccessories() {
        return !anyWornSlot
                && (requiredSlots == null || requiredSlots.length == 0)
                && (optionalSlots == null || optionalSlots.length == 0)
                && accessoriesSlots != null && accessoriesSlots.length > 0;
    }

    public AttributeModifier modifierForSlot(EquipmentSlot slot) {
        return new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath(modifierId.getNamespace(), modifierId.getPath() + "_" + slot.getName()),
                value, operation);
    }

    public AttributeModifier modifierForAccessory(String slotName) {
        String safeName = slotName.toLowerCase().replace(':', '_').replaceAll("[^a-z0-9/._-]", "_");
        return new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath(modifierId.getNamespace(), modifierId.getPath() + "_" + safeName),
                value, operation);
    }
}
