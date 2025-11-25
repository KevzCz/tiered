package draylar.tiered.api;

import com.google.common.collect.Multimap;
import com.google.gson.annotations.SerializedName;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Stores information on an AttributeModifier template applied to an ItemStack.
 * <p>
 * The ID of the AttributeTemplate is the logical ID used to determine what "type" of attribute of is. An EntityAttributeModifier has: - a UUID, which is a unique identifier to separate different
 * attributes of the same type - a name, which is used for generating a non-specified UUID and displaying in tooltips in some context - an amount, which is used in combination with the operation to
 * modify the final relevant value - a modifier, which can be something such as ADD_VALUE or subtraction
 * <p>
 * The EquipmentSlot is used to only apply this template to certain items.
 */
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

    /**
     * Check if this template should apply to all accessory slots.
     * Returns true if optional_accessories_slots contains "all" or "*"
     */
    public boolean appliesToAllAccessorySlots() {
        if (optionalAccessoriesSlots == null || optionalAccessoriesSlots.length == 0) {
            return false;
        }
        return Arrays.stream(optionalAccessoriesSlots)
                .anyMatch(slot -> slot.equalsIgnoreCase("all") || slot.equals("*"));
    }

    /**
     * Check if this template should apply to a specific accessory slot.
     * Returns true if:
     * - optional_accessories_slots contains "all" or "*", OR
     * - optional_accessories_slots contains the specific slot name
     */
    public boolean appliesToAccessorySlot(String slotName) {
        if (appliesToAllAccessorySlots()) {
            return true;
        }
        if (optionalAccessoriesSlots == null || optionalAccessoriesSlots.length == 0) {
            return false;
        }
        return Arrays.stream(optionalAccessoriesSlots)
                .anyMatch(slot -> slot.equalsIgnoreCase(slotName));
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

    /**
     * Uses this {@link AttributeTemplate} to create an {@link EntityAttributeModifier}, which is placed into the given {@link Multimap}.
     * <p>
     * Note that this method assumes the given {@link Multimap} is mutable.
     *
     * @param attributeConsumer biconsumer to accept {@link AttributeTemplate}
     * @param slot
     */
    public void applyAccessoryModifiers(ItemStack stack, SlotReference ref, BiConsumer<RegistryEntry<EntityAttribute>, EntityAttributeModifier> attributeConsumer) {
        if (!appliesToAccessorySlot(ref.slotName())) {
            return;
        }

        Optional<RegistryEntry.Reference<EntityAttribute>> optional = Registries.ATTRIBUTE.getEntry(Identifier.of(this.attributeTypeID));
        if (optional.isPresent()) {
            EntityAttributeModifier clonedModifier = new EntityAttributeModifier(
                    Identifier.of(entityAttributeModifier.id().getNamespace(),
                            (entityAttributeModifier.id().getPath() + "_" + ref.slotName().toLowerCase())),
                    entityAttributeModifier.value(),
                    entityAttributeModifier.operation()
            );

            attributeConsumer.accept(optional.get(), clonedModifier);
        }
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