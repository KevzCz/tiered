package draylar.tiered;

import com.mojang.blaze3d.platform.InputConstants;
import draylar.tiered.config.TooltipKeysConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public final class TieredKeybinds {

    private TieredKeybinds() {
    }

    public static void register() {
        TooltipKeysConfig.load();
    }

    private static boolean isHeld(int keyCode) {
        if (keyCode < 0) return false;
        long window = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(window, keyCode);
    }

    public static boolean detailsHeld() {
        return isHeld(TooltipKeysConfig.detailsKey);
    }

    public static boolean descriptionsHeld() {
        return isHeld(TooltipKeysConfig.descriptionsKey);
    }

    public static boolean worksWithHeld() {
        return isHeld(TooltipKeysConfig.worksWithKey);
    }

    private static Component keyName(int keyCode) {
        if (keyCode < 0) return Component.literal("?");
        return InputConstants.Type.KEYSYM.getOrCreate(keyCode).getDisplayName();
    }

    public static Component descriptionsKeyName() {
        return keyName(TooltipKeysConfig.descriptionsKey);
    }

    public static Component detailsKeyName() {
        return keyName(TooltipKeysConfig.detailsKey);
    }
}
