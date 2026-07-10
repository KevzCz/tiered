package draylar.tiered.api;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record ReforgeMaterialBadgeData(List<Badge> badges, List<Component> lore, boolean showDescriptions, boolean compact) implements TooltipComponent {

    public ReforgeMaterialBadgeData(List<Badge> badges, List<Component> lore, boolean showDescriptions) {
        this(badges, lore, showDescriptions, false);
    }

    public record Badge(String glyph, Component label, int fillColor, Component description) {
    }
}
