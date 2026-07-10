package draylar.tiered.compat;

import draylar.tiered.api.AttributeTemplate;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.slot.SlotType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

public class ATCCompat {

    public static final String ALL_ACCESSORIES_SLOTS = "all_accessories";
    public static final String ALL_TRINKETS_SLOTS = "all_trinkets";
    public static final String ALL_CURIOS_SLOTS = "all_curios";

    private static final List<Function<LivingEntity, List<ItemStack>>> EXTRA_PROVIDERS = new ArrayList<>();
    private static final List<BiFunction<LivingEntity, String, List<ItemStack>>> EXTRA_SLOT_PROVIDERS = new ArrayList<>();

    private static Predicate<ItemStack> trinketItemPredicate = stack -> false;
    private static Predicate<ItemStack> curioItemPredicate = stack -> false;

    private static Supplier<Set<String>> trinketSlotIdsProvider = Collections::emptySet;
    private static Supplier<Set<String>> curioSlotIdsProvider = Collections::emptySet;

    public static void registerExtraProvider(Function<LivingEntity, List<ItemStack>> provider) {
        EXTRA_PROVIDERS.add(provider);
    }

    public static void registerExtraSlotProvider(BiFunction<LivingEntity, String, List<ItemStack>> provider) {
        EXTRA_SLOT_PROVIDERS.add(provider);
    }

    public static void registerTrinketItemPredicate(Predicate<ItemStack> predicate) {
        trinketItemPredicate = predicate;
    }

    public static void registerCurioItemPredicate(Predicate<ItemStack> predicate) {
        curioItemPredicate = predicate;
    }

    public static void registerTrinketSlotIdsProvider(Supplier<Set<String>> provider) {
        trinketSlotIdsProvider = provider;
    }

    public static void registerCurioSlotIdsProvider(Supplier<Set<String>> provider) {
        curioSlotIdsProvider = provider;
    }

    public static Set<String> getAccessorylessSlotIds() {
        Set<String> out = new LinkedHashSet<>();
        out.addAll(trinketSlotIdsProvider.get());
        out.addAll(curioSlotIdsProvider.get());
        return out;
    }

    public static boolean isAnyTrinketItem(ItemStack stack) {
        return trinketItemPredicate.test(stack);
    }

    public static boolean isAnyCurioItem(ItemStack stack) {
        return curioItemPredicate.test(stack);
    }

    public static Set<String> expandSlotNames(String[] configuredSlots) {
        Set<String> out = new LinkedHashSet<>();
        if (configuredSlots == null) return out;
        for (String slotName : configuredSlots) {
            if (slotName == null) continue;
            if (slotName.equalsIgnoreCase(ALL_ACCESSORIES_SLOTS)) {
                out.addAll(getAllAccessoriesSlotIds());
                out.addAll(trinketSlotIdsProvider.get());
                out.addAll(curioSlotIdsProvider.get());
            } else if (slotName.equalsIgnoreCase(ALL_TRINKETS_SLOTS)) {
                out.addAll(trinketSlotIdsProvider.get());
            } else if (slotName.equalsIgnoreCase(ALL_CURIOS_SLOTS)) {
                out.addAll(curioSlotIdsProvider.get());
            } else {
                out.add(slotName);
            }
        }
        return out;
    }

    public static boolean matchesSlotName(String configuredSlot, String actualSlotName) {
        if (configuredSlot == null || actualSlotName == null) return false;
        if (configuredSlot.equalsIgnoreCase(ALL_ACCESSORIES_SLOTS)) return true;
        if (configuredSlot.equalsIgnoreCase(ALL_TRINKETS_SLOTS)) {
            return trinketSlotIdsProvider.get().stream().anyMatch(actualSlotName::equalsIgnoreCase);
        }
        if (configuredSlot.equalsIgnoreCase(ALL_CURIOS_SLOTS)) {
            return curioSlotIdsProvider.get().stream().anyMatch(actualSlotName::equalsIgnoreCase);
        }
        return configuredSlot.equalsIgnoreCase(actualSlotName);
    }

    private static Set<String> getAllAccessoriesSlotIds() {
        try {
            return new LinkedHashSet<>(io.wispforest.accessories.data.SlotTypeLoader.INSTANCE.getSlotTypes(false).keySet());
        } catch (Throwable ignored) {
            return Collections.emptySet();
        }
    }

    public static void applyAccessoryModifiers(ItemStack stack, String slotName, AttributeTemplate template, BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
        SlotReference ref = SlotReference.of(null, slotName, 0);
        Optional<Holder.Reference<Attribute>> optional = BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(template.getAttributeTypeID()));
        if (optional.isPresent()) {
            AttributeModifier modifier = new AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath(
                            template.getEntityAttributeModifier().id().getNamespace(),
                            template.getEntityAttributeModifier().id().getPath() + "_" + ref.slotName().toLowerCase()
                    ),
                    template.getEntityAttributeModifier().amount(),
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
            if (capability != null) {
                Object containersObj = capClass.getMethod("getContainers").invoke(capability);
                if (containersObj instanceof Map<?, ?> containers) {
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
                }
            }
        } catch (Throwable ignored) {
        }

        for (BiFunction<LivingEntity, String, List<ItemStack>> provider : EXTRA_SLOT_PROVIDERS) {
            try {
                List<ItemStack> extra = provider.apply(entity, slotName);
                if (extra != null) out.addAll(extra);
            } catch (Throwable ignored) {
            }
        }
        return out;
    }

    public static List<ItemStack> getEquippedAccessoryStacks(LivingEntity entity) {
        List<ItemStack> out = new ArrayList<>();
        try {
            Class<?> capClass = Class.forName("io.wispforest.accessories.api.AccessoriesCapability");
            Object capability = capClass.getMethod("get", LivingEntity.class).invoke(null, entity);
            if (capability != null) {
                Object containersObj = capClass.getMethod("getContainers").invoke(capability);
                if (containersObj instanceof Map<?, ?> containers) {
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
                }
            }
        } catch (Throwable ignored) {
        }

        for (Function<LivingEntity, List<ItemStack>> provider : EXTRA_PROVIDERS) {
            try {
                List<ItemStack> extra = provider.apply(entity);
                if (extra != null) out.addAll(extra);
            } catch (Throwable ignored) {
            }
        }
        return out;
    }
}
