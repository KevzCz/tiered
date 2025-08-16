package draylar.tiered.mixin;

import draylar.tiered.Tiered;
import draylar.tiered.api.AttributeTemplate;
import draylar.tiered.api.ModifierUtils;
import draylar.tiered.api.PotentialAttribute;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(method = "getMaxDamage", at = @At("TAIL"), cancellable = true)
    private void getMaxDamageMixin(CallbackInfoReturnable<Integer> info) {
        ItemStack stack = (ItemStack) (Object) this;
        if (stack.get(Tiered.TIER) != null && stack.get(Tiered.TIER).durable() > 0) {
            if (stack.get(Tiered.TIER).operation() == 0) {
                info.setReturnValue(info.getReturnValue() + (int) stack.get(Tiered.TIER).durable());
            } else {
                info.setReturnValue(info.getReturnValue() + (int) ((float) info.getReturnValue() * stack.get(Tiered.TIER).durable()));
            }
        }
    }
    @Inject(method = "Lnet/minecraft/item/ItemStack;applyAttributeModifier(Lnet/minecraft/component/type/AttributeModifierSlot;Ljava/util/function/BiConsumer;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/enchantment/EnchantmentHelper;applyAttributeModifiers(Lnet/minecraft/item/ItemStack;Lnet/minecraft/component/type/AttributeModifierSlot;Ljava/util/function/BiConsumer;)V"))
    private void applyAttributeModifierMixin(AttributeModifierSlot slot, BiConsumer<RegistryEntry<EntityAttribute>, EntityAttributeModifier> attributeModifierConsumer, CallbackInfo info) {
        applyAttributeModifier(null, slot, attributeModifierConsumer);
    }

    @Inject(method = "Lnet/minecraft/item/ItemStack;applyAttributeModifiers(Lnet/minecraft/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/enchantment/EnchantmentHelper;applyAttributeModifiers(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V"))
    private void applyAttributeModifiersMixin(EquipmentSlot slot, BiConsumer<RegistryEntry<EntityAttribute>, EntityAttributeModifier> attributeModifierConsumer, CallbackInfo info) {
        applyAttributeModifier(slot, null, attributeModifierConsumer);
    }
    private static EntityAttributeModifier.Operation opFromId(int id) {
        if (id < 0 || id > 2) id = 0;
        return EntityAttributeModifier.Operation.ID_TO_VALUE.apply(id);
    }

    private void applyStatEntry(ItemStack stack, draylar.tiered.api.SpecialStatsComponent.Entry e, @Nullable EquipmentSlot equipmentSlot, @Nullable AttributeModifierSlot attributeModifierSlot, BiConsumer<RegistryEntry<EntityAttribute>, EntityAttributeModifier> out) {
        var attrRef = net.minecraft.registry.Registries.ATTRIBUTE.getEntry(Identifier.of(e.attribute()));
        if (attrRef.isEmpty()) return;

        java.util.function.Predicate<EquipmentSlot> slotMatch = s -> {
            if (e.slots().contains("any")) return true;
            return e.slots().stream().anyMatch(n -> n.equalsIgnoreCase(s.getName()));
        };

        if (equipmentSlot != null && slotMatch.test(equipmentSlot) && Tiered.isPreferredEquipmentSlot(stack, equipmentSlot)) {
            EntityAttributeModifier mod = new EntityAttributeModifier(
                    Identifier.of(e.attribute() + "_" + equipmentSlot.getName() + "_special"),
                    e.value(),
                    opFromId(e.operation())
            );

            out.accept(attrRef.get(), mod);
        } else if (attributeModifierSlot != null && attributeModifierSlot != AttributeModifierSlot.ANY && attributeModifierSlot != AttributeModifierSlot.HAND) {
            for (EquipmentSlot s : EquipmentSlot.values()) {
                if (attributeModifierSlot.matches(s) && slotMatch.test(s) && Tiered.isPreferredEquipmentSlot(stack, s)) {
                    EntityAttributeModifier mod = new EntityAttributeModifier(
                            Identifier.of(e.attribute() + "_" + s.getName() + "_special"),
                            e.value(),
                            opFromId(e.operation())
                    );
                    out.accept(attrRef.get(), mod);
                    break;
                }
            }
        }
    }

    private void applyAttributeModifier(@Nullable EquipmentSlot equipmentSlot, @Nullable AttributeModifierSlot attributeModifierSlot, BiConsumer<RegistryEntry<EntityAttribute>, EntityAttributeModifier> attributeModifierConsumer) {
        ItemStack itemStack = (ItemStack) (Object) this;
        if (itemStack.get(Tiered.TIER) != null && "tiered:special".equals(itemStack.get(Tiered.TIER).tier())) {
            var comp = itemStack.get(draylar.tiered.registry.ModComponents.SPECIAL_STATS);
            if (comp != null) {
                for (var e : comp.specials()) {
                    applyStatEntry(itemStack, e, equipmentSlot, attributeModifierSlot, attributeModifierConsumer);
                }
                applyStatEntry(itemStack, comp.basic(), equipmentSlot, attributeModifierSlot, attributeModifierConsumer);
            }
            return;
        }

        if (itemStack.get(Tiered.TIER) != null) {
            Identifier tier = ModifierUtils.getAttributeId(itemStack);
            PotentialAttribute potentialAttribute = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tier);
            if (potentialAttribute != null) {
                for (AttributeTemplate template : potentialAttribute.getAttributes()) {
                    // get required equipment slots
                    if (template.getRequiredEquipmentSlots() != null) {
                        List<EquipmentSlot> requiredEquipmentSlots = new ArrayList<>(Arrays.asList(template.getRequiredEquipmentSlots()));

                        if (equipmentSlot != null && requiredEquipmentSlots.contains(equipmentSlot)) {
                            template.applyModifiers(equipmentSlot, attributeModifierConsumer);
                        } else if (attributeModifierSlot != null) {
                            Optional<EquipmentSlot> optional = Arrays.stream(template.getRequiredEquipmentSlots()).filter(attributeModifierSlot::matches).findFirst();
                            if (optional.isPresent() && Tiered.isPreferredEquipmentSlot(itemStack, optional.get())) {
                                template.applyModifiers(optional.get(), attributeModifierConsumer);
                            }
                        }
                    }
                    if (template.isOnlyForAccessories()) continue;

                    if (template.getOptionalAccessoriesSlots() != null) {
                        for (String slotName : template.getOptionalAccessoriesSlots()) {
                            SlotReference ref = SlotReference.of(null, slotName, 0);
                            template.applyAccessoryModifiers(itemStack, ref, attributeModifierConsumer);
                        }
                    }

                    // get optional equipment slots
                    if (template.getOptionalEquipmentSlots() != null) {
                        List<EquipmentSlot> optionalEquipmentSlots = new ArrayList<>(Arrays.asList(template.getOptionalEquipmentSlots()));
                        // optional equipment slots are valid ONLY IF the equipment slot is valid for the thing
                        if (equipmentSlot != null && optionalEquipmentSlots.contains(equipmentSlot) && Tiered.isPreferredEquipmentSlot(itemStack, equipmentSlot)) {
                            template.applyModifiers(equipmentSlot, attributeModifierConsumer);
                        } else if (attributeModifierSlot != null && attributeModifierSlot != AttributeModifierSlot.ANY && attributeModifierSlot != AttributeModifierSlot.HAND) {
                            Optional<EquipmentSlot> optional = Arrays.stream(template.getOptionalEquipmentSlots()).filter(attributeModifierSlot::matches).findFirst();
                            if (optional.isPresent() && Tiered.isPreferredEquipmentSlot(itemStack, optional.get())) {
                                template.applyModifiers(optional.get(), attributeModifierConsumer);
                            }
                        }
                    }
                }
            }
        }
    }
}
