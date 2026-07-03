package draylar.tiered.api;

import com.google.common.collect.Multimap;
import com.google.gson.annotations.SerializedName;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.Optional;
import java.util.function.BiConsumer;

public class AttributeTemplate {
    @SerializedName("type")
    private final String attributeTypeID;

    @SerializedName("modifier")
    private final EntityAttributeModifier entityAttributeModifier;

    @SerializedName("required_equipment_slots")
    private final EquipmentSlot[] requiredEquipmentSlots;

    @SerializedName("optional_equipment_slots")
    private final EquipmentSlot[] optionalEquipmentSlots;
    @SerializedName("optional_accessories_slots")
    private final String[] optionalAccessoriesSlots;
    public AttributeTemplate(String attributeTypeID, EntityAttributeModifier entityAttributeModifier, EquipmentSlot[] requiredEquipmentSlots, EquipmentSlot[] optionalEquipmentSlots, String[] optionalAccessoriesSlots) {
        this.attributeTypeID = attributeTypeID;
        this.entityAttributeModifier = entityAttributeModifier;
        this.requiredEquipmentSlots = requiredEquipmentSlots;
        this.optionalEquipmentSlots = optionalEquipmentSlots;
        this.optionalAccessoriesSlots = optionalAccessoriesSlots;
    }
    public String[] getOptionalAccessoriesSlots() {
        return optionalAccessoriesSlots;
    }
    public EquipmentSlot[] getRequiredEquipmentSlots() {
        return requiredEquipmentSlots;
    }

    public EquipmentSlot[] getOptionalEquipmentSlots() {
        return optionalEquipmentSlots;
    }

    public EntityAttributeModifier getEntityAttributeModifier() {
        return entityAttributeModifier;
    }

    public String getAttributeTypeID() {
        return attributeTypeID;
    }

    public boolean isOnlyForAccessories() {
        return (requiredEquipmentSlots == null || requiredEquipmentSlots.length == 0)
                && (optionalEquipmentSlots == null || optionalEquipmentSlots.length == 0)
                && optionalAccessoriesSlots != null && optionalAccessoriesSlots.length > 0;
    }

    public void applyModifiers(EquipmentSlot slot, BiConsumer<RegistryEntry<EntityAttribute>, EntityAttributeModifier> attributeConsumer) {
        Optional<RegistryEntry.Reference<EntityAttribute>> optional = Registries.ATTRIBUTE.getEntry(Identifier.of(this.attributeTypeID));

        if (optional.isPresent()) {
            EntityAttributeModifier cloneModifier = new EntityAttributeModifier(Identifier.of(entityAttributeModifier.id().toString() + "_" + slot.getName()), entityAttributeModifier.value(), entityAttributeModifier.operation());

            AttributeModifiersComponent.Entry entry = new AttributeModifiersComponent.Entry(optional.get(), cloneModifier, AttributeModifierSlot.forEquipmentSlot(slot));
            if (entry.slot().matches(slot)) {
                attributeConsumer.accept(entry.attribute(), entry.modifier());
            }
        }
    }
}
