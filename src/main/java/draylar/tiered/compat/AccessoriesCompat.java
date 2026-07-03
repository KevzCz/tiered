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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    public static List<ItemStack> getEquippedAccessoryStacksForSlot(LivingEntity entity, String slotName) {
        List<ItemStack> out = new ArrayList<>();
        try {
            Class<?> capClass = Class.forName("io.wispforest.accessories.api.AccessoriesCapability");
            Object capability = capClass.getMethod("get", LivingEntity.class).invoke(null, entity);
            if (capability == null) return out;

            Object containersObj = capClass.getMethod("getContainers").invoke(capability);
            if (!(containersObj instanceof Map<?, ?> containers)) return out;

            for (Map.Entry<?, ?> e : containers.entrySet()) {
                Object slotType = e.getKey();

                String name = (String) slotType.getClass().getMethod("name").invoke(slotType);
                if (!slotName.equals(name)) continue;
                Object container = e.getValue();
                if (container == null) continue;
                Object inventory = container.getClass().getMethod("getAccessories").invoke(container);
                if (inventory == null) continue;
                int size = (int) inventory.getClass().getMethod("size").invoke(inventory);
                Method getStack = inventory.getClass().getMethod("getStack", int.class);
                for (int i = 0; i < size; i++) {
                    Object stackObj = getStack.invoke(inventory, i);
                    if (stackObj instanceof ItemStack stack && !stack.isEmpty()) out.add(stack);
                }
                break;
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    public static List<ItemStack> getEquippedAccessoryStacks(LivingEntity entity) {
        List<ItemStack> out = new ArrayList<>();
        try {

            Class<?> capClass = Class.forName("io.wispforest.accessories.api.AccessoriesCapability");
            Object capability = capClass.getMethod("get", LivingEntity.class).invoke(null, entity);
            if (capability == null) return out;

            Object containersObj = capClass.getMethod("getContainers").invoke(capability);
            if (!(containersObj instanceof Map<?, ?> containers)) return out;

            for (Object container : containers.values()) {
                if (container == null) continue;

                Object inventory = container.getClass().getMethod("getAccessories").invoke(container);
                if (inventory == null) continue;

                int size = (int) inventory.getClass().getMethod("size").invoke(inventory);
                Method getStack = inventory.getClass().getMethod("getStack", int.class);
                for (int i = 0; i < size; i++) {
                    Object stackObj = getStack.invoke(inventory, i);
                    if (stackObj instanceof ItemStack stack && !stack.isEmpty()) {
                        out.add(stack);
                    }
                }
            }
        } catch (Throwable ignored) {

        }
        return out;
    }
}
