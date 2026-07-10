package draylar.tiered.registry;

import draylar.tiered.Tiered;
import draylar.tiered.config.ConfigInit;
import draylar.tiered.config.TieredConfig;
import draylar.tiered.config.TuningIngotConfig;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class TuningIngotItem extends Item {
    private final String group;
    private final String color;

    public TuningIngotItem(Properties settings, String group, String color) {
        super(settings);
        this.group = group;
        this.color = color;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag type) {
        String capitalized = group.substring(0, 1).toUpperCase() + group.substring(1);
        tooltip.add(Component.literal(capitalized).withStyle(ChatFormatting.getByName(color.toUpperCase())));

        TieredConfig config = Tiered.CONFIG;
        TuningIngotConfig match = ConfigInit.CUSTOM_TUNING_INGOTS.stream()
                .filter(c -> c.group.equalsIgnoreCase(group))
                .findFirst()
                .orElse(null);

        if (match != null) {
            tooltip.add(Component.literal("Loot Chance: " + (int)(match.lootChance * 100) + "%").withStyle(ChatFormatting.GRAY));
        }

        super.appendHoverText(stack, context, tooltip, type);
    }
}
