package draylar.tiered.registry;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class BlessedScrollItem extends Item {
    public BlessedScrollItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal("Drag the scroll onto the cursed item to undo the curse").formatted(Formatting.GRAY));
        super.appendTooltip(stack, context, tooltip, type);
    }
}
