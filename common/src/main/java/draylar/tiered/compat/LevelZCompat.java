package draylar.tiered.compat;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.player.Player;

public final class LevelZCompat {

    private LevelZCompat() {
    }

    @ExpectPlatform
    public static boolean isLoaded() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static int getSmithingLevel(Player player) {
        throw new AssertionError();
    }
}
