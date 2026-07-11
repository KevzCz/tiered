package draylar.tiered.neoforge;

import draylar.tiered.util.TieredNeoForgeStyleTooltip;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public final class TieredAccessoryTooltipNeoForge {

    private TieredAccessoryTooltipNeoForge() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, TieredAccessoryTooltipNeoForge::onItemTooltip);
    }

    private static void onItemTooltip(ItemTooltipEvent event) {
        TieredNeoForgeStyleTooltip.appendAccessoryOnlyLines(event.getToolTip(), event.getItemStack());
    }
}
