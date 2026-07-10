package draylar.tiered.mixin;

import draylar.tiered.Tiered;
import draylar.tiered.api.AttributeTemplate;
import draylar.tiered.api.ModifierUtils;
import draylar.tiered.api.PotentialAttribute;
import draylar.tiered.api.SpecialStatsComponent;
import draylar.tiered.api.SpecialStatsComponent.Entry;
import draylar.tiered.api.imprint.ImprintAttributes;
import draylar.tiered.api.imprint.ImprintComponent;
import draylar.tiered.api.imprint.ImprintResolver;
import draylar.tiered.api.imprint.MaxDurabilityImprint;
import draylar.tiered.api.imprint.behavior.StalwartBehavior;
import draylar.tiered.api.imprint.behavior.TemperedBehavior;
import draylar.tiered.compat.ATCCompat;
import draylar.tiered.registry.ModComponents;
import dev.architectury.platform.Platform;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

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

        var imprints = stack.get(ModComponents.IMPRINTS);
        if (imprints != null) {
            float bonus = imprints.valueOf(MaxDurabilityImprint.ID);
            if (bonus > 0) {
                info.setReturnValue(info.getReturnValue() + (int) bonus);
            }
        }
    }

    @ModifyVariable(method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private int tieredDurabilityImprints(int amount, int originalAmount, ServerLevel world, ServerPlayer player, Consumer<Item> breakCallback) {
        if (amount <= 0 || player == null) return amount;

        float tempered = ImprintResolver.resolveValue(player, TemperedBehavior.ID);
        if (tempered > 0f) {
            int reduced = (int) Math.floor(amount * (1.0f - tempered));
            amount = Math.max(reduced, 1);
        }

        float stalwart = ImprintResolver.resolveValue(player, StalwartBehavior.ID);
        if (stalwart > 0f && player.getRandom().nextFloat() < stalwart) {
            return 0;
        }
        return amount;
    }

    @Inject(method = "Lnet/minecraft/world/item/ItemStack;forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Ljava/util/function/BiConsumer;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;forEachModifier(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlotGroup;Ljava/util/function/BiConsumer;)V"))
    private void applyAttributeModifierMixin(EquipmentSlotGroup slot, BiConsumer<Holder<Attribute>, AttributeModifier> attributeModifierConsumer, CallbackInfo info) {
        applyAttributeModifier(null, slot, attributeModifierConsumer);
    }

    @Inject(method = "Lnet/minecraft/world/item/ItemStack;forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;forEachModifier(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V"))
    private void applyAttributeModifiersMixin(EquipmentSlot slot, BiConsumer<Holder<Attribute>, AttributeModifier> attributeModifierConsumer, CallbackInfo info) {
        applyAttributeModifier(slot, null, attributeModifierConsumer);
    }
    private static AttributeModifier.Operation opFromId(int id) {
        if (id < 0 || id > 2) id = 0;
        return AttributeModifier.Operation.BY_ID.apply(id);
    }

    private void applyStatEntry(ItemStack stack, SpecialStatsComponent.Entry e, @Nullable EquipmentSlot equipmentSlot, @Nullable EquipmentSlotGroup attributeModifierSlot, BiConsumer<Holder<Attribute>, AttributeModifier> out) {
        var attrRef = BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(e.attribute()));
        if (attrRef.isEmpty()) return;

        Predicate<EquipmentSlot> slotMatch = s -> {
            if (e.slots().contains("any")) return true;
            return e.slots().stream().anyMatch(n -> n.equalsIgnoreCase(s.getName()));
        };

        if (equipmentSlot != null && slotMatch.test(equipmentSlot) && Tiered.isPreferredEquipmentSlot(stack, equipmentSlot)) {
            AttributeModifier mod = new AttributeModifier(
                    ResourceLocation.parse(e.attribute() + "_" + equipmentSlot.getName() + "_special"),
                    e.value(),
                    opFromId(e.operation())
            );

            out.accept(attrRef.get(), mod);
        } else if (attributeModifierSlot != null && attributeModifierSlot != EquipmentSlotGroup.ANY && attributeModifierSlot != EquipmentSlotGroup.HAND) {
            for (EquipmentSlot s : EquipmentSlot.values()) {
                if (attributeModifierSlot.test(s) && slotMatch.test(s) && Tiered.isPreferredEquipmentSlot(stack, s)) {
                    AttributeModifier mod = new AttributeModifier(
                            ResourceLocation.parse(e.attribute() + "_" + s.getName() + "_special"),
                            e.value(),
                            opFromId(e.operation())
                    );
                    out.accept(attrRef.get(), mod);
                    break;
                }
            }
        }
    }

    private void applyAttributeModifier(@Nullable EquipmentSlot equipmentSlot, @Nullable EquipmentSlotGroup attributeModifierSlot, BiConsumer<Holder<Attribute>, AttributeModifier> attributeModifierConsumer) {
        ItemStack itemStack = (ItemStack) (Object) this;
        if (itemStack.get(Tiered.TIER) != null && "tiered:special".equals(itemStack.get(Tiered.TIER).tier())) {
            var comp = itemStack.get(ModComponents.SPECIAL_STATS);
            if (comp != null) {
                for (var e : comp.specials()) {
                    applyStatEntry(itemStack, e, equipmentSlot, attributeModifierSlot, attributeModifierConsumer);
                }
                applyStatEntry(itemStack, comp.basic(), equipmentSlot, attributeModifierSlot, attributeModifierConsumer);
            }
            return;
        }

        if (itemStack.get(Tiered.TIER) != null) {
            ResourceLocation tier = ModifierUtils.getAttributeId(itemStack);
            PotentialAttribute potentialAttribute = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tier);
            if (potentialAttribute != null) {
                for (AttributeTemplate template : potentialAttribute.getAttributes()) {

                    if (template.getRequiredEquipmentSlots() != null) {
                        List<EquipmentSlot> requiredEquipmentSlots = new ArrayList<>(Arrays.asList(template.getRequiredEquipmentSlots()));

                        if (equipmentSlot != null && requiredEquipmentSlots.contains(equipmentSlot)) {
                            template.applyModifiers(equipmentSlot, attributeModifierConsumer);
                        } else if (attributeModifierSlot != null) {
                            Optional<EquipmentSlot> optional = Arrays.stream(template.getRequiredEquipmentSlots()).filter(attributeModifierSlot::test).findFirst();
                            if (optional.isPresent() && Tiered.isPreferredEquipmentSlot(itemStack, optional.get())) {
                                template.applyModifiers(optional.get(), attributeModifierConsumer);
                            }
                        }
                    }
                    if (template.isOnlyForAccessories()) continue;

                    boolean hasAccessorySlots = Platform.isModLoaded("accessories")
                            || Platform.isModLoaded("trinkets")
                            || Platform.isModLoaded("curios");
                    if (hasAccessorySlots && template.getOptionalAccessoriesSlots() != null) {
                        for (String slotName : ATCCompat.expandSlotNames(template.getOptionalAccessoriesSlots())) {
                            ATCCompat.applyAccessoryModifiers(itemStack, slotName, template, attributeModifierConsumer);
                        }
                    }

                    if (template.getOptionalEquipmentSlots() != null) {
                        List<EquipmentSlot> optionalEquipmentSlots = new ArrayList<>(Arrays.asList(template.getOptionalEquipmentSlots()));

                        if (equipmentSlot != null && optionalEquipmentSlots.contains(equipmentSlot) && Tiered.isPreferredEquipmentSlot(itemStack, equipmentSlot)) {
                            template.applyModifiers(equipmentSlot, attributeModifierConsumer);
                        } else if (attributeModifierSlot != null && attributeModifierSlot != EquipmentSlotGroup.ANY && attributeModifierSlot != EquipmentSlotGroup.HAND) {
                            Optional<EquipmentSlot> optional = Arrays.stream(template.getOptionalEquipmentSlots()).filter(attributeModifierSlot::test).findFirst();
                            if (optional.isPresent() && Tiered.isPreferredEquipmentSlot(itemStack, optional.get())) {
                                template.applyModifiers(optional.get(), attributeModifierConsumer);
                            }
                        }
                    }
                }
            }
        }

        ImprintAttributes.apply(itemStack, equipmentSlot, attributeModifierSlot, attributeModifierConsumer);
    }
}
