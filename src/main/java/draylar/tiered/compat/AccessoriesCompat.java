package draylar.tiered.compat;

import draylar.tiered.api.AttributeTemplate;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.slot.SlotType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

public class AccessoriesCompat {

    public static void applyAccessoryModifiers(ItemStack stack, String slotName, AttributeTemplate template, BiConsumer<RegistryEntry<EntityAttribute>, EntityAttributeModifier> consumer) {
        SlotReference ref = SlotReference.of(null, slotName, 0);
        Optional<RegistryEntry.Reference<EntityAttribute>> optional = Registries.ATTRIBUTE.getEntry(Identifier.of(template.getAttributeTypeID()));
        if (optional.isPresent()) {
            EntityAttributeModifier modifier = new EntityAttributeModifier(
                    Identifier.of(
                            template.getEntityAttributeModifier().id().getNamespace(),
                            template.getEntityAttributeModifier().id().getPath() + "_" + ref.slotName().toLowerCase()
                    ),
                    template.getEntityAttributeModifier().value(),
                    template.getEntityAttributeModifier().operation()
            );
            consumer.accept(optional.get(), modifier);
        }
    }

    public static Set<SlotType> getValidSlotTypes(LivingEntity entity, ItemStack stack) {
        return (Set<SlotType>) AccessoriesAPI.getValidSlotTypes(entity, stack);
    }
}
