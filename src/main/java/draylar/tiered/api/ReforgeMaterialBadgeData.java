package draylar.tiered.api;

import java.util.List;

import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.text.Text;

public record ReforgeMaterialBadgeData(List<Badge> badges, List<Text> lore, boolean showDescriptions, boolean compact) implements TooltipData {

    public ReforgeMaterialBadgeData(List<Badge> badges, List<Text> lore, boolean showDescriptions) {
        this(badges, lore, showDescriptions, false);
    }

    public record Badge(String glyph, Text label, int fillColor, Text description) {
    }
}
