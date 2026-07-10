package draylar.tiered.compat.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.levelz.access.LevelManagerAccess;
import net.levelz.level.LevelManager;
import net.levelz.level.Skill;
import net.minecraft.world.entity.player.Player;

public final class LevelZCompatImpl {

    private static final String MOD_ID = "levelz";

    private LevelZCompatImpl() {
    }

    public static boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded(MOD_ID);
    }

    public static int getSmithingLevel(Player player) {
        if (!isLoaded() || player == null) return 0;
        try {
            for (Skill skill : LevelManager.SKILLS.values()) {
                if (skill.getKey().equals("smithing")) {
                    return ((LevelManagerAccess) player).getLevelManager().getSkillLevel(skill.getId());
                }
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }
}
