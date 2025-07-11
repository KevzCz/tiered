package draylar.tiered.mixin;

import draylar.tiered.Tiered;
import draylar.tiered.api.AttributeTemplate;
import draylar.tiered.api.ModifierUtils;
import draylar.tiered.api.PotentialAttribute;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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
    @Inject(method = "getTooltip", at = @At("RETURN"), cancellable = true)
    private void injectCursedTooltip(
            Item.TooltipContext context,
            @Nullable PlayerEntity player,
            TooltipType type,
            CallbackInfoReturnable<List<Text>> cir
    ) {
        ItemStack stack = (ItemStack) (Object) this;

        Identifier attributeId = ModifierUtils.getAttributeId(stack);
        if (attributeId == null) return;

        PotentialAttribute attribute = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(attributeId);
        if (attribute == null || !attribute.isCursed()) return;

        List<Text> tooltip = cir.getReturnValue();

        // Build centered "Cursed" line
        String displayName = formatModifierName(attributeId.getPath());
        MutableText cursedText = Text.translatable("tooltip.tiered.cursed", displayName)
                .formatted(Formatting.DARK_RED, Formatting.BOLD);

        TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
        int maxWidth = tooltip.stream().mapToInt(renderer::getWidth).max().orElse(renderer.getWidth(cursedText));
        int cursedWidth = renderer.getWidth(cursedText);
        int spaceWidth = renderer.getWidth(" ");
        int spaceCount = (maxWidth - cursedWidth) / 2 / spaceWidth;
        String padding = " ".repeat(Math.max(0, spaceCount));
        MutableText centered = Text.literal(padding).append(cursedText);

        // Remove existing "Cursed" lines (in case it's already been inserted)
        tooltip.removeIf(line -> line.getString().equalsIgnoreCase(cursedText.getString()));

        // Insert "Cursed" after the title line (usually first non-empty)
        int insertIndex = -1;
        for (int i = 0; i < tooltip.size(); i++) {
            if (!tooltip.get(i).getString().trim().isEmpty()) {
                insertIndex = i + 1;
                break;
            }
        }

        if (insertIndex == -1 || insertIndex > tooltip.size()) {
            tooltip.add(centered);
        } else {
            tooltip.add(insertIndex, centered);
        }

        // Do not setReturnValue — we modified the original list
    }










    private String formatModifierName(String rawId) {
        String[] parts = rawId.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                builder.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1))
                        .append(" ");
            }
        }
        return builder.toString().trim();
    }
    // Used for tooltip
    @Inject(method = "Lnet/minecraft/item/ItemStack;applyAttributeModifier(Lnet/minecraft/component/type/AttributeModifierSlot;Ljava/util/function/BiConsumer;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/enchantment/EnchantmentHelper;applyAttributeModifiers(Lnet/minecraft/item/ItemStack;Lnet/minecraft/component/type/AttributeModifierSlot;Ljava/util/function/BiConsumer;)V"))
    private void applyAttributeModifierMixin(AttributeModifierSlot slot, BiConsumer<RegistryEntry<EntityAttribute>, EntityAttributeModifier> attributeModifierConsumer, CallbackInfo info) {
        applyAttributeModifier(null, slot, attributeModifierConsumer);
    }

    @Inject(method = "Lnet/minecraft/item/ItemStack;applyAttributeModifiers(Lnet/minecraft/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/enchantment/EnchantmentHelper;applyAttributeModifiers(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V"))
    private void applyAttributeModifiersMixin(EquipmentSlot slot, BiConsumer<RegistryEntry<EntityAttribute>, EntityAttributeModifier> attributeModifierConsumer, CallbackInfo info) {
        applyAttributeModifier(slot, null, attributeModifierConsumer);
    }

    private void applyAttributeModifier(@Nullable EquipmentSlot equipmentSlot, @Nullable AttributeModifierSlot attributeModifierSlot, BiConsumer<RegistryEntry<EntityAttribute>, EntityAttributeModifier> attributeModifierConsumer) {
        ItemStack itemStack = (ItemStack) (Object) this;
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
