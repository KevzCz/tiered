package draylar.tiered.api;

import com.google.common.collect.Multimap;
import com.google.gson.annotations.SerializedName;
import java.util.Optional;
import java.util.function.BiConsumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public class AttributeTemplate {
    @SerializedName("type")
    private final String attributeTypeID;

    @SerializedName("modifier")
    private final AttributeModifier entityAttributeModifier;

    @SerializedName("required_equipment_slots")
    private final EquipmentSlot[] requiredEquipmentSlots;

    @SerializedName("optional_equipment_slots")
    private final EquipmentSlot[] optionalEquipmentSlots;
    @SerializedName("optional_accessories_slots")
    private final String[] optionalAccessoriesSlots;
    public AttributeTemplate(String attributeTypeID, AttributeModifier entityAttributeModifier, EquipmentSlot[] requiredEquipmentSlots, EquipmentSlot[] optionalEquipmentSlots, String[] optionalAccessoriesSlots) {
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

    public AttributeModifier getEntityAttributeModifier() {
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

    public void applyModifiers(EquipmentSlot slot, BiConsumer<Holder<Attribute>, AttributeModifier> attributeConsumer) {
        Optional<Holder.Reference<Attribute>> optional = BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(this.attributeTypeID));

        if (optional.isPresent()) {
            AttributeModifier cloneModifier = new AttributeModifier(ResourceLocation.parse(entityAttributeModifier.id() + "_" + slot.getName()), entityAttributeModifier.amount(), entityAttributeModifier.operation());

            ItemAttributeModifiers.Entry entry = new ItemAttributeModifiers.Entry(optional.get(), cloneModifier, EquipmentSlotGroup.bySlot(slot));
            if (entry.slot().test(slot)) {
                attributeConsumer.accept(entry.attribute(), entry.modifier());
            }
        }
    }
}
