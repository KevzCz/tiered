package draylar.tiered.neoforge;

import draylar.tiered.util.AttributeHelper;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class PlayerDigSpeedNeoForge {

    public static void register() {
        NeoForge.EVENT_BUS.addListener(PlayerDigSpeedNeoForge::onBreakSpeed);
    }

    private static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        event.setNewSpeed(AttributeHelper.getExtraDigSpeed(player, event.getNewSpeed()));
    }
}
