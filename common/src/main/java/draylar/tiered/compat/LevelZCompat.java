package draylar.tiered.compat;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.player.Player;

/**
 * LevelZ has no NeoForge port; this compat is a real Fabric-side implementation
 * backed by LevelZ's own API, and a no-op stub on NeoForge.
 */
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
