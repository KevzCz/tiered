package draylar.tiered.util;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;

public final class TieredAttributeTooltip {

    private TieredAttributeTooltip() {
    }

    public static void appendTieredSuffixes(MutableComponent text, Holder<Attribute> attribute, List<AttributeModifier> tieredModifiers, ChatFormatting additionColor) {
        for (AttributeModifier tieredModifier : tieredModifiers) {
            double tieredValue;
            if (tieredModifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    || tieredModifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                tieredValue = tieredModifier.amount() * 100.0;
            } else if (attribute.is(Attributes.KNOCKBACK_RESISTANCE)) {
                tieredValue = tieredModifier.amount() * 10.0;
            } else {
                tieredValue = tieredModifier.amount();
            }
            boolean addition = tieredValue > 0;
            text.append(CommonComponents.space());
            text.append(Component.translatable("tiered.attribute.modifier",
                            "(" + (addition ? "+" : "") + ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(tieredValue)
                                    + (tieredModifier.operation().id() > 0 ? "%" : "") + ")")
                    .withStyle(addition ? additionColor : ChatFormatting.RED));
        }
    }
}
