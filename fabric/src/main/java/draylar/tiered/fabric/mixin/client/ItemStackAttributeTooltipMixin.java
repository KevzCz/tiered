package draylar.tiered.fabric.mixin.client;

import draylar.tiered.Tiered;
import draylar.tiered.api.ModifierUtils;
import draylar.tiered.util.TieredAttributeTooltip;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
@Mixin(ItemStack.class)
public abstract class ItemStackAttributeTooltipMixin {

    @Unique
    private boolean isTiered;
    @Unique
    private boolean slotInfo;
    @Unique
    private final Map<Holder<Attribute>, List<AttributeModifier>> tieredMap = new HashMap<>();

    @Inject(method = "addAttributeTooltips", at = @At("HEAD"))
    private void appendAttributeModifiersTooltipMixin(Consumer<Component> textConsumer, @Nullable Player player, CallbackInfo info) {
        ItemStack itemStack = (ItemStack) (Object) this;
        if (itemStack.get(Tiered.TIER) != null) {
            this.isTiered = true;
            this.tieredMap.clear();

            for (EquipmentSlotGroup attributeModifierSlot : EquipmentSlotGroup.values()) {
                MutableBoolean mutableBoolean = new MutableBoolean(false);
                this.forEachModifier(attributeModifierSlot, (attribute, modifier) -> {

                    if (tiered$isImprintModifier(modifier)) {
                        return;
                    }
                    List<AttributeModifier> modifiers;
                    if (this.tieredMap.containsKey(attribute)) {
                        modifiers = this.tieredMap.get(attribute);
                        modifiers.add(modifier);
                    } else {
                        modifiers = new ArrayList<>();
                    }
                    if (modifier.amount() > 0.0001D || modifier.amount() < -0.0001D) {
                        this.tieredMap.put(attribute, modifiers);
                    }
                    mutableBoolean.setValue(true);
                });
                if (mutableBoolean.getValue()) {
                    break;
                }
            }
        }
        this.slotInfo = true;
    }

    @Inject(method = "addAttributeTooltips", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Ljava/util/function/BiConsumer;)V"), cancellable = true)
    private void appendAttributeModifiersTooltipTwoMixin(Consumer<Component> textConsumer, @Nullable Player player, CallbackInfo info) {
        if (this.isTiered && !this.slotInfo) {
            info.cancel();
        }
    }

    @Inject(method = "addAttributeTooltips", at = @At("RETURN"))
    private void appendAccessoryOnlyAttributeTooltipsMixin(Consumer<Component> textConsumer, @Nullable Player player, CallbackInfo info) {
        ItemStack itemStack = (ItemStack) (Object) this;
        if (itemStack.get(Tiered.TIER) == null) return;

        for (var entry : ModifierUtils.getAccessoryOnlyModifiers(itemStack).entrySet()) {
            Holder<Attribute> attribute = entry.getKey();
            for (AttributeModifier modifier : entry.getValue()) {
                double value = modifier.operation() == AttributeModifier.Operation.ADD_VALUE
                        ? modifier.amount()
                        : modifier.amount() * 100.0;
                boolean addition = value > 0;

                MutableComponent text = Component.translatable(
                                (addition ? "tiered.attribute.modifier.plus." : "tiered.attribute.modifier.take.") + modifier.operation().id(),
                                ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(Math.abs(value)))
                        .withStyle(attribute.value().getStyle(addition));
                text.append(CommonComponents.space());
                text.append(Component.translatable(attribute.value().getDescriptionId()).withStyle(attribute.value().getStyle(addition)));
                textConsumer.accept(text);
            }
        }
    }

    @Inject(method = "addModifierTooltip", at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V", ordinal = 0), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    @Desc(value = "addModifierTooltip", args = { Consumer.class, Player.class, Holder.class, AttributeModifier.class }, ret = void.class)
    private void appendAttributeModifierTooltipMixin(Consumer<Component> textConsumer, @Nullable Player player, Holder<Attribute> attribute, AttributeModifier modifier, CallbackInfo info, double d, boolean bl, double e) {
        if (this.isTiered) {
            this.slotInfo = false;
        }
        if (tiered$isImprintModifier(modifier)) { info.cancel(); return; }
        if (this.isTiered && this.tieredMap.containsKey(attribute)) {
            MutableComponent text = CommonComponents.space();
            text.append(Component.translatable(
                            "tiered.attribute.modifier.equals." + modifier.operation().id(),
                            ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(e))
                    .withStyle(ChatFormatting.DARK_GREEN));
            for (int i = 0; i < this.tieredMap.get(attribute).size(); i++) {
                if (this.tieredMap.get(attribute).get(i).is(modifier.id())) {
                    info.cancel();
                    return;
                }
            }
            TieredAttributeTooltip.appendTieredSuffixes(text, attribute, this.tieredMap.get(attribute), ChatFormatting.DARK_GREEN);
            text.append(CommonComponents.space());
            text.append(Component.translatable(attribute.value().getDescriptionId()).withStyle(ChatFormatting.DARK_GREEN));
            textConsumer.accept(text);
            info.cancel();
        }
    }

    @Inject(method = "addModifierTooltip", at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V", ordinal = 1), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    @Desc(value = "addModifierTooltip", args = { Consumer.class, Player.class, Holder.class, AttributeModifier.class }, ret = void.class)
    private void appendAttributeModifierTooltipTwoMixin(Consumer<Component> textConsumer, @Nullable Player player, Holder<Attribute> attribute, AttributeModifier modifier, CallbackInfo info, double d, boolean bl, double e) {
        if (this.isTiered) {
            this.slotInfo = false;
        }
        if (tiered$isImprintModifier(modifier)) { info.cancel(); return; }
        if (this.isTiered && this.tieredMap.containsKey(attribute)) {
            MutableComponent text = Component.translatable(
                            "tiered.attribute.modifier.plus." + modifier.operation().id(),
                            ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(e))
                    .withStyle(attribute.value().getStyle(true));
            for (int i = 0; i < this.tieredMap.get(attribute).size(); i++) {
                if (this.tieredMap.get(attribute).get(i).is(modifier.id())) {
                    info.cancel();
                    return;
                }
            }
            TieredAttributeTooltip.appendTieredSuffixes(text, attribute, this.tieredMap.get(attribute), ChatFormatting.BLUE);
            text.append(CommonComponents.space());
            text.append(Component.translatable(attribute.value().getDescriptionId()).withStyle(attribute.value().getStyle(true)));
            textConsumer.accept(text);
            info.cancel();
        }
    }

    @Inject(method = "addModifierTooltip", at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V", ordinal = 2), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    @Desc(value = "addModifierTooltip", args = { Consumer.class, Player.class, Holder.class, AttributeModifier.class }, ret = void.class)
    private void appendAttributeModifierTooltipThreeMixin(Consumer<Component> textConsumer, @Nullable Player player, Holder<Attribute> attribute, AttributeModifier modifier, CallbackInfo info, double d, boolean bl, double e) {
        if (this.isTiered) {
            this.slotInfo = false;
        }
        if (tiered$isImprintModifier(modifier)) { info.cancel(); return; }
        if (this.isTiered && this.tieredMap.containsKey(attribute)) {
            MutableComponent text = Component.translatable(
                            "tiered.attribute.modifier.take." + modifier.operation().id(),
                            ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(-e))
                    .withStyle(attribute.value().getStyle(false));
            for (int i = 0; i < this.tieredMap.get(attribute).size(); i++) {
                if (this.tieredMap.get(attribute).get(i).is(modifier.id())) {
                    info.cancel();
                    return;
                }
            }
            TieredAttributeTooltip.appendTieredSuffixes(text, attribute, this.tieredMap.get(attribute), ChatFormatting.BLUE);
            text.append(CommonComponents.space());
            text.append(Component.translatable(attribute.value().getDescriptionId()).withStyle(ChatFormatting.RED));
            textConsumer.accept(text);
            info.cancel();
        }
    }

    @Unique
    private static boolean tiered$isImprintModifier(AttributeModifier modifier) {
        return "tiered".equals(modifier.id().getNamespace()) && modifier.id().getPath().startsWith("imprint_");
    }

    @Shadow
    public abstract void forEachModifier(EquipmentSlotGroup slot, BiConsumer<Holder<Attribute>, AttributeModifier> attributeModifierConsumer);
}
