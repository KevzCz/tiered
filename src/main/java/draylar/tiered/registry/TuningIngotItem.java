package draylar.tiered.registry;

import draylar.tiered.Tiered;
import draylar.tiered.config.ConfigInit;
import draylar.tiered.config.TieredConfig;
import draylar.tiered.config.TuningIngotConfig;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;

public class TuningIngotItem extends Item {
    private final String group;
    private final String color;

    public TuningIngotItem(Settings settings, String group, String color) {
        super(settings);
        this.group = group;
        this.color = color;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        String capitalized = group.substring(0, 1).toUpperCase() + group.substring(1);
        tooltip.add(Text.literal(capitalized).formatted(Formatting.byName(color.toUpperCase())));

        TieredConfig config = Tiered.CONFIG;
        TuningIngotConfig match = ConfigInit.CUSTOM_TUNING_INGOTS.stream()
                .filter(c -> c.group.equalsIgnoreCase(group))
                .findFirst()
                .orElse(null);


        if (match != null) {
            tooltip.add(Text.literal("Loot Chance: " + (int)(match.lootChance * 100) + "%").formatted(Formatting.GRAY));
        }

        super.appendTooltip(stack, context, tooltip, type);
    }

    public String getGroup() {
        return group;
    }

    public String getColor() {
        return color;
    }
}


