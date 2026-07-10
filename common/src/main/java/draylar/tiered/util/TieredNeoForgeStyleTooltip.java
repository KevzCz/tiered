package draylar.tiered.util;

import dev.architectury.platform.Platform;
import draylar.tiered.api.ModifierUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TieredNeoForgeStyleTooltip {

    private TieredNeoForgeStyleTooltip() {
    }

    public static void reformat(List<Component> tooltip, ItemStack stack) {
        if (!Platform.isFabric()) appendAccessoryOnlyLines(tooltip, stack);

        Map<Holder<Attribute>, List<AttributeModifier>> tieredMap = new HashMap<>();
        for (EquipmentSlotGroup slot : EquipmentSlotGroup.values()) {
            boolean[] any = {false};
            stack.forEachModifier(slot, (attribute, modifier) -> {
                any[0] = true;
                if (isImprintModifier(modifier)) return;
                if (!isTieredModifier(modifier)) return;
                if (modifier.amount() > 0.0001D || modifier.amount() < -0.0001D) {
                    tieredMap.computeIfAbsent(attribute, a -> new ArrayList<>()).add(modifier);
                }
            });
            if (any[0]) break;
        }
        if (tieredMap.isEmpty()) return;

        for (Map.Entry<Holder<Attribute>, List<AttributeModifier>> entry : tieredMap.entrySet()) {
            Holder<Attribute> attribute = entry.getKey();
            String descriptionId = attribute.value().getDescriptionId();

            int baseIndex = -1;
            Object baseValueArg = null;
            for (int i = 0; i < tooltip.size(); i++) {
                Object value = getBaseModifierLineValue(tooltip.get(i), descriptionId);
                if (value != null) {
                    baseIndex = i;
                    baseValueArg = value;
                    break;
                }
            }
            if (baseIndex < 0) continue;

            for (int i = tooltip.size() - 1; i >= 0; i--) {
                if (i == baseIndex) continue;
                if (isNeoForgeModifierLineFor(tooltip.get(i), descriptionId)) {
                    tooltip.remove(i);
                    if (i < baseIndex) baseIndex--;
                }
            }

            MutableComponent replacement = CommonComponents.space();
            replacement.append(Component.translatable("tiered.attribute.modifier.equals.0", baseValueArg).withStyle(ChatFormatting.DARK_GREEN));
            TieredAttributeTooltip.appendTieredSuffixes(replacement, attribute, entry.getValue(), ChatFormatting.DARK_GREEN);
            replacement.append(CommonComponents.space());
            replacement.append(Component.translatable(descriptionId).withStyle(ChatFormatting.DARK_GREEN));
            tooltip.set(baseIndex, replacement);
        }
    }

    private static Object getBaseModifierLineValue(Component line, String descriptionId) {
        for (Component sibling : line.getSiblings()) {
            if (!(sibling.getContents() instanceof TranslatableContents contents)) continue;
            if (!contents.getKey().startsWith("attribute.modifier.equals.")) continue;
            Object[] args = contents.getArgs();
            if (args.length != 2 || !(args[1] instanceof Component descComponent)) continue;
            if (!(descComponent.getContents() instanceof TranslatableContents descContents)) continue;
            if (descContents.getKey().equals(descriptionId)) return args[0];
        }
        return null;
    }

    private static boolean isNeoForgeModifierLineFor(Component line, String descriptionId) {
        if (!(line.getContents() instanceof TranslatableContents contents)) return false;
        String key = contents.getKey();
        if (!key.equals("neoforge.modifier.plus") && !key.equals("neoforge.modifier.take")) return false;

        Object[] args = contents.getArgs();
        if (args.length != 2 || !(args[1] instanceof Component descComponent)) return false;
        if (!(descComponent.getContents() instanceof TranslatableContents descContents)) return false;
        return descContents.getKey().equals(descriptionId);
    }

    private static boolean isTieredModifier(AttributeModifier modifier) {
        return "tiered".equals(modifier.id().getNamespace());
    }

    private static boolean isImprintModifier(AttributeModifier modifier) {
        return "tiered".equals(modifier.id().getNamespace()) && modifier.id().getPath().startsWith("imprint_");
    }

    private static void appendAccessoryOnlyLines(List<Component> tooltip, ItemStack stack) {
        for (var entry : ModifierUtils.getAccessoryOnlyModifiers(stack).entrySet()) {
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
                tooltip.add(text);
            }
        }
    }

}
