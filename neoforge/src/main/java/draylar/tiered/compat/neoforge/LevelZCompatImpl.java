package draylar.tiered.compat.neoforge;

import net.minecraft.world.entity.player.Player;

public final class LevelZCompatImpl {

    private LevelZCompatImpl() {
    }

    public static boolean isLoaded() {
        return false;
    }

    public static int getSmithingLevel(Player player) {
        return 0;
    }
}
