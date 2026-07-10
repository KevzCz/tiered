package draylar.tiered.api;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.player.Player;

public final class Cooldowns {

    private Cooldowns() {
    }

    @ExpectPlatform
    public static void start(Player player, String id, int ticks) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static int remaining(Player player, String id) {
        throw new AssertionError();
    }

    public static boolean ready(Player player, String id) {
        return remaining(player, id) <= 0;
    }

    @ExpectPlatform
    public static void clear(Player player, String id) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void tick(Player player) {
        throw new AssertionError();
    }
}
