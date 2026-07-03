package draylar.tiered;

import draylar.tiered.config.TooltipKeysConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public final class TieredKeybinds {

    private TieredKeybinds() {
    }

    public static void register() {
        TooltipKeysConfig.load();
    }

    private static boolean isHeld(int keyCode) {
        if (keyCode < 0) return false;
        long window = MinecraftClient.getInstance().getWindow().getHandle();
        return InputUtil.isKeyPressed(window, keyCode);
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

    private static Text keyName(int keyCode) {
        if (keyCode < 0) return Text.literal("?");
        return InputUtil.Type.KEYSYM.createFromCode(keyCode).getLocalizedText();
    }

    public static Text descriptionsKeyName() {
        return keyName(TooltipKeysConfig.descriptionsKey);
    }

    public static Text detailsKeyName() {
        return keyName(TooltipKeysConfig.detailsKey);
    }
}
